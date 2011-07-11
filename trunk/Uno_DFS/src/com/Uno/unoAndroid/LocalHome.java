package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocalHome extends ListActivity {
	
	private File pwd = null;
	private String pwdString = "/sdcard";
	private ArrayAdapter <String> adapter = null;
	private ArrayList <String> pwdChildString = new ArrayList <String> ();
	private ArrayList <File> pwdChild = new ArrayList <File> ();
	private ProgressDialog pgDialog;
	private static String GOVERNOR_IP = UnoConstant.GOVERNOR_ADDRESS;
	private LocalResourceDatabaseHelper resdbh = null;
	
	private String Owner = null;
	private String Device = null;
	
	private void GovernorMessageParser(String msg) {
		String [] argv = msg.split("\\|");
		int argc = argv.length;
		//pgDialog.cancel();
		if (argc == 1) {
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		}
		else if (argc == 2) {
			
		}
		else if (argc == 3) {
			if (argv[0].equals("SETPUBLIC")) {
				if (argv[1].equals("FILE")) {
					if (argv[2].equals("DONE")) {
						if (pgDialog.isShowing()) pgDialog.cancel();
						Toast.makeText(getApplicationContext(), "File has been shared to public", Toast.LENGTH_LONG).show();
					}
				}
			}
			else if (argv[0].equals("OFFLINE")) {
				if (argv[1].equals("FILE")) {
					if (argv[2].equals("DONE")) {
						if (pgDialog.isShowing()) pgDialog.cancel();
						Toast.makeText(getApplicationContext(), "File has been set to offline", Toast.LENGTH_LONG).show();
					}
				}
			}
			else if (argv[0].equals("SETPRIVATE")) {
				if (argv[1].equals("FILE")) {
					if (argv[2].equals("DONE")) {
						if (pgDialog.isShowing()) pgDialog.cancel();
						Toast.makeText(getApplicationContext(), "File has been shared to your friends", Toast.LENGTH_LONG).show();
					}
				}
			}
		}
	}
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		resdbh = new LocalResourceDatabaseHelper(this);
		initLoginInfo();
				
		pwdChild.clear();
		pwdChildString.clear();
		
		pwd = new File(pwdString);
		pwdChild.add(pwd);
		for (File f : pwd.listFiles())
			pwdChild.add(f);

		for (File f : pwdChild) {
			if (f == pwd) pwdChildString.add("..");
			else if (f.isDirectory()) pwdChildString.add(f.getName()+"/");
			else pwdChildString.add(f.getName());
		}
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pwdChildString);
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Object o = this.getListAdapter().getItem(position);
		String keyword = o.toString();
		
		if (keyword.endsWith("/"))  {
			
			pwdString += "/"+keyword.substring(0, keyword.length()-1);
			
			pwdChild.clear();
			pwdChildString.clear();
			
			pwd = new File(pwdString);
			pwdChild.add(pwd);
			for (File f : pwd.listFiles())
				pwdChild.add(f);

			for (File f : pwdChild) {
				if (f == pwd) pwdChildString.add("..");
				else if (f.isDirectory()) pwdChildString.add(f.getName()+"/");
				else pwdChildString.add(f.getName());
			}
			adapter.notifyDataSetChanged();
		}
		else if (keyword.equals("..")) {
			pwd = pwd.getParentFile();
			if (pwd == null) pwd = new File("/");
			pwdString = pwd.getAbsolutePath();
			
			pwdChild.clear();
			pwdChildString.clear();
			
			pwdChild.add(pwd);
			for (File f : pwd.listFiles())
				pwdChild.add(f);

			for (File f : pwdChild) {
				if (f == pwd) pwdChildString.add("..");
				else if (f.isDirectory()) pwdChildString.add(f.getName()+"/");
				else pwdChildString.add(f.getName());
			}
			adapter.notifyDataSetChanged();
		}
		else {	
			//final String selectedFileName = keyword;
			final int pos = position;
			final String [] options = {"File Metadata", "Share", "Preview"};
			AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
			optBuilder.setTitle("Actions");
			optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if (item == 0) {
			    		dialog.dismiss();
			    		//String x = pwd.getAbsolutePath() + "/" + selectedFileName;
			    		showFileMetadata(pwdChild.get(pos).getAbsolutePath());
			    	}
			    	else if (item == 1){
			    		dialog.dismiss();
			    		showFileShare(pwdChild.get(pos).getAbsolutePath());
			    	}
			    	else {
			    		dialog.dismiss();
			    		showPreview(pwdChild.get(pos).getAbsolutePath());
			    	}
			    }
			});
			optBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.dismiss();
		           }
		    });
			optBuilder.create().show();
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
	
	private void showFileMetadata(String path) {
		String [] meta = getLocalFileMetadata(path);
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
		metaBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.dismiss();
	           }
	    });
		metaBuilder.create().show();
	}
	
	private void showFileShare(String path) {
		final String filepath = path;
		final String [] methods = {"Share to Everyone", "Share to Friends", "Offline"};
		AlertDialog.Builder shareBuilder = new AlertDialog.Builder(this);
		shareBuilder.setTitle("Share Methods");
		shareBuilder.setItems(methods, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	if (item == 0) {
		    		dialog.dismiss();
		    		String [] rawMeta = getLocalFileMetadata(filepath);
		    		String metadata = "";
		    		for (String str : rawMeta) {
		    			metadata += str + "%";
		    		}
		    		metadata = metadata.substring(0, metadata.length()-1);

		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPUBLIC|FILE|"+filepath+"|"+metadata);
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
		    		
		    		/*
		    		 * Update Local Database here.
		    		 * */
		    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+filepath+"'");
		    		int n = resdbh.countRow(c);
		    		
		    		if (n == 0) {
			    		String [] row = new String[6];
			    		row[0] = Owner;
			    		row[1] = Device;
			    		row[2] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
			    		row[3] = filepath;
			    		row[4] = "1";
			    		row[5] = metadata;
			    		resdbh.insertRow(row);
		    		}
		    		else {
		    			String [] row = resdbh.fetchOneRow(c);
		    			row[1] = Owner;
			    		row[2] = Device;
			    		row[3] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
			    		row[4] = filepath;
			    		row[5] = "1";
			    		row[6] = metadata;
		    			resdbh.updateRow(row);
		    		}
		    	}
		    	else if (item == 1) {
		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		showAccessList(filepath);
		    		
		    		/*
		    		 * Local Database handled in the showAccessList().
		    		 * */
		    	}
		    	else {
		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "OFFLINE|FILE|"+filepath);
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
		    		
		    		/*
		    		 * Update Local Database here.
		    		 * */
		    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+filepath+"'");
		    		int n = resdbh.countRow(c);
		    		
		    		if (n == 0) {
			    		String [] row = new String[6];
			    		row[0] = Owner;
			    		row[1] = Device;
			    		row[2] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
			    		row[3] = filepath;
			    		row[4] = "0";
			    		row[5] = "";
			    		resdbh.insertRow(row);
		    		}
		    		else {
		    			String [] row = resdbh.fetchOneRow(c);
		    			row[1] = Owner;
			    		row[2] = Device;
			    		row[3] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
			    		row[4] = filepath;
			    		row[5] = "0";
			    		row[6] = "";
		    			resdbh.updateRow(row);
		    		}
		    	}
		    }
		});
		shareBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.dismiss();
	           }
	    });
		shareBuilder.create().show();
	}
	
	private void showAccessList(String path) {
		AlertDialog.Builder accBuilder;
		final AlertDialog accAlert;
		final String filepath = path;

		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.accesslistdiag, (ViewGroup) findViewById(R.id.access_list_root));
		final EditText acclist = (EditText) layout.findViewById (R.id.accesslist_emails);
		Button btOk = (Button) layout.findViewById (R.id.access_list_button_ok);
		Button btCancel = (Button) layout.findViewById (R.id.access_list_button_cancel);
		accBuilder = new AlertDialog.Builder(this);
		accBuilder.setView(layout);
		accBuilder.setTitle("Share to Your Friends");
		accAlert = accBuilder.create();
		accAlert.show();
		
		btOk.setOnClickListener(new OnClickListener () {

			//@Override
			public void onClick(View arg0) {
				accAlert.dismiss();
				
				String accesslist = acclist.getText().toString().replace(";", "&");
				String [] rawMeta = getLocalFileMetadata(filepath);
	    		String metadata = "";
	    		for (String str : rawMeta) {
	    			metadata += str + "%";
	    		}
	    		metadata = metadata.substring(0, metadata.length()-1);

	    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPRIVATE|FILE|"+filepath+"|"+metadata+"|"+accesslist);
	    		if (reply == null) return;
	    		GovernorMessageParser(reply);
	    		
	    		/*
	    		 * Update Local Database here.
	    		 * */
	    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+filepath+"'");
	    		int n = resdbh.countRow(c);
	    		
	    		if (n == 0) {
		    		String [] row = new String[6];
		    		row[0] = Owner;
		    		row[1] = Device;
		    		row[2] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
		    		row[3] = filepath;
		    		row[4] = accesslist;
		    		row[5] = metadata;
		    		resdbh.insertRow(row);
	    		}
	    		else {
	    			String [] row = resdbh.fetchOneRow(c);
	    			row[1] = Owner;
		    		row[2] = Device;
		    		row[3] = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
		    		row[4] = filepath;
		    		row[5] = accesslist;
		    		row[6] = metadata;
	    			resdbh.updateRow(row);
	    		}
			}});
		btCancel.setOnClickListener(new OnClickListener () {

			//@Override
			public void onClick(View arg0) {
				pgDialog.dismiss();
				accAlert.dismiss();
			}});
	}
	
	private void showPreview(String path) {

		if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".jpe") || 
				path.endsWith(".jfif") || path.endsWith(".gif") || path.endsWith(".tif") || path.endsWith(".tiff") ||
				path.endsWith(".bmp")) {
			Intent intent = new Intent(LocalHome.this, imgPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			LocalHome.this.startActivity(intent);
		}
		else if (path.endsWith(".txt")) {
			Intent intent = new Intent(LocalHome.this, txtPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			LocalHome.this.startActivity(intent);
		}
		/*else if (path.endsWith(".pdf")) {
			Intent intent = new Intent(LocalHome.this, pdfPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			LocalHome.this.startActivity(intent);
		}*/
		else if (path.equals("TYPE_ACCELEROMETER") || path.equals("TYPE_GRAVITY") || path.equals("TYPE_GYROSCOPE") ||
				path.equals("TYPE_LIGHT") || path.equals("TYPE_MAGNETIC_FIELD") || path.equals("TYPE_ORIENTATION") ||
				path.equals("TYPE_PRESSURE") || path.equals("TYPE_PROXIMITY") || path.equals("TYPE_LINEAR_ACCELERATION") ||
				path.equals("TYPE_ROTATION_VECTOR") || path.equals("TYPE_TEMPERATURE")) {
			Intent intent = new Intent(LocalHome.this, txtPreview.class);
			intent.putExtra("PREVIEW_PATH", path);
			LocalHome.this.startActivity(intent);
		}
		else {
			Toast.makeText(getApplicationContext(), "Not support this file type...", Toast.LENGTH_LONG).show();
			Vibrator vbr = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vbr.vibrate(300);
		}
	}
	
	private String [] getLocalFileMetadata (String path)
    {
    	File f = new File(path);
    	if (!f.exists()) return null;
    	String [] metadata = new String[5];

    	try{
    		metadata[0] = String.valueOf(new FileInputStream(f).available()/1024);
    	} catch (Exception e) {}
    	metadata[1] = (f.canWrite() ? "w":"nw");
    	metadata[2] = (f.canRead() ? "r":"nr");
    	metadata[3] = (f.canExecute() ? "x":"nx");
    	Date d = new Date(f.lastModified());
    	metadata[4] = d.toGMTString();
    	
    	return metadata;

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
}
