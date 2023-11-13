package com.couchbase.tutorial.openid.implicit_flow;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.cli.MissingArgumentException;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.Collection;
import com.couchbase.lite.CollectionConfiguration;
import com.couchbase.lite.ConcurrencyControl;
import com.couchbase.lite.Conflict;
import com.couchbase.lite.ConflictResolver;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentReplication;
import com.couchbase.lite.DocumentReplicationListener;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.ReplicatedDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorActivityLevel;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.ReplicatorType;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.SessionAuthenticator;
import com.couchbase.lite.URLEndpoint;
import com.couchbase.tutorial.openid.parser.ArgumentsParserHelper;
import com.couchbase.tutorial.openid.parser.InputArguments;
import com.couchbase.tutorial.openid.utils.OpenIDConnectHelper;
import com.couchbase.tutorial.openid.utils.StringConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kong.unirest.Cookie;

/**
 * 
 * The purpose of this code is to demonstrate the OpenID implicit flow
 * integration between:
 * 
 * - a client (this Java SDK CB Lite 3.1.1 code )
 * 
 * - an OIDC Service Provider : KeyCloak
 * 
 * - an OIDC Service Consumer : the Sync Gateway 3.1.2
 * 
 * 
 * The source code of this tutorial is mainly derived from the Java CB-Lite
 * "Getting Started App" project.
 * 
 * 
 * @author fabriceleray
 *
 */
public class GettingStartedImplicitFlow {

	private static String PROP_CHANNELS = "channels";
	private static String PROP_TYPE = "type";
	private static String PROP_ID = "id";
	private static String PROP_PRICE = "price";
	private static String PROP_TEMPERATURE = "temperature";
	private static String PROP_DATE = "date";

	private static String SEARCH_STRING_TYPE = "product";

	private static final String SYNC_GATEWAY_URL = "ws://sync-gateway:4984/french_cuisine";
	private static final String DB_PATH = new File("").getAbsolutePath() + "/resources";
	private static final String DB_NAME = "french_cuisine";
	private static final String COLLECTION_NAME = "product";

	public static void main(String[] args) throws CouchbaseLiteException, InterruptedException, URISyntaxException {

		InputArguments input = null;
		try {
			input = ArgumentsParserHelper.parseArguments(args);
		} catch (MissingArgumentException mae) {
			System.err.println(mae);
			System.exit(-1);
		}

		// get user credentials from command line arguments
		String user = input.getUser();
		String password = input.getPassword();

		// get optional arguments
		JsonElement jsonDocToUpsert = input.getJsonDocToUpsert();

		// Initialize Couchbase Lite
		CouchbaseLite.init();

		// Get the database (and create it if it doesn’t exist).
		DatabaseConfiguration config = new DatabaseConfiguration();

		System.out.println("DB_PATH = " + DB_PATH);
		config.setDirectory(DB_PATH);
		Database database = new Database(DB_NAME + "_" + user, config);

		Collection productColl = database.getCollection(COLLECTION_NAME);
		if (productColl == null) {
			System.out.println("Collection " + COLLECTION_NAME + "did not existe and there has just been created.");
			productColl = database.createCollection(COLLECTION_NAME);
		}

		MutableDocument mutableDoc = null;

		/*
		 * 
		 * Explanations:
		 * 
		 * In order to illustrate conflict resolution at saving time, we deliberately
		 * get the document at startup time while replication is not yet enabled.
		 * 
		 * A modified version of this document instance will be saved later while,
		 * potentially being updated in parallel by the replication process, therefore
		 * conducting to a conflict.
		 * 
		 */

		if (null != jsonDocToUpsert) {
			JsonObject jsonObj = jsonDocToUpsert.getAsJsonObject();

			String docId = getDocId(jsonDocToUpsert);

			if (null == productColl.getDocument(docId)) {
				// Create a new document (i.e. a record) in a collection of the database.
				mutableDoc = new MutableDocument(docId);
			} else {
				// Retrieve the document from the collection of the database.
				mutableDoc = productColl.getDocument(docId).toMutable();
			}

			if (input.isDoNotReplicate()) {
				writeNewDocument(mutableDoc, jsonDocToUpsert, productColl);
			}
		}

		if (input.isDoNotReplicate()) {
			displayResult(productColl);
			System.out.println("Replication NOT enabled. Quit now.");
			System.exit(0);
		}

		// Create a query to fetch documents of type "product".
		System.out.println("== Executing Query 1");
		Query query = QueryBuilder.select(SelectResult.all()).from(DataSource.collection(productColl));
		// .where(Expression.property(PROP_TYPE).equalTo(Expression.string(SEARCH_STRING_TYPE)));
		ResultSet result = query.execute();
		System.out.println(
				String.format("Query returned %d rows of type %s", result.allResults().size(), SEARCH_STRING_TYPE));

		// ==================================
		// Define push/pull replication here.
		// ==================================

		final Collection coll = productColl;
		CollectionConfiguration collectionConfiguration = new CollectionConfiguration();
		collectionConfiguration.setConflictResolver(new ConflictResolver() {

			@Override
			public Document resolve(Conflict conflict) {

				System.err.println(
						"!!!!!!! While REPLICATING, conflict detected for document " + conflict.getDocumentId());

				Document localDocument = conflict.getLocalDocument();
				Document remoteDocument = conflict.getRemoteDocument();

				// Compute average temperature
				double temp1 = localDocument.getDouble(PROP_TEMPERATURE);
				double temp2 = remoteDocument.getDouble(PROP_TEMPERATURE);

				MutableDocument mutableDoc2 = remoteDocument.toMutable();
				mutableDoc2.setDouble(PROP_TEMPERATURE, (double) (temp1 + temp2) / 2.0f);

				return mutableDoc2;
			}
		});

		Endpoint targetEndpoint = new URLEndpoint(new URI(SYNC_GATEWAY_URL));
		ReplicatorConfiguration replConfig = new ReplicatorConfiguration(targetEndpoint)
				.addCollection(productColl, null).setAcceptOnlySelfSignedServerCertificate(false);

		replConfig.setType(ReplicatorType.PUSH_AND_PULL);
		replConfig.setContinuous(true);

		if (input.isOidcUser()) {
			// =======================================
			// Add OpenID Connect authentication here.
			// =======================================

			// get the id_token from user credentials
			String tokenID = OpenIDConnectHelper.getTokenID(user, password);
			// create session storing the id_token (at SG level)
			// and save the sessionID inside a cookie
			Cookie cookie = OpenIDConnectHelper.createSessionCookie(tokenID);

			SessionAuthenticator sa = new SessionAuthenticator(cookie.getValue(), StringConstants.SG_COOKIE_NAME);
			replConfig.setAuthenticator(sa);
		} else {
			replConfig.setAuthenticator(new BasicAuthenticator(user, password.toCharArray()));
		}

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

		Thread.sleep(2000);

		if (null != jsonDocToUpsert) {
			writeNewDocument(mutableDoc, jsonDocToUpsert, productColl);
		}

		// Check status of replication and wait till it is completed
		while (replicator.getStatus().getActivityLevel() != ReplicatorActivityLevel.STOPPED) {
			Thread.sleep(5000);

			displayResult(productColl);
		}

		System.out.println("Finish!");

		System.exit(0);
	}

