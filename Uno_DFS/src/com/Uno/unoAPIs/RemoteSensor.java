package com.Uno.unoAPIs;

public class RemoteSensor {

	/*
	 * Variables.
	 * */
	private String sensorRemoteName = null;
	private String sensorRemotePath = null;
	private boolean isAccessible = false; // default.
	private String [] readings = new String[4];
		
	/*
	 * Public Methods.
	 * */
	public String getLocalName() {
		return this.sensorRemoteName;
	}
	
	public String getLocalPath() {
		return this.sensorRemotePath;
	}
	
	public boolean isAccessible() {
		return this.isAccessible;
	}
	
	public String [] getInstantReadings() {
		
		// TODO read sensor.
		return null;
	}
	
	public void startRemoteLogging() {}
	
	public void stopRemoteLogging() {}
	
	/*
	 * Constructors.
	 * */
	public RemoteSensor(String path) {
		// TODO create the init information.
	}
	
	/*
	 * Local methods
	 * */
}
