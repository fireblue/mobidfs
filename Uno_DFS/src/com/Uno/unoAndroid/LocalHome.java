package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import android.os.Bundle;
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
	//private final static String UI_MESSAGE_ACTION = "com.UnoAndroid.UI_MSG_TO_SERVICE";
	//private final static String SERVICE_MESSAGE_ACTION = "com.UnoAndroid.SERVICE_MSG_TO_UI";
	private static String GOVERNOR_IP = "192.168.10.160";
	/*private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver () {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().endsWith(SERVICE_MESSAGE_ACTION)) {
				if (arg1.getExtras().containsKey("GOVERNOR_MSG_STREAM_TO_UI")) {
					GovernorMessageParser(arg1.getExtras().getString("GOVERNOR_MSG_STREAM_TO_UI"));
				}
				else if (arg1.getExtras().containsKey("SERVICE_MSG_STREAM_TO_UI")) {
					ServiceMessageParser(arg1.getExtras().getString("SERVICE_MSG_STREAM_TO_UI"));
				}
			}
			
		}
		
	};*/
	
	/*private void ServiceMessageParser(String msg) {
		String [] argv = msg.split("\\|");
		int argc = argv.length;
		
		if (argc == 1) {
			
		}
		else if (argc == 2) {
			if (argv[0].equals("SETPUBLIC")) {
				if (argv[1].equals("Done"))
					pgDialog.dismiss();
				else if (argv[1].equals("Failed")) {
					pgDialog.dismiss();
					Toast.makeText(getApplicationContext(), "Synchronization failed, please retry later...", Toast.LENGTH_SHORT).show();
				}
			}
		}
		else if (argc == 3) {
			
		}
	}*/
	
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
		//registerReceiver(mServiceBroadcastReceiver, new IntentFilter(SERVICE_MESSAGE_ACTION));
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
			final String [] options = {"File Metadata", "Share"};
			AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
			optBuilder.setTitle("Actions");
			optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if (item == 0) {
			    		dialog.dismiss();
			    		//String x = pwd.getAbsolutePath() + "/" + selectedFileName;
			    		showFileMetadata(pwdChild.get(pos-1).getAbsolutePath());
			    	}
			    	else {
			    		dialog.dismiss();
			    		showFileShare(pwdChild.get(pos-1).getAbsolutePath());
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
		    		// Talk to Governor via Service.
		    		/*Intent intent = new Intent(UI_MESSAGE_ACTION);
		    		intent.putExtra("UI_MSG_STREAM_TO_GOVERNOR", "SETPUBLIC|FILE|"+filepath+"|"+metadata);
		    		sendBroadcast(intent);*/

		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPUBLIC|FILE|"+filepath+"|"+metadata);
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
		    	}
		    	else if (item == 1) {
		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		showAccessList(filepath);
		    	}
		    	else {
		    		// Talk to Governor via Service.
		    		/*Intent intent = new Intent(UI_MESSAGE_ACTION);
		    		intent.putExtra("UI_MSG_STREAM_TO_GOVERNOR", "OFFLINE|FILE|"+filepath);
		    		sendBroadcast(intent);*/

		    		pgDialog = ProgressDialog.show(LocalHome.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "OFFLINE|FILE|"+filepath);
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
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

			@Override
			public void onClick(View arg0) {
				accAlert.dismiss();
				
				String accesslist = acclist.getText().toString().replace(";", "&");
				String [] rawMeta = getLocalFileMetadata(filepath);
	    		String metadata = "";
	    		for (String str : rawMeta) {
	    			metadata += str + "%";
	    		}
	    		metadata = metadata.substring(0, metadata.length()-1);
	    		
	    		// Talk to Governor via Service.
	    		/*Intent intent = new Intent(UI_MESSAGE_ACTION);
	    		intent.putExtra("UI_MSG_STREAM_TO_GOVERNOR", "SETPRIVATE|FILE|"+filepath+"|"+metadata+"|"+accesslist);
	    		sendBroadcast(intent);*/
	    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPRIVATE|FILE|"+filepath+"|"+metadata+"|"+accesslist);
	    		if (reply == null) return;
	    		GovernorMessageParser(reply);
			}});
		btCancel.setOnClickListener(new OnClickListener () {

			@Override
			public void onClick(View arg0) {
				accAlert.dismiss();
			}});
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
