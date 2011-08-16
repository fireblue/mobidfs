package com.Uno.unoAPIs;

public class LocalSensor {

	/*
	 * Variables.
	 * */
	private String sensorLocalName = null;
	private String sensorLocalPath = null;
	private String sensorAccessList = "0"; // default.
	private String statusInCloud = "offline"; // default.
	private String [] readings = new String[4];
		
	/*
	 * Public Methods.
	 * */
	public String getLocalName() {
		return this.sensorLocalName;
	}
	
	public String getLocalPath() {
		return this.sensorLocalPath;
	}
	
	public String getAccessList() {
		return this.sensorAccessList;
	}
	
	public void setAccessList(String newlist) {
		this.sensorAccessList = newlist;
	}
	
	public String [] getReadings() {
		
		// TODO read sensor.
		return null;
	}
	
	public void startLogging() {}
	
	public void stopLogging() {}
	
	/*
	 * Constructors.
	 * */
	public LocalSensor(int type) {
		// TODO create the init information.
	}
	
	/*
	 * Local methods
	 * */
}
