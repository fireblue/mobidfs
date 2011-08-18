package com.Uno.unoAPIs;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

import android.util.Log;
import android.widget.Toast;

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
			return false;
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
			return null;
		}
		if (reply.equals("API|POST|REMOTESENSOR|INSTANTREADING|DENIED")) {
			return null;
		}
		if (reply.startsWith("API|POST|REMOTESENSOR|INSTANTREADING")) {
			String response = sendTcpPacket(reply.split("//|")[4], 11314, "API|GET|REMOTESENSOR|INSTANTREADING|"+this.sensorRemoteName);
			if (response == null) return null;
			if (response.startsWith("API|POST|REMOTESENSOR|INSTANTREADING")) {
				this.readings = response.split("\\|")[3].split("%");
			}
			return this.readings;
		}
		
		return null;
	}
	
	// Start a particular sensor logging process. 
	public boolean startRemoteLogging() {
		// First contact server.
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTESENSOR|LOGGING|"+this.sensorRemotePath);
		if (reply == null) {
			// TODO peer-to-peer.
			return false;
		}
		if (reply.equals("API|POST|REMOTESENSOR|LOGGING|DENIED")) return false;
		if (reply.startsWith("API|POST|REMOTESENSOR|LOGGING")) {
			String response = sendTcpPacket(reply.split("\\|")[4], 11314, "API|GET|REMOTESENSOR|LOGGING|START|"+UnoConstant.Owner+"|"+this.sensorRemoteName);
			if (response == null) return false;
			if (response.equals("API|GET|REMOTESENSOR|LOGGING|START|YES")) return true;
			else return false;
		}
		return false;
	}
	
	// Stop the logging process. 
	public boolean stopRemoteLogging() {
		// First contact server.
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTESENSOR|LOGGING|"+this.sensorRemotePath);
		if (reply == null) {
			// TODO peer-to-peer.
			return false;
		}
		if (reply.equals("API|POST|REMOTESENSOR|LOGGING|DENIED")) return false;
		if (reply.startsWith("API|POST|REMOTESENSOR|LOGGING")) {
			String response = sendTcpPacket(reply.split("\\|")[4], 11314, "API|GET|REMOTESENSOR|LOGGING|STOP|"+UnoConstant.Owner+"|"+this.sensorRemoteName);
			if (response == null) return false;
			if (response.equals("API|GET|REMOTESENSOR|LOGGING|STOP|YES")) return true;
			else return false;
		}
		return false;
	}
	
	// Fetch a log.
	public boolean fetchLogs(String localPath) {
		// First contact server.
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "API|GET|REMOTESENSOR|LOGGING|"+this.sensorRemotePath);
		if (reply == null) {
			// TODO peer-to-peer.
			return false;
		}
		if (reply.equals("API|POST|REMOTESENSOR|LOGGING|DENIED")) return false;
		if (reply.startsWith("API|POST|REMOTESENSOR|LOGGING")) {
			final String owner = UnoConstant.Owner;
			final String sensor = this.sensorRemoteName;
			final String ip = reply.split("\\|")[4];
			final String path = localPath;
			new Runnable () {

				@Override
				public void run() {
					try
		        	{
		        		InetAddress remote = InetAddress.getByName(ip);
		        		Socket s = new Socket(remote, 11314);
		        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		        		out.println("PIN|SENSOR_LOG|"+sensor+"|"+owner);

		        		InputStream sin = s.getInputStream();
		        		File f = new File(path.substring(0, path.lastIndexOf("/")));
		        		if (!f.exists()) f.mkdirs();
		        		File xf = new File(path);
		        		if (!xf.exists()) xf.createNewFile();
		        		byte [] buf = new byte[s.getReceiveBufferSize()];
		        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(xf, true)); // append.
		        		
		        		while (true)
		        		{
		        			int nbytes = sin.read(buf);
		        			if (nbytes < 0) break;
		        			bout.write(buf, 0, nbytes);
		        			bout.flush();
		        		}
		        		bout.close();	
		        	}
		        	catch (Exception e)
		        	{
		        		Log.e("SocketFile", e.toString());
		        	}
					
				}}.run();
			
		}
		return true;
	}
	
	
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
