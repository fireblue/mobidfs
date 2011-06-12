package com.Uno.unoAndroid;

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
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
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
	private final String GOVERNOR_IP = "192.168.10.115";
	private ArrayAdapter adapter;
	private final PinDatabaseHelper pdbh = new PinDatabaseHelper(this);
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
		
		final String keyword = this.getListAdapter().getItem(position).toString();
		if (keyword.equals("..")) {
			if (NetworkPwd.equals("/")) return;
			NetworkPwd = NetworkPwd.substring(0, NetworkPwd.lastIndexOf("/")-1);
			NetworkPwdChild = fetchPwdChildList(NetworkPwd);
			for (NetworkItem ni : NetworkPwdChild) {
	        	String s = ni.ResourceName;
	        	NetworkPwdChildString.add(s);
	        }
			adapter.notifyDataSetChanged();
		}
		else if (keyword.endsWith("/")) {
			/*
			 * Two cases here: sensor directory and common directory.
			 * Sensor file needs sensor ID.
			 * */
			if (keyword.equals("Sensor/")) {
				
			}
			else {
				if (NetworkPwd.endsWith("/")) NetworkPwd += keyword.substring(0, keyword.length()-2);
				else NetworkPwd += "/" + keyword.substring(0, keyword.length()-2);
				NetworkPwdChild = fetchPwdChildList(NetworkPwd);
				for (NetworkItem ni : NetworkPwdChild) {
		        	String s = ni.ResourceName;
		        	NetworkPwdChildString.add(s);
		        }
				adapter.notifyDataSetChanged();
			}
		}
		else {
			/*
			 * Two cases here: sensor file and common file
			 * Sensor file use sensor ID.
			 * */
			if (NetworkPwd.endsWith("/Sensor")) {
				
			}
			else {
				final int pos = position;
				final String [] options = {"File Metadata", "File Preview", "Thumbtack"};
				AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
				optBuilder.setTitle("Actions");
				optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	if (item == 0) {
				    		showNetworkFileMetadata(pos);
				    	}
				    	else if (item == 1){
				    		// TODO preview file.
				    	}
				    	else {
				    		pinNetworkFile(pos);
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
	
	/*
	 * Given a pwd and fetch all children inside this directory.
	 * */
	private ArrayList <NetworkItem> fetchPwdChildList (String pwd) {
		ArrayList <NetworkItem> pwdchild = new ArrayList <NetworkItem> ();
		
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|DIR|"+pwd);
        if (reply == null) return null;
        NetworkItem p = new NetworkItem();
        
        // Add ".."
        p.ResourceName = "..";
        p.ResourceGlobalId = "-1";
        pwdchild.add(p);
        
        if (reply.startsWith("POST|DIR|")) {
        	String [] argv = reply.split("\\|");
        	String [] tlist = argv[2].split(";");
        	for (String s : tlist) {
        		NetworkItem ni = new NetworkItem();
        		String [] t = s.split("^");
        		ni.ResourceName = t[0];
        		ni.ResourceGlobalId = t[1]; // Global ID is -1 for directories.
        		pwdchild.add(ni);
        	}
        	return pwdchild;
        }
        return null;
        
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
				NetworkPwdChild.get(pos-1).ResourceName+"|"+NetworkPwdChild.get(pos-1).ResourceGlobalId);
		if (reply == null) return;
		if (reply.endsWith("NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Metadata not availble now!", Toast.LENGTH_LONG).show();
		}
		else if (reply.startsWith("POST|FILE|METADATA")) {
			String [] meta = reply.split("|")[3].split("%");
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
				NetworkPwdChild.get(pos-1).ResourceName+"|"+NetworkPwdChild.get(pos-1).ResourceGlobalId);
		if (reply == null) return;
		if (reply.endsWith("NO_RESOURCE")) {
			Toast.makeText(getApplicationContext(), "Metadata not availble now!", Toast.LENGTH_LONG).show();
		}
		else if (reply.startsWith("POST|FILE")) {
			String [] tmp = reply.split("\\|");
			final String resIp = tmp[3];
			final String path = tmp[4];
			final String id = NetworkPwdChild.get(pos-1).ResourceGlobalId;
			
			/*
			 * Use the following thread to retrieve file and update database.
			 * */ 
			new Runnable () {

				@Override
				public void run() {
					try
		        	{
		        		InetAddress remote = InetAddress.getByName(resIp);
		        		Socket s = new Socket(remote, 11316);
		        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		        		out.println("PIN|FILE|"+path);

		        		InputStream sin = s.getInputStream();
		        		String PinName = path.substring(path.lastIndexOf("/")+1, path.length()-1)+"_"+id;
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
}
