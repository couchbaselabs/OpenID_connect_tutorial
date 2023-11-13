package com.couchbase.tutorial.openid.parser;

import com.google.gson.JsonElement;

import kong.unirest.json.JSONObject;

/**
 * POJO storing input arguments values.
 * 
 * @author fabriceleray
 *
 */
public class InputArguments {
	private String user;
	private String password;
	private JsonElement jsonDocToUpsert;
	private boolean doNotReplicate = false; // replicate by default
	private boolean oidc_user = true; // OIDC connection by default

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public JsonElement getJsonDocToUpsert() {
		return jsonDocToUpsert;
	}

	public void setJsonDocToUpsert(JsonElement jsonDocToUpsert) {
		this.jsonDocToUpsert = jsonDocToUpsert;
		
	}
	
	public boolean isDoNotReplicate() {
		return doNotReplicate;
	}

	public void setDoNotReplicate(boolean doNotReplicate) {
		this.doNotReplicate = doNotReplicate;
	}
	
	public boolean isOidcUser() {
		return oidc_user;
	}

	public void setOidcUser(boolean oidc_user) {
		this.oidc_user = oidc_user;
	}
}