	private static void displayResult(Collection productColl) {
		int numRows = 0;
		// Create a query to fetch all documents.
		System.out.println("== Executing Local DB Query");
		Query queryAll = QueryBuilder.select(SelectResult.expression(Meta.id), SelectResult.property(PROP_PRICE),
				SelectResult.property(PROP_TYPE), SelectResult.property(PROP_CHANNELS),
				SelectResult.property(PROP_TEMPERATURE)).from(DataSource.collection(productColl));
		try {
			for (Result thisDoc : queryAll.execute()) {
				numRows++;
				System.out.println(String.format("%d ... Id: %s Name: %s Price: %.2f Type is %s Temp. %.2f", numRows,
						thisDoc.getString(PROP_ID), thisDoc.getString(PROP_ID), thisDoc.getDouble(PROP_PRICE),
						thisDoc.getString(PROP_TYPE), thisDoc.getDouble(PROP_TEMPERATURE)));
			}
		} catch (CouchbaseLiteException e) {
			e.printStackTrace();
		}
		System.out.println(String.format("Total rows returned by query = %d", numRows));
	}

	/**
	 * Create a new document (i.e. a record in the collection of the database) from
	 * the input JSON value.
	 * 
	 * @param channelValue
	 * @param collectipon
	 * @throws CouchbaseLiteException
	 */
	private static void writeNewDocument(MutableDocument mutDoc, JsonElement jsonDocToUpsert, Collection collection)
			throws CouchbaseLiteException {

		JsonObject jsonObj = jsonDocToUpsert.getAsJsonObject();

		// Update a document.
		for (String k : jsonObj.keySet()) {
			if (PROP_PRICE.equals(k) || PROP_TEMPERATURE.equals(k)) {
				double value = (double) jsonObj.get(k).getAsDouble();
				mutDoc.setDouble(k, value);
			} else {
				String str = jsonObj.get(k).getAsString();
				mutDoc.setString(k, str);
			}
		}

		mutDoc.setString(PROP_DATE, Instant.now().toString());

		if (!collection.save(mutDoc, ConcurrencyControl.FAIL_ON_CONFLICT)) {
			String id = mutDoc.getId();
			System.err.println("!!!!!!! While SAVING, conflict detected for document " + id);

			Document doc = collection.getDocument(id);

			// Compute average temperature
			double temp1 = mutDoc.getDouble(PROP_TEMPERATURE);
			double temp2 = doc.getDouble(PROP_TEMPERATURE);

			mutDoc.setDouble(PROP_TEMPERATURE, (double) (temp1 + temp2) / 2.0f);

			try {
				collection.save(mutDoc);
			} catch (CouchbaseLiteException e) {
				System.err.println(e);
			}

		}

		Document document = collection.getDocument(mutDoc.getId());
		// Log the document ID (generated by the database) and properties
		System.out.println("Document ID is :: " + document.getId());
		System.out.println("Name " + document.getString(PROP_TYPE) + "_" + document.getString(PROP_ID));
		System.out.println("Price " + document.getDouble(PROP_PRICE));
		System.out.println("Channels " + document.getString(PROP_CHANNELS));
		System.out.println("Temp. " + document.getDouble(PROP_TEMPERATURE));
	}

	private static String getDocId(JsonElement jsonDocToUpsert) {
		JsonObject jsonObj = jsonDocToUpsert.getAsJsonObject();

		String idValue = jsonObj.get(PROP_ID).getAsString();

		if (null == idValue) {
			idValue = UUID.randomUUID().toString();
		}

		return "sensor_" + idValue;
	}
}