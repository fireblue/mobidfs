package com.Uno.unoAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;


public class pdfPreview extends Activity {
	
	private String path = null;
	private File f = null;
	private TextView pdfv = null;
	
	private PDFParser parser = null;
	private PDFTextStripper pdfStripper = null;
	private PDDocument pdDoc = null;
	private COSDocument cosDoc = null;
	private String text = null;
	private int numberOfPages = 0;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.pdfpreview);
		
		pdfv = (TextView) findViewById(R.id.pdfPreview);
		
		path = this.getIntent().getExtras().getString("PREVIEW_PATH");
		f = new File(path);
		
		try {
			pdDoc = PDDocument.load(f);
			numberOfPages = pdDoc.getNumberOfPages();
			pdfStripper = new PDFTextStripper();
			pdfStripper.setStartPage(1);
			pdfStripper.setEndPage(5);
			//pdfStripper.setEndPage(pdDoc.getNumberOfPages());
			text = pdfStripper.getText(pdDoc);
			pdDoc.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pdfv.setText(text);
		
	}
}
