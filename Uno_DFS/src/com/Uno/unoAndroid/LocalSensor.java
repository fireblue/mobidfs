package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocalSensor extends ListActivity {
	
	private static String GOVERNOR_IP = UnoConstant.GOVERNOR_ADDRESS;
	private List <SensorValues> valuelist;
	private SensorAdapter adapter;
	private ProgressDialog pgDialog;
	private CommonSensorManager mComSensorMgr;
	private HashMap <String, String> SENSOR_NAME_MAP;
	private SoundMeterManager mSoundMgr;
	private Double soundPressureValue = 0.0;
	
	private LocalResourceDatabaseHelper resdbh = null;
	private String Owner = null;
	private String Device = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.localsensor);
        
        SENSOR_NAME_MAP = new HashMap <String, String>();
        resdbh = new LocalResourceDatabaseHelper(this);
        initLoginInfo();
        
        valuelist = initSensorList();
        adapter = new SensorAdapter(LocalSensor.this, valuelist);
        setListAdapter(adapter);
        mComSensorMgr = new CommonSensorManager();
        mSoundMgr = new SoundMeterManager();
        mComSensorMgr.startSensor();
        mSoundMgr.startMeasure();
        //startCoarseLocationService();
        startFineLocationService();
    }
	
	@Override
	public void onPause() {
		super.onPause();
		mComSensorMgr.stopSensor();
		mSoundMgr.stopMesaure();
		//stopCoarseLocationService();
		stopFineLocationService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mComSensorMgr.startSensor();
		mSoundMgr.startMeasure();
		//startCoarseLocationService();
		startFineLocationService();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mComSensorMgr.stopSensor();
		mSoundMgr.stopMesaure();
		//stopCoarseLocationService();
		stopFineLocationService();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		long st = this.startRespondingTimeTrack();
		
		final String [] options = {"Share to Everyone", "Share to Friends", "Offline"};
		AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
		optBuilder.setTitle("Sensor Options");
		final int pos = position;
		optBuilder.setItems(options, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	if (item == 0) {
		    		long st = startRespondingTimeTrack();
		    		
		    		pgDialog = ProgressDialog.show(LocalSensor.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPUBLIC|SENSOR|"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName));
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
		    		
		    		/*
		    		 * Update Local Database here.
		    		 * */
		    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+
		    				"/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName)+"'");
		    		int n = resdbh.countRow(c);
		    		
		    		if (n == 0) {
			    		String [] row = new String[6];
			    		row[0] = Owner;
			    		row[1] = Device;
			    		row[2] = SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[3] = "/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[4] = "1";
			    		row[5] = "";
			    		resdbh.insertRow(row);
		    		}
		    		else {
		    			c.moveToFirst();
		    			String [] row = resdbh.fetchOneRow(c);
		    			row[1] = Owner;
			    		row[2] = Device;
			    		row[3] = SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[4] = "/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[5] = "1";
			    		row[6] = "";
		    			resdbh.updateRow(row);
		    		}
		    		stopRespondingTimeTrack("LOCAL_SENSOR_SHARE_TO_EVERYONE", st);
		    	}
		    	else if (item == 1) {
		    		pgDialog = ProgressDialog.show(LocalSensor.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		showAccessList(SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName));
		    	}
		    	else {
		    		pgDialog = ProgressDialog.show(LocalSensor.this, "", "Synchronizing to Server. Please wait...", true);
		    		pgDialog.show();
		    		
		    		long st = startRespondingTimeTrack();
		    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "OFFLINE|SENSOR|"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName));
		    		if (reply == null) return;
		    		GovernorMessageParser(reply);
		    		
		    		/*
		    		 * Update Local Database here.
		    		 * */
		    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+
		    				"/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName)+"'");
		    		int n = resdbh.countRow(c);
		    		
		    		if (n == 0) {
			    		String [] row = new String[6];
			    		row[0] = Owner;
			    		row[1] = Device;
			    		row[2] = SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[3] = "/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[4] = "0";
			    		row[5] = "";
			    		resdbh.insertRow(row);
		    		}
		    		else {
		    			c.moveToFirst();
		    			String [] row = resdbh.fetchOneRow(c);
		    			row[1] = Owner;
			    		row[2] = Device;
			    		row[3] = SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[4] = "/mnt/sdcard/Sensors/"+SENSOR_NAME_MAP.get(valuelist.get(pos).SensorName);
			    		row[5] = "0";
			    		row[6] = "";
		    			resdbh.updateRow(row);
		    		}
		    		
		    		stopRespondingTimeTrack("LOCAL_SENSOR_OFFLINE", st);
		    	}
		    }
		});
		optBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.dismiss();
	           }
	    });
		optBuilder.create().show();
		
		this.stopRespondingTimeTrack("LOCAL_SENSOR_CLICK_GENERAL", st);
	}
	
	private void initLoginInfo() {
		long st = this.startRespondingTimeTrack();
		
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
		
		this.stopRespondingTimeTrack("LOCAL_SENSOR_initLoginInfo()", st);
	}
	
	private void showAccessList(String sensor) {
		AlertDialog.Builder accBuilder;
		final AlertDialog accAlert;
		final String xsensor = sensor;

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

			public void onClick(View arg0) {
				accAlert.dismiss();
				
				long st = startRespondingTimeTrack();
				
				String accesslist = acclist.getText().toString().replace(";", "&");
				
	    		String reply = sendTcpPacket(GOVERNOR_IP, 11314, "SETPRIVATE|SENSOR|"+xsensor+"|"+accesslist);
	    		if (reply == null) return;
	    		GovernorMessageParser(reply);
	    		
	    		/*
	    		 * Update Local Database here.
	    		 * */
	    		Cursor c = resdbh.execQuery("SELECT * FROM "+resdbh.dbName+" WHERE "+resdbh.colResourcePath+"='"+
	    				"/mnt/sdcard/Sensors/"+xsensor+"'");
	    		int n = resdbh.countRow(c);
	    		
	    		if (n == 0) {
		    		String [] row = new String[6];
		    		row[0] = Owner;
		    		row[1] = Device;
		    		row[2] = xsensor;
		    		row[3] = "/mnt/sdcard/Sensors/"+xsensor;
		    		row[4] = "0";
		    		row[5] = "";
		    		resdbh.insertRow(row);
	    		}
	    		else {
	    			c.moveToFirst();
	    			String [] row = resdbh.fetchOneRow(c);
	    			row[1] = Owner;
		    		row[2] = Device;
		    		row[3] = xsensor;
		    		row[4] = "/mnt/sdcard/Sensors/"+xsensor;
		    		row[5] = "0";
		    		row[6] = "";
	    			resdbh.updateRow(row);
	    		}
	    		
	    		stopRespondingTimeTrack("LOCAL_SENSOR_SHARE_TO_FRIENDS", st);
			}});
		btCancel.setOnClickListener(new OnClickListener () {

			public void onClick(View arg0) {
				pgDialog.dismiss();
				accAlert.dismiss();
				
			}});
	}
	
	private void GovernorMessageParser(String msg) {
		String [] argv = msg.split("\\|");
		int argc = argv.length;
		
		if (argc == 1) {
			
		}
		else if (argc == 2) {
			
		}
		else if (argc == 3) {
			if (argv[0].equals("SETPUBLIC")) {
				if (argv[1].equals("SENSOR")) {
					if (argv[2].equals("DONE")) {
						Toast.makeText(getApplicationContext(), "Sensor has been set to public.", Toast.LENGTH_LONG).show();
						pgDialog.dismiss();
					}
				}
			}
			else if (argv[0].equals("SETPRIVATE")) {
				if (argv[1].equals("SENSOR")) {
					if (argv[2].equals("DONE")) {
						Toast.makeText(getApplicationContext(), "Sensor has been set to public.", Toast.LENGTH_LONG).show();
						pgDialog.dismiss();
					}
				}
			}
			else if (argv[0].equals("OFFLINE")) {
				if (argv[1].equals("SENSOR")) {
					if (argv[2].equals("DONE")) {
						Toast.makeText(getApplicationContext(), "Sensor has been set to public.", Toast.LENGTH_LONG).show();
						pgDialog.dismiss();
					}
				}
			}
		}
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
	
	private ArrayList <SensorValues> initSensorList () {
		ArrayList list = new ArrayList <SensorValues> ();
		SensorValues entry = new SensorValues();
		
		entry.SensorName = "Accelerometer";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_ACCELEROMETER");
		
		entry = new SensorValues();
		entry.SensorName = "Gyroscopemeter";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_GYROSCOPE");
		
		entry = new SensorValues();
		entry.SensorName = "Lightmeter";
		entry.valueX = "0";
		entry.valueY = entry.valueZ = "N/A";
		entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_LIGHT");
		
		entry = new SensorValues();
		entry.SensorName = "Magnetometer";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_MAGNETIC_FIELD");
		
		entry = new SensorValues();
		entry.SensorName = "Orientationmeter";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_ORIENTATION");
		
		entry = new SensorValues();
		entry.SensorName = "Proximitymeter";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_PROXIMITY");
		
		entry = new SensorValues();
		entry.SensorName = "Gravitymeter";
		entry.valueX = entry.valueY = entry.valueZ = entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_GRAVITY");
		
		entry = new SensorValues();
		entry.SensorName = "Soundmeter";
		entry.valueX = "0";
		entry.valueY = entry.valueZ = "N/A";
		entry.Accuracy = "0";
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "TYPE_SOUNDMETER");
		
		entry = new SensorValues();
		entry.SensorName = "Location";
		entry.valueX = String.valueOf(UnoService.passiveLatitude);
		entry.valueY = String.valueOf(UnoService.passiveLongitude);
		entry.valueZ = String.valueOf(UnoService.passiveAltitude);
		entry.Accuracy = String.valueOf(UnoService.passiveAccuracy);
		list.add(entry);
		SENSOR_NAME_MAP.put(entry.SensorName, "LOCATION");
		
		return list;
	}
	
	private class SensorValues {
		public String SensorName = "";
		public String valueX = "";
		public String valueY = "";
		public String valueZ = "";
		public String Accuracy = "";
	}
	
	private class SensorAdapter extends BaseAdapter {

		private Context context;
		private List <SensorValues> listSensorValues;
		
		public SensorAdapter (Context c, List <SensorValues> sv) {
			this.context = c;
			this.listSensorValues = sv;
		}
		
		public int getCount() {
			return listSensorValues.size();
		}

		public Object getItem(int arg0) {
			return listSensorValues.get(arg0);
		}

		public long getItemId(int arg0) {
			return arg0;
		}

		public View getView(int pos, View v, ViewGroup vg) {
			
			SensorValues entry = listSensorValues.get(pos);
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.sensorrow, null);
			}
			TextView sensorName = (TextView) v.findViewById(R.id.sensorName);
			sensorName.setText(entry.SensorName);
			TextView valueX = (TextView) v.findViewById(R.id.valueX);
			valueX.setText(entry.valueX);
			TextView valueY = (TextView) v.findViewById(R.id.valueY);
			valueY.setText(entry.valueY);
			TextView valueZ = (TextView) v.findViewById(R.id.valueZ);
			valueZ.setText(entry.valueZ);
			
			return v;
		}
		
	}
	
	
	private class CommonSensorManager implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	private Sensor Accelerometer;
    	private Sensor Gyroscopemeter;
    	private Sensor Lightmeter;
    	private Sensor Magnetometer;
    	private Sensor Orientationmeter;
    	private Sensor Proximitymeter;
    	private Sensor Gravitymeter;
    	private int delay = 0;
    	
    	public CommonSensorManager()
    	{
    		this.mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    		this.Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    		this.Gyroscopemeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    		this.Lightmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    		this.Magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    		this.Orientationmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		this.Proximitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    		this.Gravitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    	}
    	
    	// Start sensing
    	public void startSensor() {
    		mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Gyroscopemeter, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Lightmeter, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Magnetometer, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Orientationmeter, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Proximitymeter, SensorManager.SENSOR_DELAY_GAME);
    		mSensorManager.registerListener(this, this.Gravitymeter, SensorManager.SENSOR_DELAY_GAME);
    	}
    	
    	// Stop sensing
    	public void stopSensor() {
    		mSensorManager.unregisterListener(this, this.Accelerometer);
    		mSensorManager.unregisterListener(this, this.Gyroscopemeter);
    		mSensorManager.unregisterListener(this, this.Lightmeter);
    		mSensorManager.unregisterListener(this, this.Magnetometer);
    		mSensorManager.unregisterListener(this, this.Orientationmeter);
    		mSensorManager.unregisterListener(this, this.Proximitymeter);
    		mSensorManager.unregisterListener(this, this.Gravitymeter);
    	}
    	
    	public void onSensorChanged(SensorEvent event) {
    		
    		delay++;
    		if (delay != 20) return;
    		delay = 0;
    		
    		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Accelerometer";
    			entry.valueX = String.valueOf(-event.values[0]+Double.valueOf(valuelist.get(6).valueX));
    			entry.valueY = String.valueOf(-event.values[1]+Double.valueOf(valuelist.get(6).valueY));
    			entry.valueZ = String.valueOf(-event.values[2]+Double.valueOf(valuelist.get(6).valueZ));
    			entry.Accuracy = "0";
    			valuelist.set(0, entry);
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Gyroscopemeter";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = String.valueOf(event.values[1]);
    			entry.valueZ = String.valueOf(event.values[2]);
    			entry.Accuracy = "0";
    			valuelist.set(1, entry);
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Lightmeter";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = "N/A";
    			entry.valueZ = "N/A";
    			entry.Accuracy = "0";
    			valuelist.set(2, entry);
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Magnetometer";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = String.valueOf(event.values[1]);
    			entry.valueZ = String.valueOf(event.values[2]);
    			entry.Accuracy = "0";
    			valuelist.set(3, entry);
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Orientationmeter";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = String.valueOf(event.values[1]);
    			entry.valueZ = String.valueOf(event.values[2]);
    			entry.Accuracy = "0";
    			valuelist.set(4, entry);
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Proximitymeter";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = "N/A";
    			entry.valueZ = "N/A";
    			entry.Accuracy = "0";
    			valuelist.set(5, entry);
    		}
    		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
    			SensorValues entry = new SensorValues();
    			entry.SensorName = "Gravitymeter";
    			entry.valueX = String.valueOf(event.values[0]);
    			entry.valueY = String.valueOf(event.values[1]);
    			entry.valueZ = String.valueOf(event.values[2]);
    			entry.Accuracy = "0";
    			valuelist.set(6, entry);
    		}
    		
    		SensorValues entry = new SensorValues();
	    	entry = new SensorValues();
	   		entry.SensorName = "Soundmeter";
	   		entry.valueX = String.valueOf(soundPressureValue);
	   		entry.valueY = entry.valueZ = "N/A";
	   		entry.Accuracy = "0";
    		valuelist.set(7, entry);  
    		
    		entry = new SensorValues();
    		entry.SensorName = "Location";
    		entry.valueX = String.valueOf(UnoService.passiveLatitude);
    		entry.valueY = String.valueOf(UnoService.passiveLongitude);
    		entry.valueZ = String.valueOf(UnoService.passiveAccuracy);
    		entry.Accuracy = String.valueOf(UnoService.passiveAccuracy);
    		valuelist.set(8, entry);
    		adapter.notifyDataSetChanged();
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
	
