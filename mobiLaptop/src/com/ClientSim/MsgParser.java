package com.ClientSim;

public class MsgParser {

	public static String MASTERIP = "160.36.26.199";
	
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
    		/*if (argv[0].equals("HB"))
    		{
    			String outgoingMsg = "Dir|";
    			outgoingMsg += updateResource();
    			// TODO clean database
    			this.udpSend(MASTERIP, 11315, outgoingMsg);
    			return;
    		}*/
    		if (argv[0].equals("Sync"))
    		{
    			MsgSocket.tcpSend(MASTERIP, 11314, "Join");
    			postMessage("[System] IP Address update, re-sync...\n");
    			return;
    		}
    	}
    	else if (argc == 2)
    	{
    		if (argv[0].equals("ACK0") && argv[1].equals("Info"))
    		{
    			String outgoingMsg = "Info|mouse;1;-74;abc;0;0;null;0;0;null;0;83";
    			//String outgoingMsg = getDeviceInfo();
    			MsgSocket.tcpSend(MASTERIP, 11314, outgoingMsg);
    			return;
    		}
    		if (argv[0].equals("ACK1") && argv[1].equals("dirUpdate"))
    		{
    			String outgoingMsg = "Dir|";
    			//outgoingMsg += updateResource();
    			// TODO clean database

    			MsgSocket.tcpSend(MASTERIP, 11314, outgoingMsg);
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
    			MsgSocket.tcpSend(MASTERIP, 11314, "Open|FTP");
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
    				String ans = "- Directory List:\n\n";
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
        			MsgSocket.tcpSend(MASTERIP, 11314, outgoingMsg);
    			}
    			else
    			{
    				String outgoingMsg = "ACK4|F|";
        			outgoingMsg += argv[2]+"|";
        			outgoingMsg += argv[3]+"|";
        			outgoingMsg += argv[4];
        			MsgSocket.tcpSend(MASTERIP, 11314, outgoingMsg);
    			}
    			return;
    		}
    		if (argv[0].equals("Try")) 
    		{
    			if (argv[1].equals("F"))
    			{
    				postMessage("[System] Target device is not available...\n");
    			}
    			else if (argv[1].equals("WiFi"))
    			{
    				postMessage("[Ctrl Msg] File Retriving Start...\n");
    				MsgSocket.tcpSend(MASTERIP, 11314, "ACK5");
    				FileSocket.SocketFileRequest(argv[3], argv[2], argv[4]);				
    			}
    			return;
    		}
    	}
    }
    	
    public void postMessage(String msg)
    {
    	System.out.append(msg);
    }
    
    public boolean setNetworkInterface(String Interface, boolean status)
    {
    	return true;
    }
}
