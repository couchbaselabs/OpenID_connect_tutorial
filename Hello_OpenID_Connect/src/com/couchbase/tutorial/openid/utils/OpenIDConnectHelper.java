package com.couchbase.tutorial.openid.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kong.unirest.json.JSONObject;

import kong.unirest.Cookie;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

/**
 * 
 * See https://blog.couchbase.com/oidc-authorization-code-flow-client-authentication-couchbase-sync-gateway/
 * 
 * Class responsible for establishing the link between :
 * 
 * - the OP (OpenID Connect Provider) : KeyCloak
 * 
 * - a client (this Java SDK CB Lite 3.1.1 code )
 * 
 * - an OIDC Service Consumer : the Sync Gateway 3.1.2
 * 
 * @author fabriceleray
 *
 */
public class OpenIDConnectHelper {
	// urls
	// Sync Gateway DB endpoint
	private static final String SG_DB_URL = "http://sync-gateway:4984/french_cuisine/";
	// Keycloak (KC) endpoint
	// See
	// https://www.keycloak.org/docs/6.0/server_admin/#keycloak-server-oidc-uri-endpoints
	private static final String KC_OIDC_TOKEN_URL = "http://keycloak:8080/auth/realms/couchbase/protocol/openid-connect/token";

	// Auth code.
	private static final String OIDC_CALLBACK = "_oidc_callback";
	
	private static final String CLIENT_SECRET_VALUE = "5hL1AdiFmYGEySGtVI3XAmxUJ0bj82r5";
	
	
	private static final String ID_TOKEN = "id_token";
	private static final String OPENID = "openid";
	
	/**
	 * Used by Implicit Flow Compute tokenID from DBUSER / DBPASS
	 * 
	 * @param dbUser
	 * @param dbPass
	 * @return
	 */
	public static String getTokenID(String dbUser, String dbPass) {
		
	    Map<String, Object> fields = new HashMap<>();
	    fields.put("grant_type", "password");
	    fields.put("scope", OPENID);
	    fields.put("client_id", "SyncGatewayFrenchCuisine");
	    fields.put("client_secret", CLIENT_SECRET_VALUE);
	    fields.put("username", dbUser);
	    fields.put("password", dbPass);
	    

		HttpResponse<JsonNode> jsonResponse = Unirest.post(KC_OIDC_TOKEN_URL)
				.header("accept", "application/json")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.fields(fields).asJson();


		JSONObject object = jsonResponse.getBody().getObject();
		String  idToken = (String) object.get(ID_TOKEN);

		return idToken;
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

	/**
	 * 
	 * Split the query string parameters.
	 * 
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, Object> splitQueryString(URL url) {
		Map<String, Object> query_pairs = new LinkedHashMap<String, Object>();
		String query = url.getQuery();
		String[] pairs = query.split(StringConstants.AMPERSAND_CHAR);
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(pair.substring(0, idx), pair.substring(idx + 1));
		}
		return query_pairs;
	}

	/**
	 * Used by Authorization Code Flow.
	 * 
	 * 
	 * @param dbUser
	 * @param dbPass
	 * @return
	 */
	public static String getSessionFromAuthorizationCode(String dbUser, String dbPass) {
		
		// Unirest.config().enableCookieManagement(false);

		HttpResponse<String> response1 = Unirest.get(KC_OIDC_TOKEN_URL).header("accept", "application/json")
				.queryString("response_type", "code").queryString("client_id", "SyncGatewayFrenchCuisine")
				.queryString("scope", "openid email").queryString("redirect_uri", SG_DB_URL + OIDC_CALLBACK)
				.queryString("access_type","online")
				.queryString("state", StringConstants.STATE).asString();

		// response containing form like :
		// <form id="kc-form-login" onsubmit="login.disabled = true; return true;"
		// action="http://keycloak:8080/auth/realms/couchbase/login-actions/authenticate?session_code=p7ef6R42VgVTdaMeFIzJo0KRAuS0bypOftfJIplcOdw&amp;execution=d1d57d23-a7ea-4daf-ac7e-e8e4f4df0b46&amp;client_id=SyncGatewayFrenchCuisine&amp;tab_id=q7-7Jj9cg_Q"
		// method="post">
		
		Cookies cookies = response1.getCookies();

		// retrieve the POST method inside the returned form
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

		String cookStr = "";
		Iterator<Cookie> cookIterator = cookies.iterator();
		while(cookIterator.hasNext()) {
			Cookie cook = cookIterator.next();
			cookStr += cook.getName() + "=" + cook.getValue();
			cookStr += "; ";
		}
		cookStr = cookStr.substring(0, cookStr.lastIndexOf("; "));
		
		
		// Run the Authentication POST request with the given username/password to
		// obtain the authorization code.
		HttpResponse<String> response2 = Unirest.post(basePostURL)
//				.header("Referer", "http://keycloak:8080/auth/realms/couchbase/protocol/openid-connect/auth?client_id=SyncGatewayFrenchCuisine"
//						+ "&redirect_uri=http://sync-gateway:4984/french_cuisine/_oidc"
//						+ "&response_type=code&scope=openid")
				.header("Cookie", cookStr)
				.header("Accept-Encoding", "gzip, deflate")
				.header("Upgrade-Insecure-Requests", "1")
				.header("Content-Type","application/x-www-form-urlencoded")
				.queryString(mapQueryString)
				.field("username", dbUser).field("password", dbPass).field("credentialId", "").asString();

		// get the authorization code
		List<String> locationHeaderList = response2.getHeaders().get(StringConstants.LOCATION_HEADER_NAME);
		if (locationHeaderList == null || locationHeaderList.isEmpty()) {
			throw new IllegalArgumentException("locationHeaderList is null or empty");
		}

		String locationHeader = locationHeaderList.get(0);

		if (locationHeader == null) {
			throw new IllegalArgumentException("locationHeader is null");
		}
		
		String basePostURL2 = locationHeader.toString().split("\\?")[0];
		System.out.println("basePostURL2 = " + basePostURL2);

		URL urlWithCode = null;
		try {
			urlWithCode = new URL(locationHeader);
		} catch (MalformedURLException e) {
			System.err.println(e);
		}

		// Parse the queryString into Name-Value map
		Map<String, Object> mapQueryString2 = splitQueryString(urlWithCode);
//
		/////////////
		/// ADDED F.LERAY
		
		// Run the Authentication GET request with the given username/password to
		// obtain the authorization code.
		HttpResponse<String> response3= Unirest.get(basePostURL2)
				.header("Cookie", cookStr)
				.header("Content-Type","application/x-www-form-urlencoded")
				.queryString("offline", false)
				.queryString(mapQueryString2).asString();

		String body = response3.getBody();
		if(null != body) {
			JSONObject obj = new JSONObject(body);
			System.out.println("id_token = " + obj.get("id_token"));
			System.out.println("refresh_token = " + obj.get("refresh_token"));
			System.out.println("name = " + obj.get("name"));
			System.out.println("session_id = " + obj.get("session_id"));
		}
		
		//		System.out.println();

		/////////////

		Cookies cookies2 = response3.getCookies();
		String session = cookies2.getNamed(StringConstants.SG_COOKIE_NAME).getValue();
		
		System.out.println("session = " + session);
				
		return session;
	}
}
