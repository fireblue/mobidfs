package com.Uno.unoAPIs;

public class RemoteData {
	
	/*
	 * Variables.
	 * */
	private String dataRemoteName = null;
	private String dataRemotePath = null;
	private String metaData = null;
	private boolean isAccessible = false; // default.
		
	/*
	 * Public Methods.
	 * */
	public String getRemoteName() {
		return this.dataRemoteName;
	}
	
	public String getRemotePath() {
		return this.dataRemotePath;
	}
	
	public boolean isAccessible() {
		
		// TODO update info.
		return this.isAccessible;
	}
	
	public String getMetadata() {
		
		// TODO retrieve info.
		return this.metaData;
	}
	
	/*
	 * Constructors.
	 * */
	public RemoteData(String path) {
		// TODO create the init information.
	}
	
	/*
	 * Local methods
	 * */
}
