package com.ServerActivity;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class mobiCamera 
{
	
	private String senCamera = "/mnt/sdcard/sensor/camera.jpg";
	private Camera camera = null;
	private SurfaceView view = null;
	private SurfaceHolder holder = null;
	
	
	public mobiCamera(Activity act)
	{		
		CameraInfo ci = new CameraInfo();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++)
		{
			Camera.getCameraInfo(i, ci);
			if (ci.facing == ci.CAMERA_FACING_BACK)
			{
				camera = Camera.open(i);
				break;
			}
		}
		
		view = new SurfaceView(act);
		holder = view.getHolder();
		try
		{
			camera.setPreviewDisplay(view.getHolder());
		} catch (Exception e) {}
		//Camera.Parameters params = camera.getParameters();
		//params.setPictureFormat(PixelFormat.JPEG);
		//camera.setParameters(params);
	}
	
	public void shutdown()
	{
		camera.release();
		try
		{
			camera.setPreviewDisplay(null);
		} catch (Exception e) {}
	}
	
	ShutterCallback shutterCallback = new ShutterCallback()
	{

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub
			
		}
	};

	PictureCallback rawCallback = new PictureCallback() 
	{
		@Override
		public void onPictureTaken(byte[] arg0, android.hardware.Camera arg1) {
			// TODO Auto-generated method stub
			
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] arg0, android.hardware.Camera arg1) {
			FileOutputStream os = null;
			try 
			{
				File f = new File("/mnt/sdcard/sensor/");
				if (!f.exists()) f.mkdir();
				File f1 = new File(senCamera);
				f1.deleteOnExit();
				
				os = new FileOutputStream(senCamera);
				os.write(arg0);
				os.flush();
				os.close();
			} catch (Exception e) {}
		}
	};
	
	public void takePicture()
	{
		try
		{
			//while ()
			camera.startPreview();
			camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		} catch (Exception e) 
		{
			Log.e("Camera", e.toString());
		}
	}
}
