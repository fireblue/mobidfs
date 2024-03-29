package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterPage extends Activity {

	private EditText usernameEditText = null;
	private EditText passwordEditText = null;
	private EditText confirmEditText = null;
	private Button registerButton = null;
	private static String GOVERNOR_IP = UnoConstant.GOVERNOR_ADDRESS;
	private ProgressDialog pgDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        
        usernameEditText = (EditText) findViewById(R.id.register_username);
        passwordEditText = (EditText) findViewById(R.id.register_password);
        confirmEditText = (EditText) findViewById(R.id.register_confirm);
        registerButton = (Button) findViewById(R.id.register_ok);
        
        registerButton.setOnClickListener(mRegisterKeyListener);
        registerReceiver(mBatReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    
    private OnClickListener mRegisterKeyListener = new OnClickListener() {

		public void onClick(View arg0) {
			
			long st = startRespondingTimeTrack();
			RegisterProcess();
			stopRespondingTimeTrack("REGISTER", st);
		}

    };
    
    private void RegisterProcess() {
    	
    	String usr = usernameEditText.getText().toString();
		String pwd = passwordEditText.getText().toString();
		String cfm = confirmEditText.getText().toString();
    	
		if (usr.equals("") || pwd.equals("") || cfm.equals("")) {
			Toast.makeText(getApplicationContext(), "Missing Important Information...", Toast.LENGTH_LONG).show();
			Vibrator vbr = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			vbr.vibrate(300);
			//pgDialog.dismiss();
			return;
		}
		
    	if (!cfm.equals(pwd)) {
			Toast.makeText(getApplicationContext(), "Password not the same, please check it.", Toast.LENGTH_LONG).show();
			
			Vibrator vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vbr.vibrate(100);
			
			passwordEditText.setText("");
			confirmEditText.setText("");
		}
    	else {
    		pgDialog = ProgressDialog.show(RegisterPage.this, "", "Register, loading...", true);
			pgDialog.show();
    		String registerMsg = "REGISTER|"+usr+"|"+pwd+"|"+getDeviceMetadata();
    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, registerMsg);
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
    			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
    			bw.write("Owner:"+usr);
    			bw.newLine();
    			bw.write("Device:"+android.os.Build.DEVICE);
    			bw.newLine();
    			bw.flush();
    			bw.close();
    		} catch (Exception e) {}
    	}
    	
    	/*
    	 * Fetch P2P list.
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
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			for (String str: p2pList) {
				bw.write(str);
				bw.newLine();
				bw.flush();
			}
			bw.close();
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
    
    private void GovernorMessageParser(String msg) {
    	if (pgDialog.isShowing()) pgDialog.dismiss();
    	String [] argv = msg.split("\\|");
    	int argc = argv.length;
    	
    	if (argc == 1) {
			
		}
		else if (argc == 2) {
			if (argv[0].equals("REGISTER")) {
				if (argv[1].equals("USER_EXIST")) {
					Toast.makeText(getApplicationContext(), "Username exist!!", Toast.LENGTH_LONG).show();
					usernameEditText.setText("");
					passwordEditText.setText("");
					confirmEditText.setText("");
				}
				else if (argv[1].equals("DONE")) {
					startActivity(new Intent(RegisterPage.this, MainPage.class));
				}
			}
		}
		else if (argc == 3) {
			
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
