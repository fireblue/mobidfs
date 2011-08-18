package com.Uno.unoAPIs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
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

public class LocalSensor {

	/*
	 * Variables.
	 * ----------------------------------------------------
	 * */
	private String localSensorType = null;
	private String sensorAccessList = "0"; // default.
	private String statusInCloud = "offline"; // default.
	private String [] readings = new String[4];
	private boolean isSensing = false;
	private boolean isLogging = false;
	private Context ctx = null;
	private CommonSensorManager comSensorMgr = null;
	private SoundMeterManager soundMgr = null;
	private LocationManager mLocationMgr = null;
	private String [] locReadings = new String[4];
	
	/*
	 * Public Methods.
	 * ----------------------------------------------------
	 * */
	public String getLocalSensorType() {
		return this.localSensorType;
	}
	
	public String getAccessList() {
		return this.sensorAccessList;
	}
	
	public String getInCloudStatus() {
		return this.statusInCloud;
	}
	
	public boolean isSensing() {
		return this.isSensing;
	}
	
	public boolean isLogging() {
		return this.isLogging;
	}
	
	public void setAccessList(String newlist) {
		this.sensorAccessList = newlist;
	}
	
	public String [] getReadings() {
		if (this.localSensorType.equals("TYPE_SOUNDMETER")) {
			this.soundMgr.startMeasure();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
			this.soundMgr.stopMesaure();
			this.readings[0] = String.valueOf(this.soundMgr.SoundLevel);
			return this.readings;
		}
		else if (this.localSensorType.equals("LOCATION")) {
			this.startCoarseLocationService();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
			this.stopCoarseLocationService();
			this.readings = this.locReadings;
			return this.readings;
		}
		else {
			this.comSensorMgr.startSensor();
			while (this.readings[0].equals(this.comSensorMgr.readings[0]));
			this.comSensorMgr.stopSensor();
			this.readings = this.comSensorMgr.getReadings();
			return this.readings;
		}
	}
	
	// Currently, logging does not apply to sountmeter and location service.
	public void startLogging() {
		this.comSensorMgr.startLogging();
	}
	
	public void stopLogging() {
		this.comSensorMgr.stopLogging();
	}
	
	/*
	 * Constructors.
	 * ------------------------------------------------
	 * */
	public LocalSensor(int type, Context c) {
		this.localSensorType = typeToName(type);
		this.sensorAccessList = "0";
		this.statusInCloud = "offline";
		this.isSensing = false;
		this.isLogging = false;
		this.ctx = c;
		this.comSensorMgr = new CommonSensorManager(this.ctx);
	}
	
	public LocalSensor(String name, Context c) {
		this.localSensorType = name;
		this.sensorAccessList = "0";
		this.statusInCloud = "offline";
		this.isSensing = false;
		this.isLogging = false;
		this.ctx = c;
		if (name.equals("TYPE_SOUNDMETER"))
			this.soundMgr = new SoundMeterManager();
		else if (name.equals("LOCATION"))
			this.mLocationMgr = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
		else
			this.comSensorMgr = new CommonSensorManager(this.ctx);
	}
	
	/*
	 * Local methods
	 * ------------------------------------------------
	 * */
	private String typeToName(int type) {
		switch (type) {
		case Sensor.TYPE_ACCELEROMETER: return "TYPE_ACCELEROMETER";
		case Sensor.TYPE_GRAVITY: return "TYPE_GRAVITY";
		case Sensor.TYPE_GYROSCOPE: return "TYPE_GYROSCOPE";
		case Sensor.TYPE_LIGHT: return "TYPE_LIGHT";
		case Sensor.TYPE_MAGNETIC_FIELD: return "TYPE_MAGNETIC_FIELD";
		case Sensor.TYPE_ORIENTATION: return "TYPE_ORIENTATION";
		case Sensor.TYPE_PRESSURE: return "TYPE_PRESSURE";
		case Sensor.TYPE_PROXIMITY: return "TYPE_PROXIMITY";
		case Sensor.TYPE_TEMPERATURE: return "TYPE_TEMPERATURE";
		default: return null;
		}
	}
	
