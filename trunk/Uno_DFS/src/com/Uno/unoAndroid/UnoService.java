package com.Uno.unoAndroid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
		
		startCoarseLocationService();

		registerReceiver(mBatReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		tcplist.start();

		//mComSensorMgr.startSensor();
		//mSoundMgr.startMeasure();
		
		setupSensorsObjectFile();
		
		resdbh = new LocalResourceDatabaseHelper(this);
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mBatReceiver);
		tcplist.stop();

		//mComSensorMgr.stopSensor();
		//mSoundMgr.stopMesaure();
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
	private static String GOVERNOR_IP = UnoConstant.GOVERNOR_ADDRESS;
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
    
    private LocalResourceDatabaseHelper resdbh;
	
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
					
					long st = startRespondingTimeTrack();
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
					stopRespondingTimeTrack("SERVICE_PIN_FILE", st);
				}
			}
			else if (argv[0].equals("PREVIEW")) {
				if (argv[1].equals("FILE")) {
					
					long st = startRespondingTimeTrack();
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
					stopRespondingTimeTrack("SERVICE_PREVIEW_FILE", st);
				}
			}
			else if (argv[0].equals("GET")) {
				if (argv[1].equals("SENSOR")) {
					
					long st = this.startRespondingTimeTrack();
					
					/*
					 * Optimization for sense-on-request.
					 * */
					mComSensorMgr.startSensor();
					mSoundMgr.startMeasure();
					try {
						tcplist.sleep(10);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					// ----------------------------------
					
					String sensor = argv[2];
					String [] res = readSensorValues(sensor);
					
					mComSensorMgr.stopSensor();
					mSoundMgr.stopMesaure();
					
					if (res == null) {
						try {
							String outgoingMsg = "POST|SENSOR|P2P|NO_RESOURCE";
							byte [] buf = outgoingMsg.getBytes();
							DataOutputStream out = new DataOutputStream(client.getOutputStream());
							out.write(buf);
							
							client.close();
							out.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
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
					this.stopRespondingTimeTrack("SERVICE_GET_SENSOR", st);
				}
			}
		}
		else if (argc == 4) {
			if (argv[0].equals("GET")) {
				if (argv[1].equals("SENSOR")) {
					if (argv[2].equals("P2P")) {
						
						long st = this.startRespondingTimeTrack();
						
						/*
						 * Optimization for sense-on-request.
						 * */
						mComSensorMgr.startSensor();
						mSoundMgr.startMeasure();
						try {
							tcplist.sleep(10);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// ----------------------------------
						
						String sensor = argv[3];
						String [] res = readSensorValues(sensor);
						
						mComSensorMgr.stopSensor();
						mSoundMgr.stopMesaure();
						
						if (res == null) {
							try {
								String outgoingMsg = "POST|SENSOR|P2P|NO_RESOURCE";
								byte [] buf = outgoingMsg.getBytes();
								DataOutputStream out = new DataOutputStream(client.getOutputStream());
								out.write(buf);
								
								client.close();
								out.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						try {
							String outgoingMsg = "POST|SENSOR|P2P|"+res[0]+"%"+res[1]+"%"+res[2];
							byte [] buf = outgoingMsg.getBytes();
							DataOutputStream out = new DataOutputStream(client.getOutputStream());
							out.write(buf);
							
							client.close();
							out.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						this.stopRespondingTimeTrack("SERVICE_GET_SENSOR_P2P", st);
					}
				}
			}
		}
		else if (argc == 5) {
			if (argv[0].equals("GET")) {
				if (argv[1].equals("DIR")) {
					if (argv[2].equals("P2P")) {
						
						long st = this.startRespondingTimeTrack();
						
						String [] xdir = argv[4].split("/");
						int nxdir = xdir.length;
						
						Cursor cur = resdbh.execQuery("SELECT * FROM "+resdbh.dbName);
						int n = resdbh.countRow(cur);
						if (n == 0) {
							try {
								DataOutputStream out = new DataOutputStream(client.getOutputStream());
								out.write(("POST|DIR|P2P|").getBytes());
								out.close();
								return;
							} catch (IOException e) {}
						}
						
						HashSet <String> pool = new HashSet <String>();
						
						cur.moveToFirst();
						for (int i = 0; i < n; i++) {
							String [] row = resdbh.fetchOneRow(cur);
							String [] acc = row[5].split("&");
							boolean legal = false;
							
							if (row[5].equals("1")) legal = true;
							else if (row[5].equals("0")) legal = false;
							else {
								for (String s: acc) {
									if (s.equals(argv[3])) {
										legal = true;
										break;
									}
								}
							}

							if (!legal) continue;
							String [] xrdir = ("/A/B"+row[4]).split("/");
							int nxrdir = xrdir.length;
							
							if (nxrdir <= nxdir) continue;
							else if (nxrdir - 1 == nxdir) {
								if (!pool.contains(xrdir[nxdir]))
									pool.add(xrdir[nxdir]);
							}
							else {
								if (!pool.contains(xrdir[nxdir]+"/"))
									pool.add(xrdir[nxdir]+"/"); 
							}
						}
						
						String replyMsg = "POST|DIR|P2P|";
						for (String s: pool) {
							replyMsg += s+"^-1;";
						}
						
						try {
							DataOutputStream out = new DataOutputStream(client.getOutputStream());
							out.write(replyMsg.getBytes());
						} catch (IOException e) {}
						this.stopRespondingTimeTrack("SERVICE_GET_DIR_P2P", st);
					}
				}
				else if (argv[1].equals("FILE")) {
					if (argv[2].equals("METADATA")) {
						if (argv[3].equals("P2P")) {
							
							long st = this.startRespondingTimeTrack();
							
							String [] meta = getLocalFileMetadata(argv[4]);
							String outMsg = "POST|FILE|METADATA|P2P|";
							if (meta == null)
								outMsg += "NO_RESOURCE";
							else {
								for (String s: meta)
									outMsg += s+"%";
								outMsg = outMsg.substring(0, outMsg.length()-1);
							}
							try {
								DataOutputStream out = new DataOutputStream(client.getOutputStream());
								out.write(outMsg.getBytes());
							} catch (IOException e) {}
							
							this.stopRespondingTimeTrack("SERVICE_FILE_METADATA_P2P", st);
						}
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

    						long st = startRespondingTimeTrack();
    						NetworkMessageParser(mCtx, incomingMsg, client);
    						stopRespondingTimeTrack("SERVICE_NETWORK_GENERAL_RESPONSE", st);
						
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
	
	private HashMap <String, FileOutputStream> logLocationHashMap = new HashMap <String, FileOutputStream>();
	private String locationBasePath = "/mnt/sdcard/Uno/SensorLogs/LOCATION";
	
	private void startLoggingLocation(String requestor) throws IOException {
		File dir = new File (locationBasePath);
		if (!dir.exists()) dir.mkdirs();
		
		File f = new File (locationBasePath+"/"+requestor);
		f.deleteOnExit();
		f.createNewFile();
		
		this.logLocationHashMap.put(requestor, new FileOutputStream(f));
	}
	
	private void stopLoggingLocation(String requestor) throws IOException {
		this.logLocationHashMap.get(requestor).close();
		this.logLocationHashMap.remove(requestor);
	}
	
	private void resetAllLocationLogs() throws IOException {
		for (String key: this.logLocationHashMap.keySet()) {
			this.logLocationHashMap.get(key).close();
			this.logLocationHashMap.remove(key);
		}
	}
	
	/*
	 * This part is for common sensor.
	 * */
    
    private class CommonSensorManager implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	
    	// sensor log control.
    	private HashMap <String, FileOutputStream> logAccelerometerHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logGyroscopemeterHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logLightmeterHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logMagnetometerHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logOrientationmeterHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logProximitymeterHashMap = new HashMap <String, FileOutputStream>();
    	private HashMap <String, FileOutputStream> logGravitymeterHashMap = new HashMap <String, FileOutputStream>();
    	
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
    	
    	// Sensor log base file name
    	private final String accelerometerBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_ACCELEROMETER";
    	private final String lightmeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_LIGHT";
    	private final String magnetometerBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_MAGNETIC_FIELD";
    	private final String orientationmeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_ORIENTATION";
    	private final String proximitymeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_PROXIMITY";
    	private final String gravitymeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_GRAVITY";
    	private final String gyroscopemeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_GYROSCOPE";
    	
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
    	
    	/*
    	 * Different types of sensor logging control, using hash map.
    	 * */
    	
    	// Accelerometer
    	public void startLoggingAccelerometer(String requestor) throws IOException {
    		
    		File f= new File(this.accelerometerBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.accelerometerBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logAccelerometerHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingAccelerometer(String requestor) throws IOException {
    		logAccelerometerHashMap.get(requestor).close();
    		logAccelerometerHashMap.remove(requestor);
    	}
    	
    	// Gyroscopemeter
    	public void startLoggingGyroscopemeter(String requestor) throws IOException {
    		
    		File f= new File(this.gyroscopemeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.gyroscopemeterBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logGyroscopemeterHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingGyroscopemeter(String requestor) throws IOException {
    		logGyroscopemeterHashMap.get(requestor).close();
    		logGyroscopemeterHashMap.remove(requestor);
    	}
    	
    	// Lightmeter
    	public void startLoggingLightmeter(String requestor) throws IOException {
    		
    		File f= new File(this.lightmeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.lightmeterBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logLightmeterHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingLightmeter(String requestor) throws IOException {
    		logLightmeterHashMap.get(requestor).close();
    		logLightmeterHashMap.remove(requestor);
    	}
    	
    	// Magnetometer
    	public void startLoggingMagnetometer(String requestor) throws IOException {
    		
    		File f= new File(this.magnetometerBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.magnetometerBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logMagnetometerHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingMagnetometer(String requestor) throws IOException {
    		logMagnetometerHashMap.get(requestor).close();
    		logMagnetometerHashMap.remove(requestor);
    	}
    	
    	// Orientationmeter
    	public void startLoggingOrientationmeter(String requestor) throws IOException {
    		
    		File f= new File(this.orientationmeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.orientationmeterBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logOrientationmeterHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingOrientationmeter(String requestor) throws IOException {
    		logOrientationmeterHashMap.get(requestor).close();
    		logOrientationmeterHashMap.remove(requestor);
    	}
    	
    	// Proximitymeter
    	public void startLoggingProximitymeter(String requestor) throws IOException {
    		
    		File f= new File(this.proximitymeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.proximitymeterBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logProximitymeterHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingProximitymeter(String requestor) throws IOException {
    		logProximitymeterHashMap.get(requestor).close();
    		logProximitymeterHashMap.remove(requestor);
    	}
    	
    	// Gravitymeter
    	public void startLoggingGravitymeter(String requestor) throws IOException {
    		
    		File f= new File(this.gravitymeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		f = new File(this.gravitymeterBasePath + "/" + requestor);
    		f.deleteOnExit();
    		f.createNewFile();
    		FileOutputStream os = new FileOutputStream(f);
    		logGravitymeterHashMap.put(requestor, os);
    	}
    	
    	public void stopLoggingGravitymeter(String requestor) throws IOException {
    		logGravitymeterHashMap.get(requestor).close();
    		logGravitymeterHashMap.remove(requestor);
    	}
    	
    	/*
    	 * Reset All logs.
    	 * */
    	public void resetAllLogs() throws IOException {
    		
    		for (String key: this.logAccelerometerHashMap.keySet()) {
    			this.logAccelerometerHashMap.get(key).close();
    			this.logAccelerometerHashMap.remove(key);
    		}
    		
    		for (String key: this.logGravitymeterHashMap.keySet()) {
    			this.logGravitymeterHashMap.get(key).close();
    			this.logAccelerometerHashMap.remove(key);
    		}
    		
    		for (String key: this.logGyroscopemeterHashMap.keySet()) {
    			this.logGyroscopemeterHashMap.get(key).close();
    			this.logGyroscopemeterHashMap.remove(key);
    		}
    		
    		for (String key: this.logLightmeterHashMap.keySet()) {
    			this.logLightmeterHashMap.get(key).close();
    			this.logLightmeterHashMap.remove(key);
    		}
    		
    		for (String key: this.logMagnetometerHashMap.keySet()) {
    			this.logMagnetometerHashMap.get(key).close();
    			this.logMagnetometerHashMap.remove(key);
    		}
    		
    		for (String key: this.logOrientationmeterHashMap.keySet()) {
    			this.logOrientationmeterHashMap.get(key).close();
    			this.logOrientationmeterHashMap.remove(key);
    		}
    		
    		for (String key: this.logProximitymeterHashMap.keySet()) {
    			this.logProximitymeterHashMap.get(key).close();
    			this.logProximitymeterHashMap.remove(key);
    		}
    	}
    	
    	// Start sensing
    	public void startSensor() {
    		mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Gyroscopemeter, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Lightmeter, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Orientationmeter, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Proximitymeter, SensorManager.SENSOR_DELAY_FASTEST);
    		mSensorManager.registerListener(this, this.Gravitymeter, SensorManager.SENSOR_DELAY_FASTEST);
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
    		
    		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
    			
    			this.accX = event.values[0]-this.gravityX;
    			this.accY = event.values[1]-this.gravityY;
    			this.accZ = event.values[2]-this.gravityZ;
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.accX) + ";" 
						+ String.valueOf(this.accY) + ";" + String.valueOf(this.accZ) + "\n";
				byte [] buf = str.getBytes();
				
    			for (String key: this.logAccelerometerHashMap.keySet()) {
    				try {
						this.logAccelerometerHashMap.get(key).write(buf);
						this.logAccelerometerHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
    			
    			this.spin = event.values[0];
    			this.output = event.values[1];
    			this.input = event.values[2];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.spin) + ";" 
						+ String.valueOf(this.output) + ";" + String.valueOf(this.input)+"\n";
				byte [] buf = str.getBytes();
				
				for (String key: this.logGyroscopemeterHashMap.keySet()) {
					try {
						this.logGyroscopemeterHashMap.get(key).write(buf);
						this.logGyroscopemeterHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
    			
    			this.light = event.values[0];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.light)+"\n";
				byte [] buf = str.getBytes();
				
				for (String key: this.logLightmeterHashMap.keySet()) {
					try {
						this.logLightmeterHashMap.get(key).write(buf);
						this.logLightmeterHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
    			
    			this.magX = event.values[0];
    			this.magY = event.values[1];
    			this.magZ = event.values[2];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.magX) + ";" 
						+ String.valueOf(this.magY) + ";" + String.valueOf(this.magZ)+"\n";
				byte [] buf = str.getBytes();
    			
				for (String key: this.logMagnetometerHashMap.keySet()) {
					try {
						this.logMagnetometerHashMap.get(key).write(buf);
						this.logMagnetometerHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
    			
    			this.rotationX = event.values[0];
    			this.rotationY = event.values[1];
    			this.rotationZ = event.values[2];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.rotationX) + ";" 
						+ String.valueOf(this.rotationY) + ";" + String.valueOf(this.rotationZ)+"\n";
				byte [] buf = str.getBytes();

				for (String key: this.logOrientationmeterHashMap.keySet()) {
					try {
						this.logOrientationmeterHashMap.get(key).write(buf);
						this.logOrientationmeterHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
    			
    			this.proximity = event.values[0];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.proximity) + "\n";
				byte [] buf = str.getBytes();

				for (String key: this.logProximitymeterHashMap.keySet()) {
					try {
						this.logProximitymeterHashMap.get(key).write(buf);
						this.logProximitymeterHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    		else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
    			
    			this.gravityX = event.values[0];
    			this.gravityY = event.values[1];
    			this.gravityZ = event.values[2];
    			
    			Date d = new Date();
				String str = d.toGMTString()+":"+String.valueOf(this.gravityX) + ";" 
					+ String.valueOf(this.gravityY) + ";" + String.valueOf(this.gravityZ)+"\n";
				byte [] buf = str.getBytes();

				for (String key: this.logGravitymeterHashMap.keySet()) {
					try {
						this.logGravitymeterHashMap.get(key).write(buf);
						this.logGravitymeterHashMap.get(key).flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
    
    private String [] readSensorValues (String type) {
		String [] val = null;
		if (type.equals("TYPE_ACCELEROMETER")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.accX);
			val[1] = String.valueOf(mComSensorMgr.accY);
			val[2] = String.valueOf(mComSensorMgr.accZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_GRAVITY")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.gravityX);
			val[1] = String.valueOf(mComSensorMgr.gravityY);
			val[2] = String.valueOf(mComSensorMgr.gravityZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_GYROSCOPE")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.spin);
			val[1] = String.valueOf(mComSensorMgr.output);
			val[2] = String.valueOf(mComSensorMgr.input);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_LIGHT")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.light);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("TYPE_MAGNETIC_FIELD")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.magX);
			val[1] = String.valueOf(mComSensorMgr.magY);
			val[2] = String.valueOf(mComSensorMgr.magZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_ORIENTATION")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.rotationX);
			val[1] = String.valueOf(mComSensorMgr.rotationY);
			val[2] = String.valueOf(mComSensorMgr.rotationZ);
			val[3] = "N/A";
		}
		else if (type.equals("TYPE_PROXIMITY")) {
			val = new String[4];
			val[0] = String.valueOf(mComSensorMgr.proximity);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("TYPE_SOUNDMETER")) {
			val = new String[4];
			val[0] = String.valueOf(UnoService.soundPressureValue);
			val[1] = val[2] = val[3] = "N/A";
		}
		else if (type.equals("LOCATION")) {
			val = new String[4];
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