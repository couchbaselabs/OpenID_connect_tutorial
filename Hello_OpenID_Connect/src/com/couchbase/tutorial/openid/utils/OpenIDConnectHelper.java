package com.couchbase.tutorial.openid.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kong.unirest.Cookie;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

/**
 * Class responsible for establishing the link between :
 * 
 * - the OP (OpenID Connect Provider) : KeyCloakand the client (this source
 * code).
 * 
 * - the client : this Java SDK CB Lite 2.7.0 code
 * 
 * - the OIDC Service Consumer : the Sync Gateway 2.7.0
 * 
 * @author fabriceleray
 *
 */
public class OpenIDConnectHelper {
	// urls
	// Sync Gateway DB endpoint
	private static final String SG_DB_URL = "http://sync-gateway:4984/french_cuisine/";
	// Keycloak (KC) endpoint
	private static final String KC_OIDC_AUTH_URL = "http://keycloak:8080/auth/realms/CouchbaseRealm/protocol/openid-connect/auth/";

	/**
	 * Compute tokenID from DBUSER / DBPASS
	 * 
	 * @param dbUser
	 * @param dbPass
	 * @return
	 */
	public static String getTokenID(String dbUser, String dbPass) {
		// http://keycloak:8080/auth/realms/master/protocol/openid-connect/auth/?
		// response_type=id_token&client_id=SyncGateway&scope=openid+profile
		// &redirect_uri=http%3A%2F%2Flocalhost%3A4984%2Ffrench_cuisine%2F
		// &nonce=34fasf3ds&state=af0ifjsldkj&foo=bar/

		HttpResponse<String> response1 = Unirest.get(KC_OIDC_AUTH_URL).header("accept", "application/json")
				.queryString("response_type", "id_token").queryString("client_id", "SyncGatewayFrenchCuisine")
				.queryString("scope", "openid,id_token").queryString("redirect_uri", SG_DB_URL)
				.queryString("nonce", StringConstants.NONCE).queryString("state", StringConstants.STATE).asString();

		// <form id="kc-form-login" onsubmit="login.disabled = true; return true;"
		// action="http://keycloak:8080/auth/realms/master/login-actions/authenticate?session_code=FlKWqRz58B_2YBXQRRtYbjokPFfKu5BaoUWUzaDlZw8&amp;execution=afca2cf6-c09f-4c7b-91f5-3cd7d3d69410&amp;client_id=SyncGateway&amp;
		// tab_id=-85fFZhlrcU" method="post">

		// get POST method
		URL postURL = extractPostURL(response1.getBody());

		String basePostURL = postURL.toString().split("\\?")[0];
		System.out.println("basePostURL = " + basePostURL);

		// Parse the queryString into Name-Value map
		Map<String, Object> mapQueryString = null;
		try {
			mapQueryString = splitQuery(postURL);
		} catch (UnsupportedEncodingException e) {
			System.err.println(e);
			;
		}

		// Run the Authentication POST request with the given username/password to
		// obtain the id_token.
		HttpResponse<String> response2 = Unirest.post(basePostURL).header("accept", "application/json")
				.queryString(mapQueryString).field("username", dbUser).field("password", dbPass).asString();

		// get the id_token
		List<String> locationHeaderList = response2.getHeaders().get(StringConstants.LOCATION_HEADER_NAME);
		if (locationHeaderList == null) {
			throw new IllegalArgumentException("locationHeaderList is null");
		}

		String locationHeader = locationHeaderList.get(0);

		if (locationHeader == null) {
			throw new IllegalArgumentException("locationHeader is null");
		}

		URL urlWithToken = null;
		try {
			urlWithToken = new URL(locationHeader);
		} catch (MalformedURLException e) {
			System.err.println(e);
		}

		Map<String, Object> refParams = splitRef(urlWithToken);

		String idTokenValue = (String) refParams.get("id_token");
		if (idTokenValue == null) {
			throw new IllegalArgumentException("id_token is missing");
		}

		return idTokenValue;
	}

	public static Cookie createSessionCookie(String idTokenValue) {

		HttpResponse<String> response3 = Unirest.post("http://sync-gateway:4984/french_cuisine/_session")
				.header("Authorization", "Bearer " + idTokenValue).asString();

		System.out.println(" >>>> " + response3.getBody());

		Iterator<Cookie> it = response3.getCookies().iterator();
		Cookie resCookie = null;

		while (it.hasNext()) {
			Cookie cookie = it.next();
			if (StringConstants.SG_COOKIE_NAME.equals(cookie.getName())) {
				resCookie = cookie;
				break;
			}
		}

		return resCookie;
	}

	/**
	 * Get the POST URL contained in the HTML action attribute inside the submit
	 * form
	 * 
	 * Example : // <form id="kc-form-login" onsubmit="login.disabled = true; return
	 * true;" //
	 * action="http://keycloak:8080/auth/realms/master/login-actions/authenticate?session_code=FlKWqRz58B_2YBXQRRtYbjokPFfKu5BaoUWUzaDlZw8&amp;execution=afca2cf6-c09f-4c7b-91f5-3cd7d3d69410&amp;client_id=SyncGateway&amp;
	 * // tab_id=-85fFZhlrcU" method="post">
	 *
	 * return the full authentication url
	 *
	 * @param body
	 */
	private static URL extractPostURL(String body) {
		int index1 = body.indexOf(StringConstants.ACTION_ATTRIBUTE);

		if (index1 == -1) {
			throw new IllegalArgumentException("Form with POST url is missing in the returned page");
		}

		body = body.substring(index1 + StringConstants.ACTION_ATTRIBUTE.length());
		int index2 = body.indexOf("\"");

		URL url = null;
		try {
			url = new URL(body.substring(0, index2));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return url;
	}

	/**
	 * 
	 * Parse the queryString (?...) KV into Name-Value map
	 * 
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, Object> splitQuery(URL url) throws UnsupportedEncodingException {
		Map<String, Object> query_pairs = new LinkedHashMap<String, Object>();
		String query = url.getQuery();
		String[] pairs = query.split(StringConstants.AMPERSAND);
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
					URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	/**
	 * 
	 * Parse the references (#...) KV into Name-Value map
	 * 
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, Object> splitRef(URL url) {
		Map<String, Object> query_pairs = new LinkedHashMap<String, Object>();
		String query = url.getRef();
		String[] pairs = query.split(StringConstants.AMPERSAND_CHAR);
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(pair.substring(0, idx), pair.substring(idx + 1));
		}
		return query_pairs;
	}
}
