package com.Uno.unoAndroid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

public class UnoService extends Service {

	int mValue = 0;
	public static double soundPressureValue = 0;
	public static double passiveLatitude = 0;
	public static double passiveLongitude = 0;
	public static double passiveAltitude = 0;
	public static double passiveAccuracy = 0;
	
	@Override
	public void onCreate() {
		mComSensorMgr = new CommonSensorManager();
		tcplist = new TCPListenThread(this);
		
		mSoundMgr = new SoundMeterManager();
		
		startFineLocationService();

		registerReceiver(mBatReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		tcplist.start();

		mComSensorMgr.startSensor();
		mSoundMgr.startMeasure();
		
		setupSensorsObjectFile();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mBatReceiver);
		tcplist.stop();

		mComSensorMgr.stopSensor();
		mSoundMgr.stopMesaure();
		stopCoarseLocationService();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * 
	 * The following are the 12 component of this service.
	 * 
	 * 1. UnoGovernor & Peer Commands @ TCP 11314
	 * 2. UnoGovernor & Peer Commands @ UDP 11315
	 * 3. UnoGovernor & Peer Commands @ FileSocket 11316
	 * 4. UnoGovernor & Peer Commands @ SensorSocket 11317 (@deprecated)
	 * 5. Location Service
	 * 6. Battery Service
	 * 7. Common Sensor Manager
	 * 8. Sound Meter
	 * 9. TCP Send Thread
	 * 10. UDP Send Thread
	 * 11. File Request Send Thread
	 * 12. UnoGovernor & Peer Message Parser
	 * 
	 * */

	private final Context mCtx = this;
	private static String GOVERNOR_IP = "com1379.eecs.utk.edu";
	private static String DEVICE_LOCAL_LISTEN_IP = null;
	private ServerSocket tcpServer = null;
	private TCPListenThread tcplist = null;
	private int BatteryLevel = -1;
	private CommonSensorManager mComSensorMgr = null;
	private SoundMeterManager mSoundMgr = null;
    private BroadcastReceiver mBatReceiver = new BroadcastReceiver() {
    	
    	@Override
    	public void onReceive(Context arg0, Intent arg1) {
    		BatteryLevel = arg1.getIntExtra("level", 0);		
    	}
    	
    };
    private String passiveProvider = LocationManager.PASSIVE_PROVIDER;
    private LocationManager mLocationMgr = null;
    
	
    /*
     * Network message parser deal with incoming request from server or peer smartphone.
     * 
     * */
    
	private void NetworkMessageParser(Context c, String msg, Socket client) {
		
		String [] argv = msg.split("\\|");
		int argc = argv.length;
		
		if (argc == 1) {
			
		}
		else if (argc == 2) {
			
		}
		else if (argc == 3) {
			if (argv[0].equals("PIN")) {
				if (argv[1].equals("FILE")) {
					
					try {
						File f = new File(argv[2]);
						byte [] buf = new byte[client.getSendBufferSize()];
						FileInputStream fis = new FileInputStream(f);
						BufferedInputStream bis = new BufferedInputStream(fis);
						OutputStream os = client.getOutputStream();
						while (bis.read(buf) > 0) {
							os.write(buf);
							os.flush();
							buf = new byte[client.getSendBufferSize()];
						}
					}
					catch (Exception e) {}
				}
			}
			else if (argv[0].equals("GET")) {
				if (argv[1].equals("SENSOR")) {
					String sensor = argv[2];
					String [] res = readSensorValues(sensor);
					try {
						String outgoingMsg = "POST|SENSOR|"+res[0]+"%"+res[1]+"%"+res[2];
						byte [] buf = outgoingMsg.getBytes();
						DataOutputStream out = new DataOutputStream(client.getOutputStream());
						out.write(buf);
						
						client.close();
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
		}
	}
	
	/*
	 * TCP listener on the 11314 port and message sender.
	 * */
	
	private class TCPListenThread extends Thread {
		
		private Context mCtx;
		
		public TCPListenThread(Context c) {
			this.mCtx = c;
		}
		
		@Override
		public void run() {
    		try {
    			if (DEVICE_LOCAL_LISTEN_IP == null)
    				DEVICE_LOCAL_LISTEN_IP = getLocalIPAddr();
    			if (DEVICE_LOCAL_LISTEN_IP != null) {
    				tcpServer = new ServerSocket(11314);
    				while (true) {
    					Socket client = tcpServer.accept();
    					try {
    						BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    						String line = "";
    						String incomingMsg = "";
    						incomingMsg = in.readLine();

    						NetworkMessageParser(mCtx, incomingMsg, client);
						
    						client.close();
    					}
    					catch (Exception e)	{
    						client.close();
    					}
    				}
    			}
    		}
    		catch (Exception e) {}
    	}
	}
	
	private void sendTcpPacket(String ip, int port, String msg) {
    	try	{
    		InetAddress remoteAddr = InetAddress.getByName(ip);
    		Socket socket = new Socket(remoteAddr, port);
    		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    		out.println(msg);
    		socket.close();
    		Log.d("TCP", "send done.");
    	}
    	catch (Exception e)
    	{
    		Log.e("TCP", e.toString());
    	}
    }
    
    
    private void sendUdpPacket(String ip, int port, String msg) {
    	try {
    		InetAddress serveraddr = InetAddress.getByName(ip);
    		DatagramSocket socket = new DatagramSocket();
    		byte [] buf = msg.getBytes();
    		DatagramPacket packet = new DatagramPacket(buf, buf.length, serveraddr, port);
    		socket.send(packet);
    		Log.e("UDP", "Send Done");
    	}
    	catch (Exception e) {
    		Log.e("UDP", e.toString());
    	}
    }
	
	/*
	 * This part is to do location service.
	 * */
	
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
	 * This part is for common sensor.
	 * */
    
    private class CommonSensorManager implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	private boolean isLogging;
    	
    	// Accelerometer Service
    	private Sensor Accelerometer;
    	public double accX;
    	public double accY;
    	public double accZ;
    	
    	// Gyroscope
    	private Sensor Gyroscopemeter;
    	public double spin;
    	public double output;
    	public double input;
    	
    	// Light
    	private Sensor Lightmeter;
    	public double light;
    	
    	// Magnetic Field
    	private Sensor Magnetometer;
    	public double magX;
    	public double magY;
    	public double magZ;
    	
    	// Orientation
    	private Sensor Orientationmeter;
    	public double rotationX;
    	public double rotationY;
    	public double rotationZ;
    	
    	// Proximity
    	private Sensor Proximitymeter;
    	public double proximity;
    	
    	// Gravity
    	private Sensor Gravitymeter;
    	public double gravityX;
    	public double gravityY;
    	public double gravityZ;
    	
    	// Sensor Filenames
    	private final String senAccelerometer = "Accelerometer.sensor";
    	private final String senLightmeter = "Lightmeter.sensor";
    	private final String senMagnetometer = "Magnetometer.sensor";
    	private final String senOrientationmeter = "Orientationmeter.sensor";
    	private final String senProximitymeter = "Proximitymeter.sensor";
    	private final String senGravitymeter = "Gravitymeter.sensor";
    	private final String senGyroscopemeter = "Gyroscopemeter.sensor";
    	
    	private FileOutputStream accfos;
    	private FileOutputStream lightfos;
    	private FileOutputStream magfos;
    	private FileOutputStream orifos;
    	private FileOutputStream proxfos;
    	private FileOutputStream gravfos;
    	private FileOutputStream gyrfos;
    	
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
    	
    	public boolean sensorAvaliable(int s) {
    		switch (s) {
	    		case Sensor.TYPE_ACCELEROMETER: return true;
	    		case Sensor.TYPE_GYROSCOPE: return true;
	    		case Sensor.TYPE_LIGHT: return true;
	    		case Sensor.TYPE_MAGNETIC_FIELD: return true;
	    		case Sensor.TYPE_ORIENTATION: return true;
	    		case Sensor.TYPE_PROXIMITY: return true;
	    		case Sensor.TYPE_GRAVITY: return true;
	    		default: return false;
    		}
    	}
    	
    	public void startLogging() {
    		this.isLogging = true;
    	}
    	
    	public void stopLogging() {
    		this.isLogging = false;
    	}
    	
    	public void resetLog() {
    		File accf = new File("/mnt/sdcard/sensor/"+this.senAccelerometer);
    		accf.deleteOnExit();
    		File lightf = new File("/mnt/sdcard/sensor/"+this.senLightmeter);
    		lightf.deleteOnExit();
    		File magf = new File("/mnt/sdcard/sensor/"+this.senMagnetometer);
    		magf.deleteOnExit();
    		File orif = new File("/mnt/sdcard/sensor/"+this.senOrientationmeter);
    		orif.deleteOnExit();
    		File proxf = new File("/mnt/sdcard/sensor/"+this.senProximitymeter);
    		proxf.deleteOnExit();
    		File gravf = new File("/mnt/sdcard/sensor/"+this.senGravitymeter);
    		gravf.deleteOnExit();
    		File gyrf = new File("/mnt/sdcard/sensor/"+this.senGyroscopemeter);
    		gyrf.deleteOnExit();
    	}
    	
    	// Start sensing
    	public void startSensor() {
    		mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Gyroscopemeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Lightmeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Magnetometer, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Orientationmeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Proximitymeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Gravitymeter, SensorManager.SENSOR_DELAY_UI);
    		
    		if (!isLogging) return;
    		try {
    			File senDir = new File("/mnt/sdcard/sensor/");
    			if (!senDir.exists()) senDir.mkdir();
    			
    			File accf = new File("/mnt/sdcard/sensor/"+this.senAccelerometer);
    			if (!accf.exists()) accf.createNewFile();
    			accfos = new FileOutputStream(accf, false);
    			
    			File lightf = new File("/mnt/sdcard/sensor/"+this.senLightmeter);
    			if (!lightf.exists()) lightf.createNewFile();
    			lightfos = new FileOutputStream(lightf, false);
    			
    			File magf = new File("/mnt/sdcard/sensor/"+this.senMagnetometer);
    			if (!magf.exists()) magf.createNewFile();
    			magfos = new FileOutputStream(magf, false);
    			
    			File orif = new File("/mnt/sdcard/sensor/"+this.senOrientationmeter);
    			if (!orif.exists()) orif.createNewFile();
    			orifos = new FileOutputStream(orif, false);
    			
    			File proxf = new File("/mnt/sdcard/sensor/"+this.senProximitymeter);
    			if (!proxf.exists()) proxf.createNewFile();
    			proxfos = new FileOutputStream(proxf, false);
    			
    			File gravf = new File("/mnt/sdcard/sensor/"+this.senGravitymeter);
    			if (!gravf.exists()) gravf.createNewFile();
    			gravfos = new FileOutputStream(gravf, false);
    			
    			File gyrf = new File("/mnt/sdcard/sensor/"+this.senGyroscopemeter);
    			if (!gyrf.exists()) gyrf.createNewFile();
    			gyrfos = new FileOutputStream(gyrf, false);
    			
    		} 
    		catch (Exception e) {
    			Log.e("Sensor", e.getMessage());
    		}
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
    		
    		if (!isLogging) return;
    		try {
    			accfos.close();
    			lightfos.close();
    			magfos.close();
    			orifos.close();
    			proxfos.close();
    			gravfos.close();
    			gyrfos.close();
    		} 
    		catch (Exception e) {
    			Log.e("Sensor", e.getMessage());
    		}
    		
    	}
    	
    	public void onSensorChanged(SensorEvent event) {
    		
    		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
    			this.accX = event.values[0]-this.gravityX;
    			this.accY = event.values[1]-this.gravityY;
    			this.accZ = event.values[2]-this.gravityZ;
    			
    			if (isLogging) {
    				try {
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.accX) + ";" + String.valueOf(this.accY) + ";" + String.valueOf(this.accZ);
	    				byte [] buf = str.getBytes();
	    				accfos.write(buf);
	    				accfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
    			this.spin = event.values[0];
    			this.output = event.values[1];
    			this.input = event.values[2];
    			
    			if (isLogging) {
	    			try {
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.spin) + ";" + String.valueOf(this.output) + ";" + String.valueOf(this.input);
	    				byte [] buf = str.getBytes();
	    				gyrfos.write(buf);
	    				gyrfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_LIGHT)
    		{
    			this.light = event.values[0];
    			
    			if (isLogging) {
    				try {
	    				String str = String.valueOf(this.light);
	    				byte [] buf = str.getBytes();
	    				lightfos.write(buf);
	    				lightfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
    			this.magX = event.values[0];
    			this.magY = event.values[1];
    			this.magZ = event.values[2];
    			
    			if (isLogging) {
    				try {
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.magX) + ";" + String.valueOf(this.magY) + ";" + String.valueOf(this.magZ);
	    				byte [] buf = str.getBytes();
	    				magfos.write(buf);
	    				magfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
    			this.rotationX = event.values[0];
    			this.rotationY = event.values[1];
    			this.rotationZ = event.values[2];
    			
    			if (isLogging) {	
    				try {
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.rotationX) + ";" + String.valueOf(this.rotationY) + ";" + String.valueOf(this.rotationZ);
	    				byte [] buf = str.getBytes();
	    				orifos.write(buf);
	    				orifos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		
    		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
    			this.proximity = event.values[0];
    			
    			if (isLogging) {
	    			try {
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.proximity);
	    				byte [] buf = str.getBytes();
	    				proxfos.write(buf);
	    				proxfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
    			this.gravityX = event.values[0];
    			this.gravityY = event.values[1];
    			this.gravityZ = event.values[2];
    			
    			if (isLogging) {
	    			try {
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.gravityX) + ";" + String.valueOf(this.gravityY) + ";" + String.valueOf(this.gravityZ);
	    				byte [] buf = str.getBytes();
	    				gravfos.write(buf);
	    				gravfos.flush();
	    			}
	    			catch(Exception e) {
	    				Log.e("Sensor", e.getMessage());
	    			}
    			}
    			return;
    		}
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
    
    private String [] readSensorValues (String type) {
		String [] val = new String[4];
		if (type.equals("TYPE_ACCELEROMETER")) {
			val[0] = String.valueOf(mComSensorMgr.accX);
			val[1] = String.valueOf(mComSensorMgr.accY);
			val[2] = String.valueOf(mComSensorMgr.accZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_GRAVITY")) {
			val[0] = String.valueOf(mComSensorMgr.gravityX);
			val[1] = String.valueOf(mComSensorMgr.gravityY);
			val[2] = String.valueOf(mComSensorMgr.gravityZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_GYROSCOPE")) {
			val[0] = String.valueOf(mComSensorMgr.spin);
			val[1] = String.valueOf(mComSensorMgr.output);
			val[2] = String.valueOf(mComSensorMgr.input);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_LIGHT")) {
			val[0] = String.valueOf(mComSensorMgr.light);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("TYPE_MAGNETIC_FIELD")) {
			val[0] = String.valueOf(mComSensorMgr.magX);
			val[1] = String.valueOf(mComSensorMgr.magY);
			val[2] = String.valueOf(mComSensorMgr.magZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_ORIENTATION")) {
			val[0] = String.valueOf(mComSensorMgr.rotationX);
			val[1] = String.valueOf(mComSensorMgr.rotationY);
			val[2] = String.valueOf(mComSensorMgr.rotationZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_PROXIMITY")) {
			val[0] = String.valueOf(mComSensorMgr.proximity);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("TYPE_SOUNDMETER")) {
			val[0] = String.valueOf(UnoService.soundPressureValue);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("LOCATION")) {
			val[0] = String.valueOf(UnoService.passiveLatitude);
			val[1] = String.valueOf(UnoService.passiveLongitude);
			val[2] = String.valueOf(UnoService.passiveAltitude);
			val[3] = String.valueOf(UnoService.passiveAccuracy);
		}
		
		return val;
	}
    
    /*
     * This part is sound meter.
     * */
    
    private class SoundMeterManager extends Thread {
    	private boolean isRunning = false;
    	private boolean isLogging = false;
    	public double SoundPressure;
    	private AudioRecord mrec = null;
    	private String senSound = "/mnt/sdcard/sensor/sound.sensor";
    	private File f = null;
    	private FileOutputStream fos = null;
    	private int BufferSize = 0;
    	
    	public SoundMeterManager() {
    		BufferSize = 2*AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
    		mrec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize);
    		isRunning = false;
    		isLogging = false;
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
    	
    	public void startLogging() {
    		isLogging = true;
    		f = new File(senSound);
    		try {
    			if (!f.exists()) f.createNewFile();
    		} catch (Exception e){}
    		
    		try {
    			fos = new FileOutputStream(f, true);
    		} catch (Exception e) {}
    	}
    	
    	public void stopLogging() {
    		isLogging = false;
    		try {
    			fos.close();
    		} catch (Exception e) {}
    	}
    	
    	private void writeLog(double value) {
    		if (!isLogging) return;
    		Date cur = new Date();
    		String rec = cur.toGMTString()+","+String.valueOf(value)+"\n";
    		try {
    			fos.write(rec.getBytes());
    			fos.flush();
    		} catch (Exception e) {}
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
    			writeLog(SoundPressure);
    		}
    		if (mrec != null) {
    			mrec.stop();
    			mrec.release();
    			mrec = null;
    		}
    	}
    }
    
    /*
     * This part is about local hardware.
     * */
    
	private String getLocalIPAddr() {
    	try {
    		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
    			NetworkInterface intf = en.nextElement();
    			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
    				InetAddress inetAddr = enumIpAddr.nextElement();
    				if (!inetAddr.isLoopbackAddress())
    					return inetAddr.getHostAddress().toString();
    			}
    		}
    		return null;
    	}
    	catch (SocketException e) {
    		Log.e("Server", e.toString());
    	}
    	return null;
    }
	
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
	
	/*
	 * Setup sensor object files in the system.
	 * */
	
	private void setupSensorsObjectFile() {
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