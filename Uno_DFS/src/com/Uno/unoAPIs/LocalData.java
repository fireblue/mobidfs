package com.Uno.unoAPIs;

public class LocalData {

	/*
	 * Variables.
	 * */
	private String dataLocalName = null;
	private String dataLocalPath = null;
	private String metaData = null;
	private String dataAccessList = "0"; // default.
	private String statusInCloud = "offline"; // default.
		
	/*
	 * Public Methods.
	 * */
	public String getLocalName() {
		return this.dataLocalName;
	}
	
	public String getLocalPath() {
		return this.dataLocalPath;
	}
	
	public String getAccessList() {
		return this.dataAccessList;
	}
	
	public String getMetadata() {
		return this.metaData;
	}
	
	public void setAccessList(String newlist) {
		this.dataAccessList = newlist;
	}
	
	/*
	 * Constructors.
	 * */
	public LocalData(String path) {
		// TODO create the init information.
	}
	
	/*
	 * Local methods
	 * */
	private String retrieveMetadata(String path) {
		// TODO create metadata string.
		return null;
	}
}
