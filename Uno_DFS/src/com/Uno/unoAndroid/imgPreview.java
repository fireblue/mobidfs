package com.Uno.unoAndroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class imgPreview extends Activity {

	private ImageView imgv = null;
	private String path = null;
	private Bitmap bmp = null;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.imgpreview);
		
		imgv = (ImageView) findViewById(R.id.imgPreview);
		path = this.getIntent().getExtras().getString("PREVIEW_PATH");
		bmp = BitmapFactory.decodeFile(path);
		imgv.setImageBitmap(bmp);
	}
}
