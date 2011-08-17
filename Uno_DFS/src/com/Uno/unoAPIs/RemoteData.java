package com.Uno.unoAPIs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

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
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTEDATA|ACCESS|"+this.dataRemotePath);
		if (reply == null) {
			// TODO go to peer-to-peer.
			return false;
		}
		if (reply.equals("API|POST|REMOTEDATA|ACCESS|YES"))
			this.isAccessible = true;
		else if (reply.equals("API|POST|REMOTEDATA|ACCESS|NO"))
			this.isAccessible = false;
		
		return this.isAccessible;
	}
	
	public String getMetadata() {
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTEDATA|METADATA|"+this.dataRemotePath);
		if (reply == null) {
			// TODO go to peer-to-peer.
			return null;
		}
		if (reply.equals("API|POST|REMOTEDATA|METADATA|DENIED")) {
			this.isAccessible = false; // denied.
			return null;
		}
		if (reply.startsWith("API|POST|REMOTEDATA|METADATA")) {
			this.metaData = reply.split("\\|")[4];
		}
		// Attention: this metadata is separated by % for each section.
		return this.metaData;
	}
	
	/*
	 * Constructors.
	 * */
	public RemoteData(String path) {
		this.dataRemoteName = path.substring(path.lastIndexOf("/")+1, path.length());
		this.dataRemotePath = path;
	}
	
	/*
	 * Local methods
	 * */
	private String sendTcpPacket(String ip, int port, String msg) {
    	try	{
    		InetAddress remoteAddr = InetAddress.getByName(ip);
    		Socket socket = new Socket(remoteAddr, port);
    		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    		out.println(msg);
    		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    		String reply = in.readLine();
    		socket.close();
    		return reply;
    	}
    	catch (Exception e)
    	{
    		return null;
    	}
    }
}
