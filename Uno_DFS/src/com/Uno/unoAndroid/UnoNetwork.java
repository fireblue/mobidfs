package com.Uno.unoAndroid;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class UnoNetwork extends ListActivity {
	
	private String NetworkPwd = "/";
	private ArrayList <NetworkItem> NetworkPwdChild;
	private ArrayList <String> NetworkPwdChildString;
	private final String GOVERNOR_IP = UnoConstant.GOVERNOR_ADDRESS;
	private ArrayAdapter <String> adapter;
	private PinDatabaseHelper pdbh = null;
	
	private String Owner = null;
	private String Device = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pdbh = new PinDatabaseHelper(this);
        initLoginInfo();
        
        NetworkPwdChildString = new ArrayList <String>();
        NetworkPwdChild = fetchPwdChildList(NetworkPwd);
        for (NetworkItem ni : NetworkPwdChild) {
        	String s = ni.ResourceName;
        	NetworkPwdChildString.add(s);
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, NetworkPwdChildString);
		setListAdapter(adapter);
    }
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		final int pos = position;
		final String keyword = this.getListAdapter().getItem(position).toString();
		if (keyword.equals("..")) {
			if (NetworkPwd.equals("/")) return;
			NetworkPwd = NetworkPwd.substring(0, NetworkPwd.lastIndexOf("/"));
			if (NetworkPwd.equals("") || NetworkPwd == null) NetworkPwd = "/";
			NetworkPwdChild = fetchPwdChildList(NetworkPwd);
			NetworkPwdChildString.clear();
			for (NetworkItem ni : NetworkPwdChild) {
	        	String s = ni.ResourceName;
	        	NetworkPwdChildString.add(s);
	        }
			adapter.notifyDataSetChanged();
		}
		else if (keyword.endsWith("/")) {
			/*
			 * Sensor/ has been considered in the server side. So we handle it like common file.
			 * */
			if (NetworkPwd.endsWith("/")) NetworkPwd += keyword.substring(0, keyword.length()-1);
			else NetworkPwd += "/" + keyword.substring(0, keyword.length()-1);
			NetworkPwdChild = fetchPwdChildList(NetworkPwd);
			NetworkPwdChildString.clear();
			for (NetworkItem ni : NetworkPwdChild) {
		       	String s = ni.ResourceName;
		       	NetworkPwdChildString.add(s);
		    }
			adapter.notifyDataSetChanged();
			
		}
		else {
			/*
			 * Two cases here: sensor file and common file
			 * Sensor file use sensor ID.
			 * 
			 * */
			if (NetworkPwd.endsWith("/Sensors")) {
				
				final String [] options = {"Instant Reading", "Sensor Logging", "Stop Logging"};
				AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
				optBuilder.setTitle("Actions");
				optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface arg0, int arg1) {
						arg0.dismiss();
						if (arg1 == 0) {
							long st = startRespondingTimeTrack();
							showSensorInstance(pos);
							stopRespondingTimeTrack("CLIENT_SENSOR_INSTANT", st);
						}
						else if (arg1 == 1){
							// TODO Logging sensor.
							long st = startRespondingTimeTrack();
							startLoggingProcess(pos);
							stopRespondingTimeTrack("CLIENT_SENSOR_LOG_START", st);
						}
						else {
							// TODO stop logging.
							long st = startRespondingTimeTrack();
							stopLoggingProcess(pos);
							stopRespondingTimeTrack("CLIENT_SENSOR_LOG_STOP", st);
						}
						
					}
				});
				optBuilder.setNegativeButton("Cancel", new OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						arg0.dismiss();
						
					}});
				optBuilder.create().show();
			}
			else {
				final String [] options = {"File Metadata", "File Preview", "Thumbtack"};
				AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
				optBuilder.setTitle("Actions");
				optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	dialog.dismiss();
				    	if (item == 0) {
				    		long st = startRespondingTimeTrack();
				    		showNetworkFileMetadata(pos);
				    		stopRespondingTimeTrack("CLIENT_NETWORK_FILE_METADATA", st);
				    	}
				    	else if (item == 1){
				    		long st = startRespondingTimeTrack();
				    		RemoteFilePreviewProcess(pos);
				    		stopRespondingTimeTrack("CLIENT_NETWORK_FILE_PREVIEW", st);
				    	}
				    	else {
				    		long st = startRespondingTimeTrack();
				    		pinNetworkFile(pos);
				    		stopRespondingTimeTrack("CLIENT_NETWORK_FILE_PIN", st);
				    	}
				    }
				});
				optBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   
			           }
			    });
				optBuilder.create().show();
			}
		}
	}
	
	private void initLoginInfo() {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream("/mnt/sdcard/Uno/login.ini");
		} catch (FileNotFoundException e) {}
		BufferedReader br = new BufferedReader(new InputStreamReader(fin));
		try {
			Owner = br.readLine().split(":")[1];
			Device = br.readLine().split(":")[1];
			br.close();
			fin.close();
		} catch (IOException e) {}
	}
	
	/*
	 * Given a pwd and fetch all children inside this directory.
	 * */
	private ArrayList <NetworkItem> fetchPwdChildList (String pwd) {
		ArrayList <NetworkItem> pwdchild = new ArrayList <NetworkItem> ();
		
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|DIR|"+pwd);
        
        NetworkItem p = new NetworkItem();
        
        // Add ".."
        p.ResourceName = "..";
        p.ResourceGlobalId = "-1";
        pwdchild.add(p);
        
        /*
         * Central server failed...needs to go P2P.
         * --------------------------------------------------------------------
         * */
        if (reply == null) {
        	File f = new File("/mnt/sdcard/Uno/p2p.ini");
        	if (!f.exists()) {
        		Toast.makeText(getApplicationContext(), "System failure...", Toast.LENGTH_LONG).show();
        		return pwdchild;
        	}
        	FileInputStream fin = null;
			try {
				fin = new FileInputStream(f);
			} catch (FileNotFoundException e) {}
        	BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        	
        	String line = "";
        	
        	try {
				
				// In the network root directory.
				if (NetworkPwd.equals("/")) {
					while ((line = br.readLine()) != null) {
						String [] meta = line.split(",");
						NetworkItem ni = new NetworkItem();
						ni.ResourceGlobalId = "-1";
						ni.ResourceName = meta[0]+"/";
						pwdchild.add(ni);
					}
					br.close();
					return pwdchild;
				}
				else if (NetworkPwd.split("/").length == 2) {
					while ((line = br.readLine()) != null) {
						String [] meta = line.split(",");
						
						if (!meta[0].equals(NetworkPwd.split("/")[1])) continue;
						
						NetworkItem ni = new NetworkItem();
						ni.ResourceGlobalId = "-1";
						ni.ResourceName = meta[1]+"/";
						pwdchild.add(ni);
					}
					br.close();
					return pwdchild;
				}
				else {
					while ((line = br.readLine()) != null) {
						String [] meta = line.split(",");
						
						if (!pwd.split("/")[1].equals(meta[0]) || !pwd.split("/")[2].equals(meta[1])) continue;
						
						String remoteIp = meta[2];
						String sendMsg = "GET|DIR|P2P|"+Owner+"|"+pwd;
						
						String response = sendTcpPacket(remoteIp, 11314, sendMsg);
							
						if (response == null) continue;
						if (response.equals("POST|DIR|P2P|")) continue;
						if (response.startsWith("POST|DIR|P2P|")) {
							String [] tlist = response.split("\\|")[3].split(";");
							for (String s: tlist) {
								String [] t = s.split("\\^");
								NetworkItem ni = new NetworkItem();
								ni.ResourceName = t[0];
								ni.ResourceGlobalId = "-1"; // Directory's global ID is -1.
								pwdchild.add(ni);
							}
						}
					}
					br.close();
					return pwdchild;
				}
			} catch (IOException e) {}
			
        }
        
        // -----------------------Common Case----------------------------------
        
        if (reply.endsWith("NO_RESOURCE")) {
        	Toast.makeText(getApplicationContext(), "No resource available now.", Toast.LENGTH_LONG).show();
        	return pwdchild;
        }
        if (reply.endsWith("ACCESS_DENY")) {
        	Toast.makeText(getApplicationContext(), "Access denied.", Toast.LENGTH_LONG).show();
        	return pwdchild;
        }
        
        if (reply.equals("POST|DIR|")) return pwdchild;
        
        if (reply.startsWith("POST|DIR|")) {
        	String [] argv = reply.split("\\|");
        	String [] tlist = argv[2].split(";");
        	for (String s : tlist) {
        		NetworkItem ni = new NetworkItem();
        		String [] t = s.split("\\^");
        		ni.ResourceName = t[0];
        		ni.ResourceGlobalId = t[1]; // Global ID is -1 for directories.
        		pwdchild.add(ni);
        	}
        	return pwdchild;
        }
        return pwdchild;
        
	}
	
	private class NetworkItem {
		public String ResourceName = "";
		public String ResourceGlobalId = "";
	}
	
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
	
	private void showNetworkFileMetadata(int pos) {
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|FILE|METADATA|"+ 
				NetworkPwdChild.get(pos).ResourceName+"|"+NetworkPwdChild.get(pos).ResourceGlobalId);
		
		/*
		 * reply == null means server failure, go P2P instead.
		 * ---------------------------------------------------------------
		 * */
		if (reply == null) {
			String path = NetworkPwd + "/" +NetworkPwdChild.get(pos).ResourceName;
			String [] tpath = path.split("/");
			String [] xpath = new String[tpath.length-2];
			for (int i = 0, k = 0; i < tpath.length; i++) {
				if (i == 1 || i == 2) continue;
				xpath[k++] = tpath[i];
			}
			
			File f = new File("/mnt/sdcard/Uno/p2p.ini");
        	if (!f.exists()) {
        		Toast.makeText(getApplicationContext(), "System failure...", Toast.LENGTH_LONG).show();
        		return;
        	}
        	FileInputStream fin = null;
			try {
				fin = new FileInputStream(f);
			} catch (FileNotFoundException e) {}
        	BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        	
        	String line = "";
        	
        	try {
				while ((line = br.readLine()) != null) {
					String [] t = line.split(",");
					if (!t[0].equals(tpath[1]) || !t[1].equals(tpath[2])) continue;
					
					// Get remote resource path.
					String remotePath = "";
					for (int i = 0; i < xpath.length; i++) {
						remotePath += xpath[i] + "/";
					}
					remotePath = remotePath.substring(0, remotePath.length()-1);
					
					String ans = sendTcpPacket(t[2], 11314, "GET|FILE|METADATA|P2P|" + remotePath);
					if (ans.equals("POST|FILE|METADATA|P2P|NO_RESOURCE")) {
						Toast.makeText(getApplicationContext(), "Remote resource not found...", Toast.LENGTH_LONG).show();
						continue;
					}
					if (ans.startsWith("POST|FILE|METADATA|P2P|")) {
						String [] meta = ans.split("\\|")[4].split("%");
						
						final String [] attr = new String[5];
						attr[0] = "File Size: "+meta[0]+" KB";
						attr[1] = "Writable: "+(meta[1]=="w" ? "Yes":"No");
						attr[2] = "Readable: "+(meta[2]=="r" ? "Yes":"No");
						attr[3] = "Executable: "+(meta[3]=="x" ? "Yes":"No");
						attr[4] = "Last Modified Date: "+meta[4];
						AlertDialog.Builder metaBuilderP2P = new AlertDialog.Builder(this);
						metaBuilderP2P.setTitle("File Metadata");
						metaBuilderP2P.setItems(attr, new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int item) {
						    	
						    }
						});
						metaBuilderP2P.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					        	   dialog.dismiss();
					           }
					    });
						metaBuilderP2P.create().show();
					}
					return;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// ---------------------------------------------------------------
		if (reply.endsWith("NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Metadata not availble now!", Toast.LENGTH_LONG).show();
		}
		else if (reply.startsWith("POST|FILE|METADATA")) {
			String [] meta = reply.split("\\|")[3].split("%");
			final String [] attr = new String[5];
			attr[0] = "File Size: "+meta[0]+" KB";
			attr[1] = "Writable: "+(meta[1]=="w" ? "Yes":"No");
			attr[2] = "Readable: "+(meta[2]=="r" ? "Yes":"No");
			attr[3] = "Executable: "+(meta[3]=="x" ? "Yes":"No");
			attr[4] = "Last Modified Date: "+meta[4];
			AlertDialog.Builder metaBuilder = new AlertDialog.Builder(this);
			metaBuilder.setTitle("File Metadata");
			metaBuilder.setItems(attr, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	
			    }
			});
			metaBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.dismiss();
		           }
		    });
			metaBuilder.create().show();
		}
	}
	
	private void pinNetworkFile(int pos) {
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|FILE|PIN|"+ 
				NetworkPwdChild.get(pos).ResourceName+"|"+NetworkPwdChild.get(pos).ResourceGlobalId);
		
		/*
		 * Here trigger P2P mode.
		 * ---------------------------------------------------------
		 * */
		if (reply == null) {
			String path = NetworkPwd + "/" +NetworkPwdChild.get(pos).ResourceName;
			String [] tpath = path.split("/");
			String [] xpath = new String[tpath.length-2];
			for (int i = 0, k = 0; i < tpath.length; i++) {
				if (i == 1 || i == 2) continue;
				xpath[k++] = tpath[i];
			}
			
			File f = new File("/mnt/sdcard/Uno/p2p.ini");
        	if (!f.exists()) {
        		Toast.makeText(getApplicationContext(), "System failure...", Toast.LENGTH_LONG).show();
        		return;
        	}
        	FileInputStream fin = null;
			try {
				fin = new FileInputStream(f);
			} catch (FileNotFoundException e) {}
        	BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        	
        	String line = "";
        	
        	try {
				while ((line = br.readLine()) != null) {
					String [] t = line.split(",");
					if (!t[0].equals(tpath[1]) || !t[1].equals(tpath[2])) continue;
					
					// Get remote resource path.
					String remotePath = "";
					for (int i = 0; i < xpath.length; i++) {
						remotePath += xpath[i] + "/";
					}
					final String targetPath = remotePath.substring(0, remotePath.length()-1);
					final String targetIp = t[2];
					new Runnable () {

						public void run() {
							try
				        	{
				        		InetAddress remote = InetAddress.getByName(targetIp);
				        		Socket s = new Socket(remote, 11314);
				        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
				        		out.println("PIN|FILE|"+targetPath);

				        		InputStream sin = s.getInputStream();
				        		String PinName = targetPath.substring(targetPath.lastIndexOf("/")+1, targetPath.length())+"_P2P";
				        		File PinDir = new File("/mnt/sdcard/Uno/Pin");
				        		if (!PinDir.exists()) PinDir.mkdirs();
				        		byte [] buf = new byte[s.getReceiveBufferSize()];
				        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(PinDir, PinName), true)); // append.
				        		
				        		while (true)
				        		{
				        			int nbytes = sin.read(buf);
				        			if (nbytes < 0) break;
				        			bout.write(buf, 0, nbytes);
				        			bout.flush();
				        		}
				        		bout.close();
				        		
				        		// Update database.
				        		String [] row = new String[3];
								row[0] = "-1";
								row[1] = "/mnt/sdcard/Uno/Pin/"+PinName;
								row[2] = PinName;
								pdbh.insertRow(row);
								
								Toast.makeText(getApplicationContext(), "File has been thumbtacked...", Toast.LENGTH_LONG).show();
				        	}
				        	catch (Exception e)
				        	{
				        		Log.e("SocketFile", e.toString());
				        	}
						}
						
					}.run();
				}
				return;
        	}
        	catch(Exception e){}
		}
		
		// ---------------------------------------------------------
		if (reply.endsWith("NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Metadata not availble now!", Toast.LENGTH_LONG).show();
		}
		else if (reply.startsWith("POST|FILE")) {
			String [] tmp = reply.split("\\|");
			final String resIp = tmp[2];
			final String path = tmp[3];
			final String id = NetworkPwdChild.get(pos).ResourceGlobalId;
			
			/*
			 * Use the following thread to retrieve file and update database.
			 * */ 
			new Runnable () {

				public void run() {
					try
		        	{
		        		InetAddress remote = InetAddress.getByName(resIp);
		        		Socket s = new Socket(remote, 11314);
		        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		        		out.println("PIN|FILE|"+path);

		        		InputStream sin = s.getInputStream();
		        		String PinName = path.substring(path.lastIndexOf("/")+1, path.length())+"_"+id;
		        		File PinDir = new File("/mnt/sdcard/Uno/Pin");
		        		if (!PinDir.exists()) PinDir.mkdirs();
		        		byte [] buf = new byte[s.getReceiveBufferSize()];
		        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(PinDir, PinName), true)); // append.
		        		
		        		while (true)
		        		{
		        			int nbytes = sin.read(buf);
		        			if (nbytes < 0) break;
		        			bout.write(buf, 0, nbytes);
		        			bout.flush();
		        		}
		        		bout.close();
		        		
		        		// Update database.
		        		String [] row = new String[3];
						row[0] = id;
						row[1] = "/mnt/sdcard/Uno/Pin/"+PinName;
						row[2] = PinName;
						pdbh.insertRow(row);
						
						Toast.makeText(getApplicationContext(), "File has been thumbtacked...", Toast.LENGTH_LONG).show();
		        	}
		        	catch (Exception e)
		        	{
		        		Log.e("SocketFile", e.toString());
		        	}
				}
				
			}.run();
		}
	}
	
	private void showSensorInstance(int pos) {
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|SENSOR|"+NetworkPwdChild.get(pos).ResourceName+
				"|"+NetworkPwdChild.get(pos).ResourceGlobalId);
		
		/*
		 * P2P mode to retrieve instant sensor data values.
		 * */
		if (reply == null) {
			String targetHolder = NetworkPwd.split("/")[1];
			String targetDevice = NetworkPwd.split("/")[2];
			
			File f = new File("/mnt/sdcard/Uno/p2p.ini");
        	if (!f.exists()) {
        		Toast.makeText(getApplicationContext(), "System failure...", Toast.LENGTH_LONG).show();
        		return;
        	}
        	FileInputStream fin = null;
			try {
				fin = new FileInputStream(f);
			} catch (FileNotFoundException e) {}
        	BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        	
        	String line = "";
        	
        	try {
				while ((line = br.readLine()) != null) {
					String [] t = line.split(",");
					if (!t[0].equals(targetHolder) || !t[1].equals(targetDevice)) continue;
					
					String ans = sendTcpPacket(t[2], 11314, "GET|SENSOR|P2P|"+NetworkPwdChild.get(pos).ResourceName);
					if (ans == null) continue;
					if (ans.equals("POST|SENSOR|P2P|NO_RESOURCE")) {
						Toast.makeText(getApplicationContext(), "Target sensor not reachable...", Toast.LENGTH_LONG).show();
						continue;
					}
					String [] tmp = ans.split("\\|")[3].split("%");
					String [] val = new String[3];
					val[0] = "X: "+tmp[0];
					val[1] = "Y: "+tmp[1];
					val[2] = "Z: "+tmp[2];
					AlertDialog.Builder sensorInsBuilder = new AlertDialog.Builder(this);
					sensorInsBuilder.setTitle("Sensor Instant Values");
					sensorInsBuilder.setItems(val, new OnClickListener () {
		
						public void onClick(DialogInterface arg0, int arg1) {
							
						}});
					sensorInsBuilder.setNegativeButton("OK", new OnClickListener () {
		
						public void onClick(DialogInterface arg0, int arg1) {
							arg0.dismiss();
							
						}});
					sensorInsBuilder.create().show();
				}
				return;
        	}
        	catch(Exception e) {}
		}
		
		// ------------------------------------------------
		if (reply.startsWith("POST|SENSOR|")) {
			/*
			 * First go to target mobile device to get sensor values.
			 * */
			String targetIp = reply.split("\\|")[2];
			String msg = "GET|SENSOR|"+NetworkPwdChild.get(pos).ResourceName;
			String response = sendTcpPacket(targetIp, 11314, msg);
			if (response == null) return;
			
			if (response.startsWith("POST|SENSOR|")) {
				String [] tmp = response.split("\\|")[2].split("%");
				String [] val = new String[3];
				val[0] = "X: "+tmp[0];
				val[1] = "Y: "+tmp[1];
				val[2] = "Z: "+tmp[2];
				AlertDialog.Builder sensorInsBuilder = new AlertDialog.Builder(this);
				sensorInsBuilder.setTitle("Sensor Instant Values");
				sensorInsBuilder.setItems(val, new OnClickListener () {
	
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						
					}});
				sensorInsBuilder.setNegativeButton("OK", new OnClickListener () {
	
					public void onClick(DialogInterface arg0, int arg1) {
						arg0.dismiss();
						
					}});
				sensorInsBuilder.create().show();
			}
		}
	}
	
	private void RemoteFilePreviewProcess(int pos) {
		
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|FILE|PREVIEW|"+ 
				NetworkPwdChild.get(pos).ResourceName+"|"+NetworkPwdChild.get(pos).ResourceGlobalId);
		
		/*
		 * Here is the P2P part for File Preview.
		 * ----------------------------------------------------------------
		 * */		
		if (reply == null) {
			String path = NetworkPwd + "/" +NetworkPwdChild.get(pos).ResourceName;
			String [] tpath = path.split("/");
			String [] xpath = new String[tpath.length-2];
			for (int i = 0, k = 0; i < tpath.length; i++) {
				if (i == 1 || i == 2) continue;
				xpath[k++] = tpath[i];
			}
			
			File f = new File("/mnt/sdcard/Uno/p2p.ini");
        	if (!f.exists()) {
        		Toast.makeText(getApplicationContext(), "System failure...", Toast.LENGTH_LONG).show();
        		return;
        	}
        	FileInputStream fin = null;
			try {
				fin = new FileInputStream(f);
			} catch (FileNotFoundException e) {}
        	BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        	
        	String line = "";
        	
        	try {
				while ((line = br.readLine()) != null) {
					String [] t = line.split(",");
					if (!t[0].equals(tpath[1]) || !t[1].equals(tpath[2])) continue;
					
					String remotePath = "";
					for (int i = 0; i < xpath.length; i++) {
						remotePath += xpath[i] + "/";
					}
					
					final String targetIp = t[2];
					final String targetPath = remotePath.substring(0, remotePath.length()-1);
					final String previewPath = "/mnt/sdcard/Uno/Preview/"+targetPath.substring(targetPath.lastIndexOf("/")+1, targetPath.length())+"_p2p";
					
					new Runnable () {

						public void run() {
							try
				        	{
				        		InetAddress remote = InetAddress.getByName(targetIp);
				        		Socket s = new Socket(remote, 11314);
				        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
				        		out.println("PREVIEW|FILE|"+targetPath);

				        		InputStream sin = s.getInputStream();
				        		String PreviewName = targetPath.substring(targetPath.lastIndexOf("/")+1, targetPath.length())+"_p2p";
				        		File PreviewDir = new File("/mnt/sdcard/Uno/Preview");
				        		if (!PreviewDir.exists()) PreviewDir.mkdirs();
				        		byte [] buf = new byte[s.getReceiveBufferSize()];
				        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(PreviewDir, PreviewName), true)); // append.
				        		
				        		while (true)
				        		{
				        			int nbytes = sin.read(buf);
				        			if (nbytes < 0) break;
				        			bout.write(buf, 0, nbytes);
				        			bout.flush();
				        		}
				        		bout.close();
				        		
				        		// Update database.
				        		String [] row = new String[3];
								row[0] = "-1";
								row[1] = "/mnt/sdcard/Uno/Preview/"+PreviewName;
								row[2] = PreviewName;
								pdbh.insertRow(row);
								
								Toast.makeText(getApplicationContext(), "Retrieve Finished, start previewing...", Toast.LENGTH_LONG).show();
				        	}
				        	catch (Exception e)
				        	{
				        		Log.e("SocketFile", e.toString());
				        	}
						}
						
					}.run();
					
					// Start showing file preview
					showPreview(previewPath);
				}
				return;
        	}
        	catch(Exception e){}
		}
		// ----------------------------------------------------------------
		
		if (reply.endsWith("NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Metadata not availble now!", Toast.LENGTH_LONG).show();
		}
		else if (reply.startsWith("POST|FILE")) {
			String [] tmp = reply.split("\\|");
			final String resIp = tmp[2];
			final String path = tmp[3];
			final String id = NetworkPwdChild.get(pos).ResourceGlobalId;
			final String previewPath = "/mnt/sdcard/Uno/Preview/"+path.substring(path.lastIndexOf("/")+1, path.length())+"_"+id;
			
			/*
			 * Use the following thread to retrieve file and update database.
			 * */ 
			new Runnable () {

				public void run() {
					try
		        	{
		        		InetAddress remote = InetAddress.getByName(resIp);
		        		Socket s = new Socket(remote, 11314);
		        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		        		out.println("PREVIEW|FILE|"+path);

		        		InputStream sin = s.getInputStream();
		        		String PreviewName = path.substring(path.lastIndexOf("/")+1, path.length())+"_"+id;
		        		File PreviewDir = new File("/mnt/sdcard/Uno/Preview");
		        		if (!PreviewDir.exists()) PreviewDir.mkdirs();
		        		byte [] buf = new byte[s.getReceiveBufferSize()];
		        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(PreviewDir, PreviewName), true)); // append.
		        		
		        		while (true)
		        		{
		        			int nbytes = sin.read(buf);
		        			if (nbytes < 0) break;
		        			bout.write(buf, 0, nbytes);
		        			bout.flush();
		        		}
		        		bout.close();
		        		
		        		// Update database.
		        		String [] row = new String[3];
						row[0] = id;
						row[1] = "/mnt/sdcard/Uno/Preview/"+PreviewName;
						row[2] = PreviewName;
						pdbh.insertRow(row);
						
						Toast.makeText(getApplicationContext(), "Retrieve Finished, start previewing...", Toast.LENGTH_LONG).show();
		        	}
		        	catch (Exception e)
		        	{
		        		Log.e("SocketFile", e.toString());
		        	}
				}
				
			}.run();
			
			// Start showing file preview
			showPreview(previewPath);
		}
		
		
	}
	
	private void showPreview(String path) {

		if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".jpe") || 
				path.endsWith(".jfif") || path.endsWith(".gif") || path.endsWith(".tif") || path.endsWith(".tiff") ||
				path.endsWith(".bmp")) {
			Intent intent = new Intent(UnoNetwork.this, imgPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			UnoNetwork.this.startActivity(intent);
		}
		else if (path.endsWith(".txt")) {
			Intent intent = new Intent(UnoNetwork.this, txtPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			UnoNetwork.this.startActivity(intent);
		}
		/*else if (path.endsWith(".pdf")) {
			Intent intent = new Intent(UnoNetwork.this, pdfPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			UnoNetwork.this.startActivity(intent);
		}*/
		else if (path.equals("TYPE_ACCELEROMETER") || path.equals("TYPE_GRAVITY") || path.equals("TYPE_GYROSCOPE") ||
				path.equals("TYPE_LIGHT") || path.equals("TYPE_MAGNETIC_FIELD") || path.equals("TYPE_ORIENTATION") ||
				path.equals("TYPE_PRESSURE") || path.equals("TYPE_PROXIMITY") || path.equals("TYPE_LINEAR_ACCELERATION") ||
				path.equals("TYPE_ROTATION_VECTOR") || path.equals("TYPE_TEMPERATURE")) {
			Intent intent = new Intent(UnoNetwork.this, txtPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			UnoNetwork.this.startActivity(intent);
		}
		else {
			Toast.makeText(getApplicationContext(), "Not support this file type...", Toast.LENGTH_LONG).show();
			Vibrator vbr = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vbr.vibrate(300);
		}
	}
	
	/*
	 * Logging operations, username can be found in /mnt/sdcard/Uno/sys.ini
	 * */
	private void startLoggingProcess(int pos) {
		String sensor = NetworkPwdChild.get(pos).ResourceName;
		String sensorid = NetworkPwdChild.get(pos).ResourceGlobalId;
		
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "GET|SENSOR|LOG|"+sensor+"|"+sensorid);
		
		/*
		 * P2P mode.
		 * */
		if (reply == null) {
			return;
		}
		// ----------------------------------
		
		if (reply.equals("POST|SENSOR|LOG|NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Sensor is not availble...", Toast.LENGTH_LONG).show();
			return;
		}
		if (reply.startsWith("POST|SENSOR|LOG|")) {
			String targetIp = reply.split("\\|")[3];
			String response = sendTcpPacket(targetIp, 11314, "GET|SENSOR|LOG|START|"+sensor+"|"+Owner);
			if (response == null) {
				Toast.makeText(getApplicationContext(), "No response from target sensor...", Toast.LENGTH_LONG).show();
				return;
			}
			
			// This function is not ready yet. It will trigger new log.
			if (response.equals("POST|SENSOR|LOG|IN_GOING")) {
				Toast.makeText(getApplicationContext(), "You are already in use of the sensor...", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (response.equals("POST|SENSOR|LOG|OK")) {
				Toast.makeText(getApplicationContext(), "Start sensing for you...", Toast.LENGTH_LONG).show();
				return;
			}
		}
	}
	
	private void stopLoggingProcess(int pos) {
		String sensor = NetworkPwdChild.get(pos).ResourceName;
		String sensorid = NetworkPwdChild.get(pos).ResourceGlobalId;
		
		String reply = sendTcpPacket(UnoConstant.GOVERNOR_ADDRESS, 11314, "GET|SENSOR|LOG|"+sensor+"|"+sensorid);
		
		/*
		 * P2P mode.
		 * */
		if (reply == null) {
			return;
		}
		// ----------------------------------
		
		if (reply.equals("POST|SENSOR|LOG|NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Sensor is not availble...", Toast.LENGTH_LONG).show();
			return;
		}
		if (reply.startsWith("POST|SENSOR|LOG|")) {
			String targetIp = reply.split("\\|")[3];
			String response = sendTcpPacket(targetIp, 11314, "GET|SENSOR|LOG|STOP|"+sensor+"|"+Owner);
			if (response == null) {
				Toast.makeText(getApplicationContext(), "No response from target sensor...", Toast.LENGTH_LONG).show();
				return;
			}
			if (response.equals("POST|SENSOR|LOG|OK")) {
				Toast.makeText(getApplicationContext(), "Stop sensing for you and fetch log...", Toast.LENGTH_LONG).show();
				
				final String resIp = targetIp;
				final String targetSensor = sensor;
				final String targetId = sensorid;
				
				/*
				 * New runnable thread to retreive a sensor log.
				 * */
				
				new Runnable () {

					public void run() {
						try
			        	{
			        		InetAddress remote = InetAddress.getByName(resIp);
			        		Socket s = new Socket(remote, 11314);
			        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
			        		out.println("PIN|SENSOR_LOG|"+targetSensor+"|"+Owner);

			        		InputStream sin = s.getInputStream();
			        		String SensorLogName = targetSensor + "_" + String.valueOf(new Date().getTime());
			        		File SensorLogDir = new File("/mnt/sdcard/Uno/Pin/SensorLogs");
			        		if (!SensorLogDir.exists()) SensorLogDir.mkdirs();
			        		byte [] buf = new byte[s.getReceiveBufferSize()];
			        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(SensorLogDir, SensorLogName), true)); // append.
			        		
			        		while (true)
			        		{
			        			int nbytes = sin.read(buf);
			        			if (nbytes < 0) break;
			        			bout.write(buf, 0, nbytes);
			        			bout.flush();
			        		}
			        		bout.close();
			        		
			        		// Update database.
			        		String [] row = new String[3];
							row[0] = targetId;
							row[1] = "/mnt/sdcard/Uno/Pin/SensorLogs/"+SensorLogName;
							row[2] = SensorLogName;
							pdbh.insertRow(row);
							
							Toast.makeText(getApplicationContext(), "Sensor Log fetched...", Toast.LENGTH_LONG).show();
			        	}
			        	catch (Exception e)
			        	{
			        		Log.e("SocketFile", e.toString());
			        	}
					}
					
				}.run();
			}
		}
	}
	
	/*
	 * These code is for evaluation the responding time of the system.
	 * */
    private File trackFile = null;
	private BufferedWriter trackBW = null;
	
    private long startRespondingTimeTrack() {
    	trackFile = new File("/mnt/sdcard/Uno/responding_time.txt");
    	if (!trackFile.exists()) {
			try {
				trackFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		try {
			trackBW = new BufferedWriter(new FileWriter(trackFile, true));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (new Date()).getTime();
    }
    
    private void stopRespondingTimeTrack(String type, long startTime) {
    	long duration = new Date().getTime() - startTime;
    	try {
			trackBW.write(type + "," + String.valueOf(duration));
    		trackBW.newLine();
			trackBW.flush();
			trackBW.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