private class SoundMeterManager extends Thread {
    	
    	private boolean isRunning = false;
    	private HashMap <String, FileOutputStream> logSoundmeterHashMap = new HashMap <String, FileOutputStream>();
    	private double SoundPressure;
    	private AudioRecord mrec = null;
    	private String soundmeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_SOUNDMETER";
    	private int BufferSize = 0;
    	
    	public SoundMeterManager() {
    		BufferSize = 2*AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
    		mrec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize);
    		isRunning = false;
    		SoundPressure = 0.0;
    	}
    	
    	public double getSoundPressure() {
    		return this.SoundPressure;
    	}
    	
    	public void startMeasure() {
    		isRunning = true;
    		try {
    			this.start();
    		} catch (Exception e) {}
    	}
    	
    	public void stopMesaure() {
    		isRunning = false;
    	}
    	
    	public void startLoggingSoundmeter(String requestor) throws IOException {
    		
    		File dir = new File(this.soundmeterBasePath);
    		if (!dir.exists()) dir.mkdirs();
    		
    		File f = new File(this.soundmeterBasePath+"/"+requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		
    		this.logSoundmeterHashMap.put(requestor, new FileOutputStream(f));
    	}
    	
    	public void stopLogging(String requestor) throws IOException {
    		this.logSoundmeterHashMap.get(requestor).close();
    		this.logSoundmeterHashMap.remove(requestor);
    	}
    	
    	public void resetAllLogs() throws IOException {
    		for (String key: this.logSoundmeterHashMap.keySet()) {
    			this.logSoundmeterHashMap.get(key).close();
    			this.logSoundmeterHashMap.remove(key);
    		}
    	}
    	
    	private void writeLog(double value) throws IOException {

    		Date cur = new Date();
    		String rec = cur.toGMTString()+","+String.valueOf(value)+"\n";
    		byte [] buf = rec.getBytes();
    		
    		for (String key: this.logSoundmeterHashMap.keySet()) {
    			this.logSoundmeterHashMap.get(key).write(buf);
    			this.logSoundmeterHashMap.get(key).flush();
    		}
    	}
    	
    	@Override
    	public void run() {
    		mrec.startRecording();
    		while (isRunning) {
    			short [] tmpBuf = new short[BufferSize];
    			mrec.read(tmpBuf, 0, BufferSize);
    			double sum = 0.0;
    			for (int i = 0; i < BufferSize; i++)
    				sum += Math.abs(tmpBuf[i]);
    			SoundPressure = 20.0*Math.log10(sum/BufferSize);
    			soundPressureValue = SoundPressure;
    			try {
					writeLog(SoundPressure);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		if (mrec != null) {
    			mrec.stop();
    			mrec.release();
    			mrec = null;
    		}
    	}
    }
	
	/*
	 * This part is to do location service.
	 * */
	
	private LocationManager mLocationMgr = null;

	private LocationListener coarseListener = new LocationListener() {
	
		public void onLocationChanged(Location location) {
			UnoService.passiveLatitude = location.getLatitude();
			UnoService.passiveLongitude = location.getLongitude();
			UnoService.passiveAltitude = location.getAltitude();
			UnoService.passiveAccuracy = location.getAccuracy();
			
		}
	
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	private LocationListener fineListener = new LocationListener() {
	
		public void onLocationChanged(Location location) {
			UnoService.passiveLatitude = location.getLatitude();
			UnoService.passiveLongitude = location.getLongitude();
			UnoService.passiveAltitude = location.getAltitude();
			UnoService.passiveAccuracy = location.getAccuracy();
			
		}
	
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	private Criteria createCoarseCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		//c.setAccuracy(100);
		c.setBearingAccuracy(Criteria.ACCURACY_COARSE);
		//c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_LOW);
		return c;
	}
	
	private Criteria createFineCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		//c.setBearingRequired(false);
		c.setBearingAccuracy(Criteria.ACCURACY_FINE);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;
	}
	
	private void startCoarseLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, coarseListener);
	}
	
	private void stopCoarseLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.removeUpdates(coarseListener);
	}
	
	private void startFineLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createFineCriteria(), true));
		mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, fineListener);
	}
	
	private void stopFineLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.removeUpdates(fineListener);
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