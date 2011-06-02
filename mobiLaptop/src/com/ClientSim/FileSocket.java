package com.ClientSim;

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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class FileSocket implements Runnable {

	public ServerSocket scfServer;
	public String LOCALLISTENIP = "192.168.1.100"; 
	
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
							String fullpath = dirHandler.header + data[1];
							postMessage("[Ctrl Msg] File Request @ "+fullpath+"\n");
							File f = new File(fullpath);
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
		}
	}
	
	public static void postMessage(String msg)
	{
		System.out.append(msg);
	}
	
	public String getLocalIPAddr()
	{
		try
		{
			InetAddress addr = InetAddress.getLocalHost();
			if (!addr.isLoopbackAddress())
				return addr.toString();
		}
		catch (Exception e)
		{
			postMessage("Get Local IP Failed.\n");
		}
		return null;
	}
	
	public static boolean SocketFileRequest(String src, String ip, String dest) // raw_src and dest should be full path.
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
    		String fullpath = dirHandler.header+dest;
    		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(fullpath), true)); // append.
    		
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
    		return true;
    	}
    	catch (Exception e)
    	{
    		postMessage("[System] File request failed...\nPlease try again!!\n");
    		return false;
    	}
    }
}