	private int nameToType(String name) {
		if (name.equals("TYPE_ACCELEROMETER")) return Sensor.TYPE_ACCELEROMETER;
		if (name.equals("TYPE_GRAVITY")) return Sensor.TYPE_GRAVITY;
		if (name.equals("TYPE_GYROSCOPE")) return Sensor.TYPE_GYROSCOPE;
		if (name.equals("TYPE_LIGHT")) return Sensor.TYPE_LIGHT;
		if (name.equals("TYPE_MAGNETIC_FIELD")) return Sensor.TYPE_MAGNETIC_FIELD;
		if (name.equals("TYPE_ORIENTATION")) return Sensor.TYPE_ORIENTATION;
		if (name.equals("TYPE_PRESSURE")) return Sensor.TYPE_PRESSURE;
		if (name.equals("TYPE_PROXIMITY")) return Sensor.TYPE_PROXIMITY;
		if (name.equals("TYPE_TEMPERATURE")) return Sensor.TYPE_TEMPERATURE;
		return -1;
	}
	
	// Common Sensor
	private class CommonSensorManager implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	private Sensor Accelerometer;
    	private Sensor Gyroscopemeter;
    	private Sensor Lightmeter;
    	private Sensor Magnetometer;
    	private Sensor Orientationmeter;
    	private Sensor Proximitymeter;
    	private Sensor Gravitymeter;
    	private boolean isRunning = false;
    	private boolean isLogging = false;
    	private FileOutputStream fos = null;
    	private final String loggingBasePath = "/mnt/sdcard/Uno/SensorLogs/";
    	public String [] readings = new String[4];
    	
    	public CommonSensorManager(Context c)
    	{
    		this.mSensorManager = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
    		if (localSensorType.equals("TYPE_ACCELEROMETER")) 
    			this.Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    		else if (localSensorType.equals("TYPE_GYROSCOPE"))
    			this.Gyroscopemeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    		else if (localSensorType.equals("TYPE_LIGHT"))
    			this.Lightmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    		else if (localSensorType.equals("TYPE_MAGNETIC_FIELD"))
    			this.Magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    		else if (localSensorType.equals("TYPE_ORIENTATION"))
    			this.Orientationmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		else if (localSensorType.equals("TYPE_PROXIMITY"))
    			this.Proximitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    		else if (localSensorType.equals("TYPE_GRAVITY"))
    			this.Gravitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    	}
    	
