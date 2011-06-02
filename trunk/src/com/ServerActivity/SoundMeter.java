package com.ServerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class SoundMeter extends Thread{
	
	private boolean isRunning = false;
	private boolean isLogging = false;
	public double SoundPressure;
	private AudioRecord mrec = null;
	private String senSound = "/mnt/sdcard/sensor/sound.sensor";
	private File f = null;
	private FileOutputStream fos = null;
	private int BufferSize = 0;
	
	public SoundMeter(Context mContext)
	{
		BufferSize = 2*AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		mrec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize);
		isRunning = false;
		isLogging = false;
		SoundPressure = 0.0;
	}
	
	public double getSoundPressure()
	{
		return this.SoundPressure;
	}
	
	public void startMeasure()
	{
		isRunning = true;
		try 
		{
			this.start();
		} catch (Exception e) {}
	}
	
	public void stopMesaure()
	{
		isRunning = false;
	}
	
	public void startLogging()
	{
		isLogging = true;
		f = new File(senSound);
		try
		{
			if (!f.exists()) f.createNewFile();
		} catch (Exception e){}
		
		try
		{
			fos = new FileOutputStream(f, true);
		} catch (Exception e) {}
	}
	
	public void stopLogging()
	{
		isLogging = false;
		try
		{
			fos.close();
		} catch (Exception e) {}
	}
	
	private void writeLog(double value)
	{
		if (!isLogging) return;
		Date cur = new Date();
		String rec = cur.toGMTString()+","+String.valueOf(value)+"\n";
		try
		{
			fos.write(rec.getBytes());
			fos.flush();
		} catch (Exception e) {}
	}
	
	public void run()
	{
		mrec.startRecording();
		while (isRunning)
		{
			short [] tmpBuf = new short[BufferSize];
			mrec.read(tmpBuf, 0, BufferSize);
			double sum = 0.0;
			for (int i = 0; i < BufferSize; i++)
				sum += Math.abs(tmpBuf[i]);
			SoundPressure = 20.0*Math.log10(sum/BufferSize);
			writeLog(SoundPressure);
			/*try
			{
				Thread.sleep(500);
			} catch (Exception e) {}*/
		}
		if (mrec != null)
		{
			mrec.stop();
			mrec.release();
			mrec = null;
		}
	}

}
