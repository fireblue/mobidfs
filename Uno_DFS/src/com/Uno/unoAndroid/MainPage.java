package com.Uno.unoAndroid;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class MainPage extends TabActivity {
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);
        
        startService(new Intent(getApplicationContext(), UnoService.class));
        
        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabSpec spec = null;
        Intent intent = null;
        
        // Local Home tab
        intent = new Intent().setClass(this, LocalHome.class);
        spec = tabHost.newTabSpec("home").setIndicator("Home").setContent(intent);
        tabHost.addTab(spec);
        
        // Uno Network tab
        intent = new Intent().setClass(this, UnoNetwork.class);
        spec = tabHost.newTabSpec("network").setIndicator("Network").setContent(intent);
        tabHost.addTab(spec);
        
        // Local Sensor tab
        intent = new Intent().setClass(this, LocalSensor.class);
        spec = tabHost.newTabSpec("sensor").setIndicator("Sensor").setContent(intent);
        tabHost.addTab(spec);
        
        // Search tab
        intent = new Intent().setClass(this, Search.class);
        spec = tabHost.newTabSpec("search").setIndicator("Search").setContent(intent);
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);
    }
}
