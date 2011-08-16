package com.Uno.unoAPIs;

public class CloudManager {

	/*
	 * Some constants.
	 * */
	private String GOVERNOR_ADDRESS = "com1379.eecs.utk.edu";
	
	/*
	 * Client System Initialization
	 * Attention: this step does not run Service, it needs manually run.
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
	 * Push methods: only send metadata.
	 * */
	public void push() {}
	
	public void push(LocalData ld) {}
	
	public void push(LocalSensor ls) {}
	
	/*
	 * Pull methods: get metadata, remote sensor, remote data instance.
	 * */
	public void pull() {}
	
	// pull physical remote data to local.
	public LocalData pull(RemoteData rd) {return null;}
	
	// pull remote metadata from a pariticular path.
	public RemoteData pull(String path) {return null;}
	
	// pull a remote sensor from a specific path with certain type.
	public RemoteSensor pull(String path, int type) {return null;}
	
	/*
	 * Search methods: return a group of path which contain the resource keyword.
	 * */
	public String [] search(String keyword) {return null;}
	
	/*
	 * Backup methods: it will send not only the metadata, but also the physical data itself.
	 * */
	public void backup(LocalData ld) {}
	
	/*
	 * Private methods
	 * */
	
}
