package com.Uno.unoAPIs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

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
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTESENSOR|ACCESS|"+this.sensorRemotePath);
		if (reply == null) {
			// TODO peer-to-peer.
		}
		if (reply.equals("API|POST|REMOTESENSOR|ACCESS|NO"))
			this.isAccessible = false;
		else if (reply.equals("API|POST|REMOTESENSOR|ACCESS|YES"))
			this.isAccessible = true;
		return this.isAccessible;
	}
	
	public String [] getInstantReadings() {
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTESENSOR|INSTANTREADING|"+this.sensorRemotePath);
		if (reply == null) {
			// TODO peer-to-peer.
		}
		if (reply.equals("API|GET|REMOTESENSOR|INSTANTREADING|DENIED")) {
			return null;
		}
		if (reply.startsWith("API|GET|REMOTESENSOR|INSTANTREADING")) {
			// TODO the UnoService part.
			String response = sendTcpPacket(reply.split("//|")[4], 11314, "API|GET|INSTANTREADING|"+this.sensorRemoteName);
			if (response == null) return null;
			if (response.startsWith("API|POST|INSTANTREADING")) {
				this.readings = response.split("\\|")[3].split("%");
			}
			return this.readings;
		}
		
		return null;
	}
	
	public void startRemoteLogging() {}
	
	public void stopRemoteLogging() {}
	
	/*
	 * Constructors.
	 * */
	public RemoteSensor(String path) {
		this.sensorRemoteName = path.substring(path.lastIndexOf("/")+1, path.length());
		this.sensorRemotePath = path;
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
