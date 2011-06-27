package com.Uno.unoAndroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class txtPreview extends Activity {
	
	private TextView txtv = null;
	private String path = null;
	private File f = null;
	private BufferedReader br = null;
	private String buf = null;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.txtpreview);
		
		txtv = (TextView) findViewById(R.id.txtPreview);
		path = this.getIntent().getExtras().getString("PREVIEW_PATH");
		f = new File(path);
		try {
			br = new BufferedReader(new FileReader(f));
			String line = "";
			while ((line = br.readLine()) != null) {
				buf += line + "\n";
			}
		} catch (Exception e) {}
		
		txtv.setText(buf);
	}
}
