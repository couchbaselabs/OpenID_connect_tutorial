package com.couchbase.tutorial.openid;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.cli.MissingArgumentException;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentReplication;
import com.couchbase.lite.DocumentReplicationListener;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.ReplicatedDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.SessionAuthenticator;
import com.couchbase.lite.URLEndpoint;
import com.couchbase.tutorial.openid.parser.ArgumentsParserHelper;
import com.couchbase.tutorial.openid.parser.InputArguments;
import com.couchbase.tutorial.openid.utils.OpenIDConnectHelper;
import com.couchbase.tutorial.openid.utils.StringConstants;

import kong.unirest.Cookie;

/**
 * 
 * The purpose of this code is to demonstrate the OpenID implicit flow
 * integration between:
 * 
 * - a client (this Java SDK CB Lite 2.7.0 code )
 * 
 * - an OIDC Service Provider : KeyCloak
 * 
 * COMMENT TO REMOVE => note : behind the scene, an internal Identity Provider will be used (Key
 * cloak built-in users provider)
 * 
 * - an OIDC Service Consumer : the Sync Gateway 2.7.0
 * 
 * 
 * The source code of this tutorial is mainly derived from the Java CB-Lite
 * "Getting Started App" project. See
 * https://docs.couchbase.com/couchbase-lite/2.7/java-platform.html#building-a-getting-started-app
 * 
 * Note : the "name' returned by keycloak is not 'paul or 'wolfgang' or... but a
 * unique string composed of KC domain URL + a generated UUID for the user.
 * 
 * Example (for 'paul') :
 * 
 * "name":"keycloak%3A8080%2Fauth%2Frealms%2Fmaster_8de695c4-68ff-4fbd-bf82-6c5ef84e733c"
 * 
 * AS A CONSEQUENCE, the user declaration (inside the Sync Gateway config file or
 * via REST API calls) MUST be equal to that same name.
 * 
 * @author fabriceleray
 *
 */
public class GettingStartedWithOpenIDConnect {

	private static String PROP_CHANNELS = "channels";
	private static String PROP_TYPE = "type";
	private static String PROP_ID = "id";
	private static String PROP_NAME = "name";
	private static String PROP_PRICE = "price";
	private static String SEARCH_STRING_TYPE = "product";

	private static final String SYNC_GATEWAY_URL = "ws://sync-gateway:4984/french_cuisine";
	private static final String DB_PATH = new File("").getAbsolutePath() + "/resources";
	private static final String DB_NAME = "french_cuisine";

