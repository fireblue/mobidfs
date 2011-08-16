package com.Uno.unoAPIs;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

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
		this.dataLocalPath = path;
		this.dataLocalName = path.substring(path.lastIndexOf("/")+1, path.length());
		this.dataAccessList = "0";
		this.statusInCloud = "offline";
	}
	
	public LocalData(File rf) {
		this.dataLocalPath = rf.getAbsolutePath();
		this.dataLocalName = rf.getName();
		this.dataAccessList = "0";
		this.statusInCloud = "offline";
	}
	
	/*
	 * Local methods
	 * */
	private String retrieveMetadata (String path)
    {
    	File f = new File(path);
    	if (!f.exists()) return null;
    	String metadata = "";

    	try{
    		metadata += String.valueOf(new FileInputStream(f).available()/1024)+"%";
    	} catch (Exception e) {}
    	metadata += (f.canWrite() ? "w":"nw")+"%";
    	metadata += (f.canRead() ? "r":"nr")+"%";
    	metadata += (f.canExecute() ? "x":"nx")+"%";
    	Date d = new Date(f.lastModified());
    	metadata += d.toGMTString();
    	
    	return metadata;
    }
}
