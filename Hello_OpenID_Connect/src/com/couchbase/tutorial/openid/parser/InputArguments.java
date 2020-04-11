package com.couchbase.tutorial.openid.parser;

/**
 * POJO storing input arguments values.
 * 
 * @author fabriceleray
 *
 */
public class InputArguments {
	private String user;
	private String password;
	private int numberNewDocsToCreate;
	private String channelValue;
	
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

	public int getNumberNewDocsToCreate() {
		return numberNewDocsToCreate;
	}

	public void setNumberNewDocsToCreate(int numberNewDocsToCreate) {
		this.numberNewDocsToCreate = numberNewDocsToCreate;
	}

	public String getChannelValue() {
		return channelValue;
	}

	public void setChannelValue(String channelValue) {
		this.channelValue = channelValue;
	}

}