    	// Start sensing
    	public void startSensor() {
    		this.isRunning = true;
    		if (localSensorType.equals("TYPE_ACCELEROMETER"))
    			mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_GYROSCOPE"))
    			mSensorManager.registerListener(this, this.Gyroscopemeter, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_LIGHT"))
    			mSensorManager.registerListener(this, this.Lightmeter, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_MAGNETIC_FIELD"))
    			mSensorManager.registerListener(this, this.Magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_ORIENTATION"))
    			mSensorManager.registerListener(this, this.Orientationmeter, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_PROXIMITY"))
    			mSensorManager.registerListener(this, this.Proximitymeter, SensorManager.SENSOR_DELAY_NORMAL);
    		else if (localSensorType.equals("TYPE_GRAVITY"))
    			mSensorManager.registerListener(this, this.Gravitymeter, SensorManager.SENSOR_DELAY_NORMAL);
    	}
    	
    	// Stop sensing
    	public void stopSensor() {
    		this.isRunning = false;
    		if (localSensorType.equals("TYPE_ACCELEROMETER"))
    			mSensorManager.unregisterListener(this, this.Accelerometer);
    		else if (localSensorType.equals("TYPE_GYROSCOPE"))
    			mSensorManager.unregisterListener(this, this.Gyroscopemeter);
    		else if (localSensorType.equals("TYPE_LIGHT"))
    			mSensorManager.unregisterListener(this, this.Lightmeter);
    		else if (localSensorType.equals("TYPE_MAGNETIC_FIELD"))
    			mSensorManager.unregisterListener(this, this.Magnetometer);
    		else if (localSensorType.equals("TYPE_ORIENTATION"))
    			mSensorManager.unregisterListener(this, this.Orientationmeter);
    		else if (localSensorType.equals("TYPE_PROXIMITY"))
    			mSensorManager.unregisterListener(this, this.Proximitymeter);
    		else if (localSensorType.equals("TYPE_GRAVITY"))
    			mSensorManager.unregisterListener(this, this.Gravitymeter);
    	}
    	
    	public void startLogging() {
    		if (!this.isRunning) this.startSensor();
    		this.isLogging = true;
    		File f = new File(this.loggingBasePath+localSensorType+"/");
    		if (!f.exists())
    			f.mkdirs();
    		Date d = new Date();
    		f = new File(this.loggingBasePath+localSensorType+"/"+"local_"+String.valueOf(d.getTime()));
    		try {
    			if (!f.exists()) f.createNewFile();
    			fos = new FileOutputStream(f);
    		}
    		catch(Exception e){}
    	}
    	
    	public void stopLogging() {
    		this.isLogging = false;
    		try {
				fos.close();
			} 
    		catch(Exception e) {}
    		if (this.isRunning) this.stopSensor();
    	}
    	
    	public String [] getReadings() {
    		return this.readings;
    	}
    	
    	public void onSensorChanged(SensorEvent event) {
    		
	    	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	    		for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}
	    	}
	    	
	    	if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
	    		for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}		
	    	}
	  		
	   		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
	   			for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}		
	   		}
	   		
	   		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
	   			for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}		
	   		}
	   		
	   		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
	   			for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}	
	   		}
	   		
	   		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
	   			for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}		
	   		}
	   		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
	   			for(int i = 0; i < 3; i++)
					this.readings[i] = String.valueOf(event.values[i]);
	    		if (this.isLogging) {
	    			try {
	    				fos.write((this.readings[0]+","+this.readings[1]+","+this.readings[2]+"\n").getBytes());
	    				fos.flush();
	    			}
	    			catch (Exception e) {}
	    		}		
	   		}
    
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
	
	// Soundmeter
	private class SoundMeterManager extends Thread {
    	
    	private boolean isRunning = false;
    	private boolean isLogging = false;
    	private double SoundLevel;
    	private AudioRecord mrec = null;
    	private String soundmeterBasePath = "/mnt/sdcard/Uno/SensorLogs/TYPE_SOUNDMETER/";
    	private int BufferSize = 0;
    	private FileOutputStream fos = null;
    	
    	public SoundMeterManager() {
    		BufferSize = 2*AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
    		mrec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize);
    		isRunning = false;
    		SoundLevel = 0.0;
    	}
    	
    	public double getSoundPressure() {
    		return this.SoundLevel;
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
    	
    	public void startLoggingSoundmeter() {
    		if (!this.isRunning) this.startMeasure();
    		
    		this.isLogging = true;
    		
    		File f = new File(this.soundmeterBasePath);
    		if (!f.exists()) f.mkdirs();
    		
    		Date d = new Date();     		
    		f = new File(this.soundmeterBasePath+"local_"+String.valueOf(d.getTime()));
    		try {
    			f.createNewFile();
    			fos = new FileOutputStream(f);
    		}
    		catch(Exception e) {}
    	}
    	
    	public void stopLogging(String requestor) throws IOException {
    		this.isLogging = false;
    		
    		try {
    			fos.close();
    		}
    		catch(Exception e) {}
    		
    		if (this.isRunning) this.stopMesaure();
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
    			SoundLevel = 20.0*Math.log10(sum/BufferSize);
    			try {
					fos.write((String.valueOf(SoundLevel)+"\n").getBytes());
					fos.flush();
				} catch (Exception e) {}
    		}
    		if (mrec != null) {
    			mrec.stop();
    			mrec.release();
    			mrec = null;
    		}
    	}
    }
	
	// Location Sensor

	private LocationListener coarseListener = new LocationListener() {
	
		public void onLocationChanged(Location location) {
			locReadings[0] = String.valueOf(location.getLatitude());
			locReadings[1] = String.valueOf(location.getLongitude());
			locReadings[2] = String.valueOf(location.getAltitude());
			locReadings[3] = String.valueOf(location.getAccuracy());
			
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
			locReadings[0] = String.valueOf(location.getLatitude());
			locReadings[1] = String.valueOf(location.getLongitude());
			locReadings[2] = String.valueOf(location.getAltitude());
			locReadings[3] = String.valueOf(location.getAccuracy());
			
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
		mLocationMgr = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, coarseListener);
	}
	
	private void stopCoarseLocationService() {
		mLocationMgr = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
		//LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.removeUpdates(coarseListener);
	}
	
	private void startFineLocationService() {
		mLocationMgr = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createFineCriteria(), true));
		mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, fineListener);
	}
	
	private void stopFineLocationService() {
		mLocationMgr = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
		//LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		mLocationMgr.removeUpdates(fineListener);
	}
}
