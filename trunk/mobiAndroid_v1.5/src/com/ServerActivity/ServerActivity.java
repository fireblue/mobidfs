package com.ServerActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class ServerActivity extends Activity {
    /** Called when the activity is first created. */
	private TextView tv;
	private EditText et;
	private ScrollView sv;
	
	private Vibrator vbr = null;
	
	public static String LOCALLISTENIP = null;
	public static String MASTERIP = "192.168.10.126";
	private Handler handler = new Handler();
	
	private Thread tcplist = new Thread(new TCPListenThread());
	private Thread udplist = new Thread(new UDPListenThread());
	private Thread scflist = new Thread(new SocketFileListenThread());
	
	private SensorMaintenance sm = null;
	private SoundMeter msound = null;
	private mobiCamera cam = null;
	
	private ServerSocket tcpServer;
	private ServerSocket scfServer;
	
	public static String pwd = "/mnt/sdcard"; // I think the default home should be /data/download
	public static int timestamp = 0;
	
	private static DatabaseHandler dbh;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // start listen thread
        tcplist.start();
        udplist.start();
        scflist.start();
        
        msound = new SoundMeter(ServerActivity.this);
        msound.startMeasure();
        
        sm = new SensorMaintenance(ServerActivity.this);
        sm.startSensor();
        
        cam = new mobiCamera(ServerActivity.this);
        //startService(new Intent(ServerActivity.this, mobiGPSService.class));
        
        setContentView(R.layout.main);
        tv = (TextView) findViewById(R.id.shellView);
        sv = (ScrollView) findViewById(R.id.scrollView);
        tv.setMovementMethod(new ScrollingMovementMethod());
        LOCALLISTENIP = getLocalIPAddr();
       
        vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        dbh = new DatabaseHandler(ServerActivity.this);
        
        setupBluetoothDevice();
        
        tv.setText("");
        this.tcpSend("192.168.10.126", 11314, "Join");
        this.registerReceiver(this.mbr, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tv.append(this.getDeviceInfo()+"\n");
        sv.smoothScrollTo(0, tv.getBottom());
        et = (EditText) findViewById(R.id.cmdText);
        et.setOnKeyListener(new OnKeyListener()
        {
        	public boolean onKey(View v, int keyCode, KeyEvent event)
        	{
        		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))
        		{
        			String cmd = et.getText().toString();
        			tv.append(">"+cmd+"\n");
        			sv.smoothScrollTo(0, tv.getBottom());
        			et.setText("");
        			cmdParser(cmd);
        			return true;
        		}
        		return false;
        	}
        });
    }
    
    @Override
    public void onStop()
    {
    	super.onStop();
    	msound.stopMesaure();
    	stopService(new Intent(ServerActivity.this, mobiGPSService.class));
    	try
    	{
    		tcplist.stop();
    		udplist.stop();
    		scflist.stop();
    		tcpServer.close();
    		scfServer.close();
    	}
    	catch (Exception e)
    	{
    		Log.d("onStop", e.toString());
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	sm.startSensor();
    	msound.startMeasure();
    	//startService(new Intent(ServerActivity.this, mobiGPSService.class));
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	sm.stopSensor();
    	stopService(new Intent(ServerActivity.this, mobiGPSService.class));
    	//msound.stopMesaure();
    }
    
    public class TCPListenThread implements Runnable
    {
    	public void run()
    	{
    		try
    		{
    			if (LOCALLISTENIP == null)
    				LOCALLISTENIP = getLocalIPAddr();
    			if (LOCALLISTENIP != null)
    			{
    				tcpServer = new ServerSocket(11314);
    				while (true)
    				{
    					Socket client = tcpServer.accept();
    					try
    					{
    						BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    						String line = "";
    						String incomingMsg = "";
    						while ((line = in.readLine()) != null)
    						{
    							Log.d("Server", line);
    							incomingMsg += line;
    						}
    						postMessage("[Ctrl Msg] TCP Packet Receive: "+incomingMsg+"\n");
    						msgParser(incomingMsg);
    						
    						client.close();
    					}
    					catch (Exception e)
    					{
    						postMessage("[Ctrl Msg] TCP Receive interrupted...\n");
    						e.printStackTrace();
    						client.close();
    					}
    				}
    				
    			}
    			else
    			{
    				postMessage("[System] Offline...\n");
    			}
    		}
    		catch (Exception e)
    		{
    			postMessage("[System] TCP Port on use...\n");
    			Log.e("TCP", e.toString());
    		}
    	}
    }
    
    public class UDPListenThread implements Runnable
    {
    	public void run()
    	{
    		
    		try
    		{   			
    			if (LOCALLISTENIP == null)
    				LOCALLISTENIP = getLocalIPAddr();
    			if (LOCALLISTENIP != null)
    			{
    				DatagramSocket sck = new DatagramSocket(11315);
	    			while (true)
	    			{
	    				byte [] buf = new byte[1024];
	        			DatagramPacket packet = new DatagramPacket(buf, buf.length);
	    				sck.receive(packet);
		    			String incomingMsg = new String(buf, 0,buf.length).trim();
		    			postMessage("[Ctrl Msg] UDP Packet received: "+incomingMsg+"\n");
		    			msgParser(incomingMsg);
	    			}
    			}
    			else
    			{
    				postMessage("[System] Offline...\n");
    			}
    		}
    		catch(Exception e)
    		{
    			Log.e("UDP", e.toString());
    			postMessage("[System] UDP Port on use...\n");
    		}
    	}
    }    
    
    public class SocketFileListenThread implements Runnable
    {
    	public void run()
    	{
    		try
    		{
    			if (LOCALLISTENIP == null)
    				LOCALLISTENIP = getLocalIPAddr();
    			if (LOCALLISTENIP != null)
    			{
    				scfServer = new ServerSocket(11316);
    				while (true)
    				{
    					Socket client = scfServer.accept();
    					try
    					{
    						BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    						String line = "";
    						String incomingMsg = in.readLine();
    						
    						postMessage("[Ctrl Msg] FTP Received: "+incomingMsg+"\n");
    						String [] data = incomingMsg.split("\\|");
    						if (data[0].equals("FTP"))  // currently use sync file cp.
    						{
    							postMessage("[Ctrl Msg] File Request @ "+data[1]+"\n");
    							File f = new File(data[1]);
    							byte [] buf = new byte[client.getSendBufferSize()];
    							FileInputStream fis = new FileInputStream(f);
    							BufferedInputStream bis = new BufferedInputStream(fis);
    							OutputStream os = client.getOutputStream();
    							while (bis.read(buf) > 0)
    							{
    								os.write(buf);
    								os.flush();
    								buf = new byte[client.getSendBufferSize()];
    							}
    							postMessage("[System] File responding success.\n");
    							client.close();
    						}
    					}
    					catch (Exception e)
    					{
    						postMessage("[Ctrl Msg] FTP Receive interrupted...\n");
    						e.printStackTrace();
    						client.close();
    					}
    				}
    				
    			}
    			else
    			{
    				postMessage("[System] Offline...\n");
    			}
    		}
    		catch (Exception e)
    		{
    			postMessage("[System] FTP Port on use...\n");
    			Log.e("TCP", e.toString());
    		}
    	}
    }
    
    public class SocketFileRequest implements Runnable
    {
    	private String src = "";
    	private String ip = "";
    	private String dest = "";
    	
    	public SocketFileRequest(String src, String ip, String dest)
    	{
    		this.src = src;
    		this.ip = ip;
    		this.dest = dest;
    	}
    	
        public void run() // raw_src and dest should be full path.
        {
        	String msg = "FTP|"+src;
        	try
        	{
        		InetAddress remote = InetAddress.getByName(ip);
        		Socket s = new Socket(remote, 11316);
        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
        		out.println(msg);
        		postMessage("[Ctrl Msg] File request send, waiting for reponse.\n");
        		// receive file and write to local address.
        		InputStream sin = s.getInputStream();
        		
        		byte [] buf = new byte[s.getReceiveBufferSize()];
        		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(dest), true)); // append.
        		
        		postMessage("[System] In processing...\n");
        		while (true)
        		{
        			int nbytes = sin.read(buf);
        			if (nbytes < 0) break;
        			bout.write(buf, 0, nbytes);
        			bout.flush();
        		}
        		bout.close();
        		postMessage("[System] Done!\n");
        	}
        	catch (Exception e)
        	{
        		Log.e("SocketFile", e.toString());
        		postMessage("[System] File request failed...\nPlease try again!!\n");
        	}
        }
    }
    
    public class BluetoothFileListenThread implements Runnable
    {
    	public void run()
    	{
    		
    	}
    }
    
    public String getLocalIPAddr()
    {
    	try
    	{
    		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
    		{
    			NetworkInterface intf = en.nextElement();
    			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
    			{
    				InetAddress inetAddr = enumIpAddr.nextElement();
    				if (!inetAddr.isLoopbackAddress())
    				{
    					postMessage("IP: "+inetAddr.getHostAddress().toString()+"\n");
    					return inetAddr.getHostAddress().toString();
    				}
    			}
    		}
    		return null;
    	}
    	catch (SocketException e)
    	{
    		Log.e("Server", e.toString());
    	}
    	return null;
    }
    
    public static int blevel;
    private BroadcastReceiver mbr = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context arg0, Intent arg1)
    	{
    		int level = arg1.getIntExtra("level", 0);
    		ServerActivity.blevel = level;
    	}
    };
    
    public boolean tcpSend(String IP, int port, String msg)
    {
    	try
    	{
    		InetAddress remoteAddr = InetAddress.getByName(IP);
    		Socket socket = new Socket(remoteAddr, port);
    		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    		out.println(msg);
    		socket.close();
    		Log.d("TCP", "send done.");
    		return true;
    	}
    	catch (Exception e)
    	{
    		Log.e("TCP", e.toString());
    		return false;
    	}
    }
    
    public boolean udpSend(String IP, int port, String msg)
    {
    	try
    	{
    		InetAddress serveraddr = InetAddress.getByName(IP);
    		DatagramSocket socket = new DatagramSocket();
    		byte [] buf = msg.getBytes();
    		DatagramPacket packet = new DatagramPacket(buf, buf.length, serveraddr, port);
    		socket.send(packet);
    		Log.d("UDP", "send done.");
    		return true;
    	}
    	catch (Exception e)
    	{
    		Log.e("UDP", e.toString());
    	}
    	return false;
    }
    
    public void msgParser(String msg)  // This is used to parse the incoming msg.
    {
    	String [] argv = msg.split("\\|");
    	int argc = argv.length;
    	
    	if (argc == 1)
    	{
    		if (argv[0].equals("ACK2"))
    		{
    			postMessage("[System] Connection established and Sync Finished...\n");
    			return;
    		}
    		if (argv[0].equals("Ongoing"))
    		{
    			postMessage("[System] Request posted, waiting for response...\n");
    			return;
    		}
    		if (argv[0].equals("HB"))
    		{
    			String outgoingMsg = "Dir|";
    			outgoingMsg += updateResource();
    			// TODO clean database
    			this.udpSend(MASTERIP, 11315, outgoingMsg);
    			return;
    		}
    		if (argv[0].equals("Sync"))
    		{
    			this.tcpSend(MASTERIP, 11314, "Join");
    			postMessage("[System] IP Address update, re-sync...\n");
    			return;
    		}
    	}
    	else if (argc == 2)
    	{
    		if (argv[0].equals("ACK0") && argv[1].equals("Info"))
    		{
    			//String outgoingMsg = "Info|mouse;1;98;abc;0;0;null;1;100;aaa;0;78";
    			String outgoingMsg = getDeviceInfo();
    			this.tcpSend(MASTERIP, 11314, outgoingMsg);
    			return;
    		}
    		if (argv[0].equals("ACK1") && argv[1].equals("dirUpdate"))
    		{
    			String outgoingMsg = "Dir|";
    			outgoingMsg += updateResource();
    			// TODO clean database
    			this.tcpSend(MASTERIP, 11314, outgoingMsg);
    			return;
    		}
    		if (argv[0].equals("Close"))
    		{
    			if (!setNetworkInterface(argv[1], false))
    			{
    				postMessage("[Warning] cannot close interface -> " + argv[1] + "\n => You may close manually...\n");
    			}
    			return;
    		}
    		if (argv[0].equals("ACK3"))
    		{
    			if (argv[1] == "Failed")
    			{
    				postMessage("[System] Sorry, no such resource...\n");
    				return;
    			}
    			String [] res = argv[1].split(";");
    			String ans = "\n-----[Search Result]-----\n";
    			for (int i = 0; i < res.length; i++)
    				ans += (String.valueOf(i+1)+": "+res[i]+"\n");
    			ans += "\n-------------------------\n";
    			postMessage(ans);
    			return;
    		}
    		if (argv[0].equals("E"))
    		{
    			postMessage("[System] File "+argv[1]+"\n");
    			return;
    		}
    		if (argv[0].equals("C"))
    		{
    			postMessage("[Ctrl Msg] Server request cache...\n");
    			this.tcpSend(MASTERIP, 11314, "Open|FTP");
    			return;
    		}
    		if (argv[0].equals("ACK") && argv[1].equals("Ready"))
    		{
    			// TODO switch to ftp socket and call sendFile()
    			return;
    		}
    	}
    	else if (argc == 3)
    	{
    		
    		if (argv[0].equals("S"))
    		{
    			if (argv[1].equals("dir"))
    			{
    				String [] res = argv[2].split(";");
    				String ans = "- Directory List ("+pwd+"):\n\n";
    				for (int i = 0; i < res.length; i++)
    					ans += (res[i]+"\t");
    				postMessage(ans+"\n\n");
    				return;
    			}
    			else if (argv[1].equals("file"))
    			{
    				String [] stat = argv[2].split("%");
    	    		String res = "File Status:\n\n";
    	    		res += "- Size: "+stat[0]+"\n";
    	    		res += "- Writing Privilege: "+stat[1]+"\n";
    	    		res += "- Reading Privilege: "+stat[2]+"\n";
    	    		res += "- Executing Privilege: "+stat[3]+"\n";
    	    		res += "- Last Modified Data: "+stat[5]+"\n\n";
    	    		postMessage(res);
    				return;
    			}
    			return;
    		}
    		
    	}
    	else if (argc == 5)
    	{
    		if (argv[0].equals("Open")) // Open|WiFi|srcIP|src|dest  => ACK4|WiFi|srcIP|src|dest
    		{
    			if (setNetworkInterface(argv[1], true))
    			{
    				String outgoingMsg = "ACK4|";
        			outgoingMsg += argv[1]+"|";
        			outgoingMsg += argv[2]+"|";
        			outgoingMsg += argv[3]+"|";
        			outgoingMsg += argv[4];
        			this.tcpSend(MASTERIP, 11314, outgoingMsg);
    			}
    			else
    			{
    				String outgoingMsg = "ACK4|F|";
        			outgoingMsg += argv[2]+"|";
        			outgoingMsg += argv[3]+"|";
        			outgoingMsg += argv[4];
        			this.tcpSend(MASTERIP, 11314, outgoingMsg);
    			}
    			return;
    		}
    		if (argv[0].equals("Try")) // TODO server side does not change right now...
    		{
    			if (argv[1].equals("F"))
    			{
    				postMessage("[System] Target device is not available...\n");
    			}
    			else if (argv[1].equals("WiFi"))
    			{
    				postMessage("[Ctrl Msg] File Retriving Start...\n");
    				this.tcpSend(MASTERIP, 11314, "ACK5");
    				Thread sfr = new Thread(new SocketFileRequest(argv[2], argv[3], argv[4]));
    				sfr.setDaemon(true);
    				sfr.start();
    								
    			}
    			else if (argv[1].equals("BT"))
    			{
    				
    			}
    			return;
    		}
    	}
    }
    
    public void cmdParser(String cmd)
    {
    	String [] argv = cmd.split(" ");
    	int argc = argv.length;
    	if (argc == 1)
    	{
    		if (argv[0].equals("ls"))
    		{
    			this.fs_ls();
    			return;
    		}
    		if (argv[0].equals("tree"))
    		{
    			this.fs_tree();
    			return;
    		}
    		if (argv[0].equals("cd"))
    		{
    			this.fs_cd("");
    			return;
    		}
    		if (argv[0].equals("sensor"))
    		{
    			// Remote case.
    			if (pwd.contains("mobiHome/*") && !pwd.endsWith("mobiHome/"))
    			{
    				this.tcpSend(MASTERIP, 11314, "Sensor");
    				return;
    			}
    			
    			// Local case.
    			this.fs_sensor();
    			return;
    		}
    		// Currently, camera does not support background service.
    		if (argv[0].equals("pic"))
    		{    			
    			cam.takePicture();
    			postMessage("[System Msg] Picture taken.\n");
    			cam.shutdown();
    			return;
    		}
    		if (argv[0].equals("vb"))
    		{
    			// Remote case.
    			if (pwd.contains("mobiHome/*") && !pwd.endsWith("mobiHome/"))
    			{
    				this.tcpSend(MASTERIP, 11314, "VB");
    				return;
    			}
    			
    			// Local case.
    			vbr.vibrate(300);
    			return;
    		}
    		if (argv[0].equals("loc"))
    		{
    			// Remote case.
    			if (pwd.contains("mobiHome/*") && !pwd.endsWith("mobiHome/"))
    			{
    				this.tcpSend(MASTERIP, 11314, "LOC");
    				return;
    			}
    			
    			// Local case.
    			String mloc = "\n----\nLocation:\n";
    			mloc += "Altitude: "+String.valueOf(mobiGPSService.altitude)+"\n";
    			mloc += "Latitude: "+String.valueOf(mobiGPSService.latitude)+"\n";
    			mloc += "Longitude: "+String.valueOf(mobiGPSService.longitude)+"\n";
    			mloc += "Accuracy: "+String.valueOf(mobiGPSService.accuracy)+"\n";
    			postMessage(mloc);
    		}
    	}
    	else if (argc == 2)
    	{
    		if (argv[0].equals("cd"))
    		{
    			this.fs_cd(argv[1]);
    			return;
    		}
    		if (argv[0].equals("mkdir"))
    		{
    			this.fs_mkdir(argv[1]);
    			return;
    		}
    		if (argv[0].equals("rmdir"))
    		{
    			this.fs_rmdir(argv[1]);
    			return;
    		}
    		if (argv[0].equals("stat"))
    		{
    			this.fs_stat(argv[1]);
    			return;
    		}
    		if (argv[0].equals("search"))
    		{
    			this.fs_search(argv[1]);
    			return;
    		}
    		if (argv[0].equals("setpublic"))
    		{
    			// setup sensor public.
    			if (argv[1].equals("sensor"))
    			{
    				this.tcpSend(MASTERIP, 11314, "Sensor|Public");
    				return;
    			}
    			
    			// handle files.
    			this.fs_setPublic(argv[1]);
    			return;
    		}
    		if (argv[0].equals("rm"))
    		{
    			this.fs_rmdir(argv[1]);
    			return;
    		}
    		if (argv[0].equals("slog"))
    		{
    			// Remote request, first go to system server to check access privilege.
    			if (pwd.contains("mobiHome/*") && !pwd.endsWith("mobiHome/"))
    			{
    				this.tcpSend(MASTERIP, 11314, "Sensor|Log|"+argv[1]);
    				return;
    			}
    			
    			// Local case.
    			if (argv[1].equals("on"))
    				sm.isLogging = true;
    			if (argv[1].equals("off"))
    				sm.isLogging = false;
    			if (argv[1].equals("cl"))
    				sm.resetLog();
    			return;
    		}
    		if (argv[0].equals("gps"))
    		{
    			// Remote request, first go to system server to check access privilege.
    			if (pwd.contains("mobiHome/*") && !pwd.endsWith("mobiHome/"))
    			{
    				this.tcpSend(MASTERIP, 11314, "GPS|"+argv[1]);
    				return;
    			}
    			
    			// Local case.
    			if (argv[1].equals("on"))
    				this.gpsOn();
    			if (argv[1].equals("off"))
    				this.gpsOff();
    			return;
    		}
    		if (argv[0].equals("preview"))
    		{
    			// Remote case.
    			if (argv[1].contains("mobiHome/*") || pwd.contains("mobiHome/*"))
    			{
    				this.tcpSend(MASTERIP, 11314, "Preview|"+argv[1]);
    				return;
    			}
    			
    			// Local case.
    			// TODO start another activity to handle file or pic preview
    		}
    	}
    	else if (argc == 3)
    	{
    		if (argv[0].equals("cp"))
    		{
    			this.fs_cp(argv[1], argv[2]);
    			return;
    		}
    		if (argv[0].equals("setprivate")) // setprivate a.txt jliao2,mmc,eyes,mushroom
    		{
    			// setup sensor as private to access list people.
    			if (argv[1].equals("sensor"))
    			{
    				this.tcpSend(MASTERIP, 11314, "Sensor|Private|"+argv[2]);
    				return;
    			}
    			
    			// setup files.
    			this.fs_setPrivate(argv[1], argv[2]);
    			return;
    		}
    	}
    	else
    		postMessage("[System] Command not found.\n");
    	return;
    }
    
    public void postMessage(String str)
    {
    	final String msg = str;
    	handler.post(new Runnable() {
			@Override
			public void run()
			{
				tv.append(msg);
				sv.smoothScrollTo(0, tv.getBottom());
			}
		});
    }
    
    // TODO
    public boolean setNetworkInterface(String netIntf, boolean enable)
    {
    	if (netIntf.toLowerCase() == "wifi")
    	{
    		return true;
    	}
    	else if (netIntf.toLowerCase() == "bluetooth")
    	{
    		return true;
    	}
    	return true;
    }
    
    public boolean fs_pwd()
    {
    	postMessage(pwd+"\n");
    	return true;
    }
    
    public boolean fs_ls()
    {
    	if (!pwd.contains("/mnt/sdcard/mobiHome"))
    	{
    		File f = new File(pwd);
    		File [] children = f.listFiles();
    		String ls = "./\t\t../\t\t";
    		for (File t: children)
    		{
    			if (t.isDirectory())
    				ls += t.getName()+"/\t\t";
    			else
    				ls += t.getName()+"\t\t";
    		}
    		if (pwd.equals("/mnt/sdcard/") || pwd.equals("/mnt/sdcard"))
    			ls += "mobiHome/\n\n";
    		else
    			ls += "\n\n";
    		postMessage(ls);
    	}
    	else
    	{
    		this.tcpSend(MASTERIP, 11314, "R|dir|"+pwd);
    		return true;
    	}
    	return true;
    }
    
    public boolean fs_mkdir(String dir) // right now we don't support mkdir on server side.
    {
    	String fpath = "";
    	String [] path = dir.split("/");
    	if (!path[0].equals("")) // create under current directory
    		fpath += pwd+"/"+dir;
    	else // create from root.
    		fpath = dir;
    	File f = new File(fpath);
    	
    	if (f.mkdir())
    		return true;
    	else
    		return false;
    }
    
    public boolean fs_rmdir(String dir)
    {
    	String fpath = "";
    	String [] path = dir.split("/");
    	if (!path[0].equals("")) // create under current directory
    		fpath += pwd+"/"+dir;
    	else // create from root.
    		fpath = dir;
    	File f = new File(fpath);
    	if (!f.exists())
    	{
    		postMessage("[System] Directory not exist.\n");
    		return false;
    	}
    	
    	DeleteRecursive(f);
    	return true;
    }
    
    private void DeleteRecursive(File dir)
    {
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) 
            {
               File temp =  new File(dir, children[i]);
               if(temp.isDirectory())
            	   DeleteRecursive(temp);
               else
            	   temp.delete();
            }

            dir.delete();
        }
    }
    
    private String shorten(String string)
    {
		String x = "";
		String[] temp = string.split("/");
		int tempLen = temp.length;
		for (int j=1; j < tempLen -1; j++)
			x += "/" + temp[j];
		return x;
	}
    
    public boolean fs_cd(String path)
    {
    	String[] fileds = path.split("/");
    	int filedsLen = fileds.length;
    	
    	for (int i = 0; i < filedsLen; i ++){
    		if (fileds[i].equals(".")){
    			;
    		}
    		else if (fileds[i].equals("..")){
    			pwd = shorten(pwd);
    		}
    		else if (fileds[i].equals(""))
    			pwd = "/mnt/sdcard";
    		else 
    			pwd += "/" + fileds[i];
    	}
      // 	postMessage("pwd is: "+pwd + "\n");
    	return true;
    }
    
    public boolean fs_cp(String src, String dest)
    {
    	String [] srcpath = src.split("/");
    	String [] destpath = dest.split("/");
    	String fsrc = "";
    	String fdest = "";
    	if (srcpath[0].equals(""))
    		fsrc = src;
    	else
    	{
    		if (src.startsWith("/")) fsrc = pwd + src;
    		else fsrc = pwd+"/"+src;
    	}
    		
    	
    	if (destpath[0].equals(""))
    		fdest = dest;
    	else
    	{
    		if (dest.startsWith("/")) fdest = pwd +dest;
    		else fdest = pwd+"/"+dest;
    	}
    	
    	File srcFile = new File (fsrc);
    	if (!srcFile.exists())
    		this.tcpSend(MASTERIP, 11314, "Q|"+fsrc+"|"+fdest);
    	else
    	{
    		try
    		{
    			InputStream is = new FileInputStream(fsrc);
    			OutputStream os = new FileOutputStream(fdest);
    			byte [] data = new byte[is.available()];
    			is.read(data);
    			os.write(data);
    			os.flush();
    			postMessage("[System] copy success...\n");
    		}
    		catch (Exception e)
    		{
    			Log.e("cp", e.toString());
    			postMessage("[System] copy failed...\n");
    		}
    	}
		return true;
    }
    
    private void Tree(String parpath, int n)
    {
    	File par = new File(parpath);
    	if (!par.exists()) return;
    	if (par.isHidden()) return;
    	String line = "";
    	for (int i = 0; i < n; i++) 
    		line += "|\t\t";
    	
    	if (par.isDirectory())
    		line += par.getName()+"/";
    	else
    		line += par.getName();
    	
    	if (par.isFile()) 
    		postMessage(line+"\n");
    	else
    	{
    		File [] childfile = par.listFiles();
    		postMessage(line+"->\n");
    		for (int i = 0; i < childfile.length; i++)
    		{
    			String x = childfile[i].getAbsolutePath();
    			Tree(x, n+1);
    		}
    	}
    		
    }
    
    public boolean fs_tree()
    {
    	postMessage("[System] Tree view:\n\n");
    	Tree("/mnt/sdcard", 0);
    	return true;
    }
    
    public boolean fs_search(String res)
    {
    	this.tcpSend(MASTERIP, 11314, "Search|"+res);
    	return true;
    }
    
    // filename is a full path+name.
    public boolean fs_setPublic(String fname)
    {
    	String [] tname = fname.split("/");
    	String filename = "";
    	if (tname[0].equals(""))
    		filename = fname;
    	else
    		filename = pwd+"/"+fname;
    	String [] name = filename.split("/");
    	int n = name.length;
    	String xname = "";
    	for (int i = n-1; i >= 0; i--)
    		if (name[i] != "")
    		{
    			xname = name[i];
    			break;
    		}
    	String query = "SELECT * from "+dbh.dbName+" WHERE "+dbh.colPath+"='"+filename+"'";
    	Cursor c = dbh.execQuery(query);
    	n = dbh.countRow(c);
    	String [] row = new String[7];
    	if (n == 0) // it is the first time that the file shows up.
    	{
    		//String [] row = new String[6];
    		row[0] = xname;
    		row[1] = filename;
    		row[2] = "1";
    		row[3] = "";
    		row[4] = getLocalFileStatus(filename); // add the file status to this string bucket.
    		row[5] = String.valueOf(timestamp+1); // put it into new round, waiting for update.
    		dbh.insertRow(row);
    		String updateMsg = "Dir|+,"+row[1]+","+row[2]+","+row[3]+","+row[4];
        	this.tcpSend(MASTERIP, 11314, updateMsg);
    	}
    	else if (n == 1) // ONLY ONE.
    	{
    		c.moveToFirst();
    		row = dbh.fetchOneRow(c);
    		row[3] = "1";
    		row[4] = "";
    		row[5] = getLocalFileStatus(filename);
    		row[6] = String.valueOf(timestamp+1);
    		dbh.updateRow(row);
    		String updateMsg = "Dir|+,"+row[2]+","+row[3]+","+row[4]+","+row[5];
        	this.tcpSend(MASTERIP, 11314, updateMsg);
    	}
    	
    	return true;
    }
    
    public boolean fs_setPrivate(String fname, String accesslist) // accesslist should be transform from , to & before storing into database.
    {
    	String [] tname = fname.split("/");
    	String filename = "";
    	if (tname[0].equals(""))
    		filename = fname;
    	else
    		filename = pwd+"/"+fname;
    	String [] name = filename.split("/");
    	
    	accesslist = accesslist.replace(",", "&");
    	
    	int n = name.length;
    	String xname = "";
    	for (int i = n-1; i >= 0; i--)
    		if (name[i] != "")
    		{
    			xname = name[i];
    			break;
    		}
    	String query = "SELECT * from "+dbh.dbName+" WHERE "+dbh.colPath+"='"+filename+"'";
    	Cursor c = dbh.execQuery(query);
    	n = dbh.countRow(c);
    	String [] row = new String[7];
    	if (n == 0) // it is the first time that the file shows up.
    	{
    		//String [] row = new String[6];
    		row[0] = xname;
    		row[1] = filename;
    		row[2] = "0";
    		row[3] = accesslist;
    		row[4] = getLocalFileStatus(filename); // add the file status to this string bucket.
    		row[5] = String.valueOf(timestamp+1); // put it into new round, waiting for update.
    		dbh.insertRow(row);
    		String updateMsg = "Dir|+,"+row[1]+","+row[2]+","+row[3]+","+row[4];
        	this.tcpSend(MASTERIP, 11314, updateMsg);
    	}
    	else if (n == 1) // ONLY ONE.
    	{
    		c.moveToFirst();
    		row = dbh.fetchOneRow(c); // it is row[7].
    		row[3] = "0";
    		row[4] = accesslist;
    		row[5] = getLocalFileStatus(filename); //update.
    		row[6] = String.valueOf(timestamp+1);
    		dbh.updateRow(row);
    		String updateMsg = "Dir|+,"+row[2]+","+row[3]+","+row[4]+","+row[5];
        	this.tcpSend(MASTERIP, 11314, updateMsg);
    	}

    	return true;
    }

    public boolean fs_stat(String filename) 
    {
    	String [] path = filename.split("/");
    	String filepath = "";
    	if (!path[0].equals(""))
    		filepath = pwd+"/"+filename;
    	else
    		filepath = filename;
    	
    	if (filepath.contains("/mnt/sdcard/mobiHome"))
    		this.tcpSend(MASTERIP, 11314, "R|file|"+filepath);
    	else
    	{
    		String str = getLocalFileStatus(filepath);
    		String [] stat = str.split("%");
    		String msg = "File Status:\n\n";
    		msg += "- Size: "+stat[0]+"\n";
    		msg += "- Writing Privilege: "+stat[1]+"\n";
    		msg += "- Reading Privilege: "+stat[2]+"\n";
    		msg += "- Executing Privilege: "+stat[3]+"\n";
    		msg += "- Last Modified Data: "+stat[4]+"\n\n";
    		postMessage(msg);
    	}
    	
    	return true;
    }
    
    public boolean fs_rm(String filename) // ONLY local file are removable.
    {
    	fs_rmdir(filename);
    	return true;
    }
    
    public boolean fs_sensor()
    {
    	String sensor = "\n----";
    	sensor += "\nAccelerometer:\nX-axis: "+String.valueOf(sm.accX)+"\nY-axis: "+String.valueOf(sm.accY)+"\nZ-axis: "+String.valueOf(sm.accZ)+"\n";
    	sensor += "----";
    	sensor += "\nGyroscopemeter:\nX-axis: "+String.valueOf(sm.spin)+"\nY-axis: "+String.valueOf(sm.output)+"\nZ-axis: "+String.valueOf(sm.input)+"\n";
    	sensor += "----";
    	sensor += "\nLightmeter:\nLight: "+String.valueOf(sm.light)+"\n";
    	sensor += "----";
    	sensor += "\nMagnetometer:\nX-axis: "+String.valueOf(sm.magX)+"\nY-axis: "+String.valueOf(sm.magY)+"\nZ-axis: "+String.valueOf(sm.magZ)+"\n";
    	sensor += "----";
    	sensor += "\nOrientationmeter:\nX-axis: "+String.valueOf(sm.rotationX)+"\nY-axis: "+String.valueOf(sm.rotationY)+"\nZ-axis: "+String.valueOf(sm.rotationZ)+"\n";
    	sensor += "----";
    	sensor += "\nProximitymeter:\nProximity: "+String.valueOf(sm.proximity)+"\n";
    	sensor += "----";
    	sensor += "\nGravitymeter:\nX-axis: "+String.valueOf(sm.gravityX)+"\nY-axis: "+String.valueOf(sm.gravityY)+"\nZ-axis: "+String.valueOf(sm.gravityZ)+"\n";
    	sensor += "----";
    	sensor += "\nSoundmeter:\nSound: "+String.valueOf(msound.getSoundPressure())+"\n";
    	postMessage(sensor);
    	return true;
    }
    
    private static final int REQUEST_ENABLE_BT = 3;
    // device information implementation
    public String getDeviceInfo()
    {
    	String info = "Info|";
    	//info += ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId()+";";
    	info += android.os.Build.DEVICE+";";
    	WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	if (wifi == null)
    		info += "0;0;0;";
    	else
    	{
    		info += "1;";
    		WifiInfo wifiInfo = wifi.getConnectionInfo();
        	info += String.valueOf(wifiInfo.getRssi())+";";
        	info += wifiInfo.getMacAddress()+";";
    	}
    	info += "0;0;0;"; // 3G has NAT will block everything, so we abort it.
    	BluetoothAdapter mbta = BluetoothAdapter.getDefaultAdapter();
    	if (mbta == null)
    	{
    		info += "0;null;null;null";
    		if (ServerActivity.blevel == 0)
    			info += "100";
    		else
    			info += String.valueOf(ServerActivity.blevel);
    	}
    	else
    	{
    		// Bluetooth
    		if (!mbta.isEnabled())
    			info += "1;"+"100;potential;";
    		else
    			info += "1;"+"100;"+mbta.getName()+ "+" + mbta.getAddress()+";";
    		
    		// GPS location
    		if (mobiGPSService.altitude != 0 && mobiGPSService.latitude != 0 && mobiGPSService.longitude != 0 && mobiGPSService.accuracy != 0)
    			info += String.valueOf(mobiGPSService.longitude)+","+String.valueOf(mobiGPSService.latitude)+","+String.valueOf(mobiGPSService.altitude)+","+String.valueOf(mobiGPSService.accuracy)+";";
    		else
    			info += "null;";
    		
    		
    		if (ServerActivity.blevel == 0)
    			info += "100";
    		else
    			info += String.valueOf(ServerActivity.blevel);
    	}
    	return info;
    }
    
    public String getSensorInfo()
    {
    	String info = "Sensor|";
    	if (sm.sensorAvaliable(Sensor.TYPE_ACCELEROMETER)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_GRAVITY)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_GYROSCOPE)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_LIGHT)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_LINEAR_ACCELERATION)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_MAGNETIC_FIELD)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_ORIENTATION)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_PRESSURE)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_PROXIMITY)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_ROTATION_VECTOR)) info += "1";  else info += "0";
    	if (sm.sensorAvaliable(Sensor.TYPE_TEMPERATURE)) info += "1";  else info += "0";
    	
    	return info;
    }
    
    public String [] getSensorValue(int type)
    {
    	String [] val = new String[3];
    	switch (type)
    	{
    		case Sensor.TYPE_ACCELEROMETER: 
    			val[0] = String.valueOf(sm.accX);
    			val[1] = String.valueOf(sm.accY);
    			val[2] = String.valueOf(sm.accZ);
    			return val;
    		case Sensor.TYPE_GRAVITY:
    			val[0] = String.valueOf(sm.gravityX);
    			val[1] = String.valueOf(sm.gravityY);
    			val[2] = String.valueOf(sm.gravityZ);
    			return val;
    		case Sensor.TYPE_GYROSCOPE:
    			val[0] = String.valueOf(sm.spin);
    			val[1] = String.valueOf(sm.output);
    			val[2] = String.valueOf(sm.input);
    			return val;
    		case Sensor.TYPE_LIGHT:
    			val[0] = String.valueOf(sm.light);
    			return val;
    		case Sensor.TYPE_MAGNETIC_FIELD:
    			val[0] = String.valueOf(sm.magX);
    			val[1] = String.valueOf(sm.magY);
    			val[2] = String.valueOf(sm.magZ);
    			return val;
    		case Sensor.TYPE_ORIENTATION:
    			val[0] = String.valueOf(sm.rotationX);
    			val[1] = String.valueOf(sm.rotationY);
    			val[2] = String.valueOf(sm.rotationZ);
    			return val;
    		case Sensor.TYPE_PROXIMITY:
    			val[0] = String.valueOf(sm.proximity);
    			return val;
    		default: return val;
    	}
    }
    
    public void gpsOn()
    {
    	startService(new Intent(ServerActivity.this, mobiGPSService.class));
    }
    
    public void gpsOff()
    {
    	stopService(new Intent(ServerActivity.this, mobiGPSService.class));
    }
    
    public String [] getLocationValue()
    {
    	String [] val = new String[4];
    	val[0] = String.valueOf(mobiGPSService.longitude);
    	val[1] = String.valueOf(mobiGPSService.latitude);
    	val[2] = String.valueOf(mobiGPSService.altitude);
    	val[3] = String.valueOf(mobiGPSService.accuracy);
    	return val;
    }
    
    private static boolean bt_ok = false;
    public void onActivityResult(int reqCode, int resCode, Intent data)
    {
    	Log.d("onActivityResult", String.valueOf(resCode));
    	switch (reqCode)
    	{
    		case REQUEST_ENABLE_BT:
    			if (resCode == Activity.RESULT_OK)
    			{
    				postMessage("[System] Bluetooth ready.\n");
    				bt_ok = true;
    			}
    			else
    			{
    				postMessage("[System] Bluetooth denied.\n");
    				bt_ok = false;
    			}
    			break;
    	}
    }
    
    // TODO update resource to server
    public String updateResource()
    {
    	String updates = "";
    	String query = "SELECT * from "+dbh.dbName+" WHERE "+dbh.colModify + ">'"+String.valueOf(this.timestamp)+"'";
    	Cursor c = dbh.execQuery(query);
    	int n = c.getCount();
    	c.moveToFirst();
    	while (n > 0)
    	{
    		String [] row = dbh.fetchOneRow(c);
    		String item = "";
    		if (row[5].equals("D"))
    		{
    			item += "-,";
    			item += row[2]+",,,;";
    		}
    		else
    		{
    			item += "+,";
    			item += row[2]+",";
        		item += row[3]+",";
        		item += row[4]+",";
        		item += row[5]+";";
    		}
    		updates += item;
    		--n;
    	}
    	this.timestamp++; // timestamp increase to new current time.
    	return updates;
    	//return "+,/mouse/a.txt,0,A&B&C,excellent;+,/mouse/jliao2/b.txt,1,,good;+,/mushroom/c.txt,0,jliao2&B,bad;+,/mouse/d.txt,0,A&B&C,excellent;";
    }
    
    public String getLocalFileStatus(String path)
    {
    	File f = new File(path);
    	if (!f.exists()) return null;
    	String res = "";
    	int size = 0;
    	try
    	{
    		InputStream is = new FileInputStream(f);
    		size = is.available();
    	}
    	catch (Exception e)
    	{
    		Log.e("fstream", e.toString());
    	}
    	res += String.valueOf(size/1024)+"KB"+"%";
    	res += (f.canWrite() ? "w":"nw") + "%";
    	res += (f.canRead() ? "r":"nr") + "%";
    	res += (f.canExecute() ? "x":"nx") + "%";
    	//res += f.getParent() + "%";
    	Date d = new Date(f.lastModified());
    	res += d.toGMTString();
    	postMessage("[Debug] "+res+"\n");
    	return res;
    }
    
    // MD5 encryption
    public String md5(String s) 
    {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            
            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    
    /*
     *  Functions about Bluetooth File Transfer.
     */
    public boolean setupBluetoothDevice()
    {
    	BluetoothAdapter mbta = BluetoothAdapter.getDefaultAdapter();
        if (mbta != null) // If adapter available, check user to open it right now.
        {
	        if (!mbta.isEnabled())
			{
				Intent enBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enBtIntent, REQUEST_ENABLE_BT);
			}
	        if (!mbta.isDiscovering())
	        {
	        	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	            startActivity(discoverableIntent);
	        }
	        return true;
    	}
    	else
    	{
    		postMessage("[System] No hardware support.\n");
    		return false;
    	}
    }
    
    public BluetoothDevice searchDevice(String name)
    {
    	String [] id = name.split("+");
    	BluetoothAdapter mbta = BluetoothAdapter.getDefaultAdapter();
    	BluetoothDevice remote = mbta.getRemoteDevice(id[1]);
    	if (remote == null) return null;
    	return remote;
    }
    
    public boolean connect(BluetoothDevice d, String mode)
    {
    	//BluetoothSocket bts = new 
    	return true;
    }
    
    public class BluetoothFileThread implements Runnable
    {
    	public void run()
    	{
    		
    	}
    }
    
    public class SensorMaintenance implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	private boolean isLogging;
    	
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
    	
    	// Sensor Filenames
    	private final String senAccelerometer = "Accelerometer.sensor";
    	private final String senLightmeter = "Lightmeter.sensor";
    	private final String senMagnetometer = "Magnetometer.sensor";
    	private final String senOrientationmeter = "Orientationmeter.sensor";
    	private final String senProximitymeter = "Proximitymeter.sensor";
    	private final String senGravitymeter = "Gravitymeter.sensor";
    	private final String senGyroscopemeter = "Gyroscopemeter.sensor";
    	
    	private FileOutputStream accfos;
    	private FileOutputStream lightfos;
    	private FileOutputStream magfos;
    	private FileOutputStream orifos;
    	private FileOutputStream proxfos;
    	private FileOutputStream gravfos;
    	private FileOutputStream gyrfos;
    	
    	public SensorMaintenance(Activity act)
    	{
    		this.mSensorManager = (SensorManager) act.getSystemService(SENSOR_SERVICE);
    		this.Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    		this.Gyroscopemeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    		this.Lightmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    		this.Magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    		this.Orientationmeter = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		this.Proximitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    		this.Gravitymeter = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    	}
    	
    	public boolean sensorAvaliable(int s)
    	{
    		switch (s)
    		{
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
    	
    	public void startLogging()
    	{
    		this.isLogging = true;
    	}
    	
    	public void stopLogging()
    	{
    		this.isLogging = false;
    	}
    	
    	public void resetLog()
    	{
    		File accf = new File("/mnt/sdcard/sensor/"+this.senAccelerometer);
    		accf.deleteOnExit();
    		File lightf = new File("/mnt/sdcard/sensor/"+this.senLightmeter);
    		lightf.deleteOnExit();
    		File magf = new File("/mnt/sdcard/sensor/"+this.senMagnetometer);
    		magf.deleteOnExit();
    		File orif = new File("/mnt/sdcard/sensor/"+this.senOrientationmeter);
    		orif.deleteOnExit();
    		File proxf = new File("/mnt/sdcard/sensor/"+this.senProximitymeter);
    		proxf.deleteOnExit();
    		File gravf = new File("/mnt/sdcard/sensor/"+this.senGravitymeter);
    		gravf.deleteOnExit();
    		File gyrf = new File("/mnt/sdcard/sensor/"+this.senGyroscopemeter);
    		gyrf.deleteOnExit();
    	}
    	
    	// Start sensing
    	public void startSensor()
    	{
    		mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Gyroscopemeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Lightmeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Magnetometer, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Orientationmeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Proximitymeter, SensorManager.SENSOR_DELAY_UI);
    		mSensorManager.registerListener(this, this.Gravitymeter, SensorManager.SENSOR_DELAY_UI);
    		
    		if (!isLogging) return;
    		try
    		{
    			File senDir = new File("/mnt/sdcard/sensor/");
    			if (!senDir.exists()) senDir.mkdir();
    			
    			File accf = new File("/mnt/sdcard/sensor/"+this.senAccelerometer);
    			if (!accf.exists()) accf.createNewFile();
    			accfos = new FileOutputStream(accf, false);
    			
    			File lightf = new File("/mnt/sdcard/sensor/"+this.senLightmeter);
    			if (!lightf.exists()) lightf.createNewFile();
    			lightfos = new FileOutputStream(lightf, false);
    			
    			File magf = new File("/mnt/sdcard/sensor/"+this.senMagnetometer);
    			if (!magf.exists()) magf.createNewFile();
    			magfos = new FileOutputStream(magf, false);
    			
    			File orif = new File("/mnt/sdcard/sensor/"+this.senOrientationmeter);
    			if (!orif.exists()) orif.createNewFile();
    			orifos = new FileOutputStream(orif, false);
    			
    			File proxf = new File("/mnt/sdcard/sensor/"+this.senProximitymeter);
    			if (!proxf.exists()) proxf.createNewFile();
    			proxfos = new FileOutputStream(proxf, false);
    			
    			File gravf = new File("/mnt/sdcard/sensor/"+this.senGravitymeter);
    			if (!gravf.exists()) gravf.createNewFile();
    			gravfos = new FileOutputStream(gravf, false);
    			
    			File gyrf = new File("/mnt/sdcard/sensor/"+this.senGyroscopemeter);
    			if (!gyrf.exists()) gyrf.createNewFile();
    			gyrfos = new FileOutputStream(gyrf, false);
    			
    		} catch (Exception e)
    		{
    			Log.e("Sensor", e.getMessage());
    			postMessage("[System Msg] Sensor File Create Failed.\n");
    		}
    		
    	}
    	
    	// Stop sensing
    	public void stopSensor()
    	{
    		mSensorManager.unregisterListener(this, this.Accelerometer);
    		mSensorManager.unregisterListener(this, this.Gyroscopemeter);
    		mSensorManager.unregisterListener(this, this.Lightmeter);
    		mSensorManager.unregisterListener(this, this.Magnetometer);
    		mSensorManager.unregisterListener(this, this.Orientationmeter);
    		mSensorManager.unregisterListener(this, this.Proximitymeter);
    		mSensorManager.unregisterListener(this, this.Gravitymeter);
    		
    		if (!isLogging) return;
    		try
    		{
    			accfos.close();
    			lightfos.close();
    			magfos.close();
    			orifos.close();
    			proxfos.close();
    			gravfos.close();
    			gyrfos.close();
    		} catch (Exception e)
    		{
    			Log.e("Sensor", e.getMessage());
    			postMessage("[System Msg] Sensor File Close Failed.\n");
    		}
    		
    	}
    	
    	@Override
        public void onSensorChanged(SensorEvent event) {
    		
    		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    		{
    			this.accX = event.values[0];
    			this.accY = event.values[1];
    			this.accZ = event.values[2];
    			
    			if (isLogging)
    			{
    				try
	    			{
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.accX) + ";" + String.valueOf(this.accY) + ";" + String.valueOf(this.accZ);
	    				byte [] buf = str.getBytes();
	    				accfos.write(buf);
	    				accfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Accelerometer Flush Failed.\n");
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
    		{
    			this.spin = event.values[0];
    			this.output = event.values[1];
    			this.input = event.values[2];
    			
    			if (isLogging)
    			{
	    			try
	    			{
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.spin) + ";" + String.valueOf(this.output) + ";" + String.valueOf(this.input);
	    				byte [] buf = str.getBytes();
	    				gyrfos.write(buf);
	    				gyrfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Gyroscopemeter Flush Failed.\n");
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_LIGHT)
    		{
    			this.light = event.values[0];
    			
    			if (isLogging)
    			{
    				try
	    			{
	    				String str = String.valueOf(this.light);
	    				byte [] buf = str.getBytes();
	    				lightfos.write(buf);
	    				lightfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Lightmeter Flush Failed.\n");
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
    		{
    			this.magX = event.values[0];
    			this.magY = event.values[1];
    			this.magZ = event.values[2];
    			
    			if (isLogging)
    			{
    				try
	    			{
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.magX) + ";" + String.valueOf(this.magY) + ";" + String.valueOf(this.magZ);
	    				byte [] buf = str.getBytes();
	    				magfos.write(buf);
	    				magfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Magnetometer Flush Failed.\n");
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
    		{
    			this.rotationX = event.values[0];
    			this.rotationY = event.values[1];
    			this.rotationZ = event.values[2];
    			
    			if (isLogging)
    			{	
    				try
	    			{
    					Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.rotationX) + ";" + String.valueOf(this.rotationY) + ";" + String.valueOf(this.rotationZ);
	    				byte [] buf = str.getBytes();
	    				orifos.write(buf);
	    				orifos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Orienationmeter Flush Failed.\n");
	    			}
    			}
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY)
    		{
    			this.proximity = event.values[0];
    			
    			if (isLogging)
    			{
	    			try
	    			{
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.proximity);
	    				byte [] buf = str.getBytes();
	    				proxfos.write(buf);
	    				proxfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Proixmitymeter Flush Failed.\n");
	    			}
    			}
    			
    			return;
    		}
    		if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
    		{
    			this.gravityX = event.values[0];
    			this.gravityY = event.values[1];
    			this.gravityZ = event.values[2];
    			
    			if (isLogging)
    			{
	    			try
	    			{
	    				Date d = new Date();
	    				String str = d.toGMTString()+":"+String.valueOf(this.gravityX) + ";" + String.valueOf(this.gravityY) + ";" + String.valueOf(this.gravityZ);
	    				byte [] buf = str.getBytes();
	    				gravfos.write(buf);
	    				gravfos.flush();
	    			}
	    			catch(Exception e)
	    			{
	    				Log.e("Sensor", e.getMessage());
	    				postMessage("[System Msg] Gravitymeter Flush Failed.\n");
	    			}
    			}
    			
    			return;
    		}
    	}
    	
    	@Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
}












