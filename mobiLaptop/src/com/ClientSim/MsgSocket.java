package com.ClientSim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;



public class MsgSocket implements Runnable {
	
	public ServerSocket tcpServer;
	public String LOCALLISTENIP = "192.168.1.100"; 
	
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
							incomingMsg += line;
						}
						postMessage("[Ctrl Msg] TCP Packet Receive: "+incomingMsg+"\n");
						MsgParser mp = new MsgParser();
						mp.msgParser(incomingMsg);
						
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
	
	public static boolean tcpSend(String IP, int port, String msg)
    {
    	try
    	{
    		InetAddress remoteAddr = InetAddress.getByName(IP);
    		Socket socket = new Socket(remoteAddr, port);
    		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    		out.println(msg);
    		socket.close();
    		postMessage("Msg send done.\n"+msg+"\n");
    		return true;
    	}
    	catch (Exception e)
    	{
    		postMessage("Msg send failed.\n"+msg+"\n");
    		return false;
    	}
    }
}
