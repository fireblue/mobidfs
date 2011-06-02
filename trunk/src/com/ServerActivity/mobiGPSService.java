package com.ServerActivity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class mobiGPSService extends Service{

	private LocationManager lm = null;
	private mobiLocationListener mll = null;
	public static double latitude = 0;
	public static double longitude = 0;
	public static double altitude = 0;
	public static double accuracy = 0;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void startLocating()
	{
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mll = new mobiLocationListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, mll);
	}
	
	public void stopLocating()
	{
		lm.removeUpdates(mll);
	}
	
	@Override
	public void onCreate()
	{
		this.startLocating();
	}
	
	@Override
	public void onDestroy()
	{
		this.stopLocating();
	}
	
	public class mobiLocationListener implements LocationListener
	{

		@Override
		public void onLocationChanged(Location loc) {
			if (loc == null) return;
			try
			{
				if (!loc.hasAccuracy()) return;
				mobiGPSService.accuracy = loc.getAccuracy();
				mobiGPSService.altitude = loc.getAltitude();
				mobiGPSService.longitude = loc.getLongitude();
				mobiGPSService.latitude = loc.getLatitude();
			} catch(Exception e) {}
		}

		@Override
		public void onProviderDisabled(String arg0) {
			Toast.makeText(getBaseContext(), "onProviderDisabled: "+arg0, Toast.LENGTH_SHORT).show();
			
		}

		@Override
		public void onProviderEnabled(String arg0) {
			Toast.makeText(getBaseContext(), "onProviderEnabled: "+arg0, Toast.LENGTH_SHORT).show();
			
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
