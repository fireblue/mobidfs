package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginPage extends Activity {
	
	private EditText usernameEditText = null;
	private EditText passwordEditText = null;
	private Button loginButton = null;
	private Button registerButton = null;
	private static String GOVERNOR_IP = "com1379.eecs.utk.edu";
	private final Context mCtx = this;
	
	private ProgressDialog pgDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        startService(new Intent(getApplicationContext(), UnoService.class));
        
        usernameEditText = (EditText) findViewById(R.id.login_username);
        passwordEditText = (EditText) findViewById(R.id.login_pwd);
        loginButton = (Button) findViewById(R.id.login_ok);
        registerButton = (Button) findViewById(R.id.login_reg);
        
        loginButton.setOnClickListener(mLoginKeyListener);
        registerButton.setOnClickListener(mRegisterKeyListener);
        registerReceiver(mBatReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        setupSensorsOjectFile();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	registerReceiver(mBatReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mBatReceiver);
    }
    
    @Override
    public void onDestroy () {
    	super.onDestroy();
    	unregisterReceiver(mBatReceiver);
    }
    
    private OnClickListener mLoginKeyListener = new OnClickListener() {

		public void onClick(View arg0) {
			pgDialog = ProgressDialog.show(LoginPage.this, "", "Login, please wait...", true);
			pgDialog.show();
			LoginProcess();
		}
    	
    };
    
    private OnClickListener mRegisterKeyListener = new OnClickListener() {

		public void onClick(View arg0) {
			startActivity(new Intent(LoginPage.this, RegisterPage.class));
			
		}
    	
    };
    
    private void LoginProcess()
    {
		String usr = usernameEditText.getText().toString();
		String pwd = passwordEditText.getText().toString();
		
		if (usr.equals("") || pwd.equals("")) {
			Toast.makeText(getApplicationContext(), "Missing Important Information...", Toast.LENGTH_LONG).show();
			Vibrator vbr = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			vbr.vibrate(300);
			pgDialog.dismiss();
			return;
		}
		
		String loginMsg = "LOGIN|"+usr+"|"+pwd+"|"+getDeviceMetadata();
		String reply = sendTcpPacket(GOVERNOR_IP, 11314, loginMsg);
		if (reply == null) return;
		GovernorMessageParser(reply);
		
		/*
		 * Record username.
		 * */
		try {
			File dir = new File("/mnt/sdcard/Uno");
			if (!dir.exists()) dir.mkdirs();
			
			File f = new File("/mnt/sdcard/Uno/login.ini");
			f.deleteOnExit();
			f.createNewFile();
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(("Owner:"+usr+"\n").getBytes());
			fos.flush();
			fos.write(("Device:"+android.os.Build.DEVICE).getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {}
		
		/*
		 * Fetch P2P list after login.
		 * */
		fetchP2pList();
    }
    
    private void fetchP2pList() {
    	String reply = sendTcpPacket(GOVERNOR_IP, 11314, "GET|P2P");
    	if (reply == null) {
    		Toast.makeText(getApplicationContext(), "Fetch P2P List Failed...", Toast.LENGTH_LONG).show();
    		return;
    	}
    	String [] p2pList = reply.split("\\|")[2].split(";");
    	File f = new File("/mnt/sdcard/Uno/p2p.ini");
    	f.deleteOnExit();
    	try {
			f.createNewFile();
		} catch (IOException e) {}
		try {
			FileOutputStream fos = new FileOutputStream(f);
			for (String str: p2pList) {
				fos.write((str+"\n").getBytes());
				fos.flush();
			}
			fos.close();
		} catch (Exception e) {}
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
    
    private void GovernorMessageParser(String msg) {
    	String [] argv = msg.split("\\|");
    	int argc = argv.length;
    	if (pgDialog.isShowing()) pgDialog.dismiss();
    	if (argc == 1) {
			
		}
		else if (argc == 2) {
			if (argv[0].equals("LOGIN")) {
				
				if (argv[1].equals("NO_USER")) {
					Toast.makeText(getApplicationContext(), "Welcome!!! New user, you NEED to register now.",
							Toast.LENGTH_LONG).show();
				}
				else if (argv[1].equals("FAILED")) {
					Toast.makeText(getApplicationContext(), "Sorry, your Username and Password do not match.", 
							Toast.LENGTH_LONG).show();
				}
				else if (argv[1].equals("DONE")) {
					mCtx.startActivity(new Intent(LoginPage.this, MainPage.class));
				}
			}
		}
		else if (argc == 3) {
			
		}
    }
    private int BatteryLevel = -1;
    private BroadcastReceiver mBatReceiver = new BroadcastReceiver() {
    	
    	@Override
    	public void onReceive(Context arg0, Intent arg1) {
    		BatteryLevel = arg1.getIntExtra("level", 0);		
    	}
    	
    };
    private String getDeviceMetadata() {
		String metadata = "";
		
		// Device name
		metadata +=	android.os.Build.DEVICE+";";
		
		// Wifi Information Quality & MAC
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	if (wifi == null)
    		metadata += "N/A;";
    	else
    	{
    		WifiInfo wifiInfo = wifi.getConnectionInfo();
        	metadata += String.valueOf(wifiInfo.getRssi())+"&"+wifiInfo.getMacAddress()+";";
    	}
    	
    	// 3G Information (N/A)
    	metadata += "N/A;";
    	
    	// Bluetooth Information
    	metadata += "N/A;";
    	
    	// Battery
    	metadata += String.valueOf(this.BatteryLevel);
    	
    	return metadata;
	}
    
    private void setupSensorsOjectFile() {
    	File f = new File("/mnt/sdcard/Sensors");
    	if (!f.exists()) f.mkdirs();
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_ACCELEROMETER");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_GRAVITY");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_GYROSCOPE");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_LIGHT");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_LINEAR_ACCELERATION");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_MAGNETIC_FIELD");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_ORIENTATION");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_PRESSURE");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_PROXIMITY");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_ROTATION_VECTOR");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/TYPE_TEMPERATURE");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    	
    	f = new File("/mnt/sdcard/Sensors/LOCATION");
    	try {
    		if (!f.exists()) f.createNewFile();
    	} catch (Exception e) {}
    }
}