package com.Uno.unoAPIs;

public class CloudManager {

	/*
	 * Some constants.
	 * */
	private String GOVERNOR_ADDRESS = "com1379.eecs.utk.edu";
	
	/*
	 * Client System Initialization
	 * */
	public void init() {}
	
	/*
	 * Authentication 
	 * */
	public void login(String usr, String pwd) {}
	
	/*
	 * Register new users
	 * */
	public void register(String usr, String pwd) {}
	
	/*
	 * Push methods
	 * */
	public void push() {}
	
	public void push(LocalData ld) {}
	
	public void push(LocalSensor ls) {}
	
	/*
	 * Pull methods
	 * */
	public void pull() {}
	
	public LocalData pull(RemoteData rd) {return null;}
	
	public RemoteData pull(String path) {return null;}
	
	public RemoteSensor pull(String path, int type) {return null;}
	
	/*
	 * Search methods
	 * */
	public String [] search(String keyword) {return null;}
	
	/*
	 * Backup methods
	 * */
	public void backup(LocalData ld) {}
	
	/*
	 * Private methods
	 * */
	
}
