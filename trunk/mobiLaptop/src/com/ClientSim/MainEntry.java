package com.ClientSim;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class MainEntry {

	/**
	 * @param args
	 */
	private static Thread msglist = new Thread(new MsgSocket());
	private static Thread flist = new Thread(new FileSocket());
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		msglist.setDaemon(true);
		flist.setDaemon(true);
		msglist.start();
		flist.start();
		
		MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, "Join");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		cmdParser cp = new cmdParser();
		
		while (true)
		{
			System.out.print("$");
			try
			{
				String cmd = br.readLine();
				if (cmd.equals("exit") || cmd.equals("quit")) break;
				cp.cmdParser(cmd);
			}
			catch (Exception e)
			{
				System.out.println("Read command error.\n");
			}
		}
	}

}