	public static void main(String[] args) throws CouchbaseLiteException, InterruptedException, URISyntaxException {

		InputArguments input = null;
		try {
			input = ArgumentsParserHelper.parseArguments(args);
		} catch (MissingArgumentException mae) {
			System.err.println(mae);
			System.exit(-1);
		}

		// get user credentials from commandd line arguments
		String user = input.getUser();
		String password = input.getPassword();

		// get optional arguments
		int numberNewDocsToCreate = input.getNumberNewDocsToCreate();
		String channelValue = input.getChannelValue(); // "PDV_Bretagne";

		// Initialize Couchbase Lite
		CouchbaseLite.init();

		// Get the database (and create it if it doesn’t exist).
		DatabaseConfiguration config = new DatabaseConfiguration();
		config.setDirectory(DB_PATH);
		Database database = new Database(DB_NAME, config);

		for (int i = 0; i < numberNewDocsToCreate; i++) {
			writeNewDocument(channelValue, database);
		}

		// Create a query to fetch documents of type "product".
		System.out.println("== Executing Query 1");
		Query query = QueryBuilder.select(SelectResult.all()).from(DataSource.database(database))
				.where(Expression.property(PROP_TYPE).equalTo(Expression.string(SEARCH_STRING_TYPE)));
		ResultSet result = query.execute();
		System.out.println(
				String.format("Query returned %d rows of type %s", result.allResults().size(), SEARCH_STRING_TYPE));

		// ==================================
		// Define push/pull replication here.
		// ==================================

		Endpoint targetEndpoint = new URLEndpoint(new URI(SYNC_GATEWAY_URL));
		ReplicatorConfiguration replConfig = new ReplicatorConfiguration(database, targetEndpoint);
		replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

		replConfig.setContinuous(true);

		// =======================================
		// Add OpenID Connect authentication here.
		// =======================================

		// get the id_token from user credentials
		String tokenID = OpenIDConnectHelper.getTokenID(user, password);
		// create session storing the id_token (at SG level)
		// and save the sessionID inside a cookie
		Cookie cookie = OpenIDConnectHelper.createSessionCookie(tokenID);

		replConfig.setAuthenticator(new SessionAuthenticator(cookie.getValue(), StringConstants.SG_COOKIE_NAME));

		// =======================================
		// =======================================

		// Create replicator (be sure to hold a reference somewhere that will prevent
		// the Replicator from being GCed)
		Replicator replicator = new Replicator(replConfig);

		// Listen to replicator change events.
		replicator.addChangeListener(change -> {
			if (change.getStatus().getError() != null) {
				System.err.println("Error code ::  " + change.getStatus().getError().getCode());
			}
		});

		replicator.addDocumentReplicationListener(new DocumentReplicationListener() {

			@Override
			public void replication(DocumentReplication documentReplication) {
				for (ReplicatedDocument rep : documentReplication.getDocuments()) {
					System.err.println("Document " + rep.getID() + " has been replicated !!");
				}
			}
		});

		// Start replication.
		replicator.start();

		// Check status of replication and wait till it is completed
		while (replicator.getStatus().getActivityLevel() != Replicator.ActivityLevel.STOPPED) {
			Thread.sleep(5000);

			int numRows = 0;
			// Create a query to fetch all documents.
			System.out.println("== Executing Query 3");
			Query queryAll = QueryBuilder.select(SelectResult.expression(Meta.id), SelectResult.property(PROP_NAME),
					SelectResult.property(PROP_PRICE), SelectResult.property(PROP_TYPE),
					SelectResult.property(PROP_CHANNELS)).from(DataSource.database(database));
			try {
				for (Result thisDoc : queryAll.execute()) {
					numRows++;
					System.out.println(String.format("%d ... Id: %s is learning: %s version: %.2f type is %s", numRows,
							thisDoc.getString(PROP_ID), thisDoc.getString(PROP_NAME), thisDoc.getDouble(PROP_PRICE),
							thisDoc.getString(PROP_TYPE)));
				}
			} catch (CouchbaseLiteException e) {
				e.printStackTrace();
			}
			System.out.println(String.format("Total rows returned by query = %d", numRows));
		}

		System.out.println("Finish!");

		System.exit(0);
	}

	/**
	 * Create a new document (i.e. a record) in the database (with some random
	 * values insidd).
	 * 
	 * @param channelValue
	 * @param database
	 * @throws CouchbaseLiteException
	 */
	private static void writeNewDocument(String channelValue, Database database) throws CouchbaseLiteException {

		// Create a new document (i.e. a record) in the database.
		MutableDocument mutableDoc = new MutableDocument("produit_from_CBL_" + UUID.randomUUID()).setString(PROP_TYPE,
				"product");

		// Save it to the database.
		database.save(mutableDoc);

		Random rand = new Random();
		// Update a document.
		mutableDoc = database.getDocument(mutableDoc.getId()).toMutable();
		mutableDoc.setDouble(PROP_PRICE, rand.nextDouble() + 1);
		mutableDoc.setString(PROP_NAME, generateRandomString(rand));
		mutableDoc.setString(PROP_CHANNELS, channelValue);
		database.save(mutableDoc);

		Document document = database.getDocument(mutableDoc.getId());
		// Log the document ID (generated by the database) and properties
		System.out.println("Document ID is :: " + document.getId());
		System.out.println("Name " + document.getString(PROP_NAME));
		System.out.println("Price " + document.getDouble(PROP_PRICE));
		System.out.println("Channels " + document.getString(PROP_CHANNELS));
	}

	private static String generateRandomString(Random rand) {

		int length = rand.nextInt(10) + 1;
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(StringConstants.ALPHABET.charAt(rand.nextInt(StringConstants.ALPHABET.length())));
		}
		return sb.toString();
	}
}