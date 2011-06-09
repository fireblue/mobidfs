package com.Uno.unoAndroid;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class Search extends Activity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
        	String query = intent.getStringExtra(SearchManager.QUERY);
        	// TODO search & present result
        }
    }
}
