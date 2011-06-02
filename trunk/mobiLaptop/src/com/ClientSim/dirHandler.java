package com.ClientSim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;


public class dirHandler {
	
	public String pwd = "C:/cygwin/home/jliao2/mnt/sdcard";
	public static String home = "C:/cygwin/home/jliao2/mnt/sdcard";
	public static String header = "C:/cygwin/home/jliao2";
	
	public boolean fs_pwd()
    {
    	postMessage(pwd+"\n");
    	return true;
    }
    
    // TODO
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
    		if (pwd.equals(home) || pwd.equals(home+"/"))
    			ls += "mobiHome/\n\n";
    		else
    			ls += "\n\n";
    		postMessage(ls);
    	}
    	else
    	{
    		MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, "R|dir|"+pwd.substring(21));
    		return true;
    	}
    	return true;
    }
    
    // TODO
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
    
    // TODO
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
		for (int j=0; j < tempLen -1; j++)
		{	
			if (temp[j] == "") continue;
			if (j == 0) x += temp[j];
			else x += "/" + temp[j];
		}
		return x;
	}
    
    
    // TODO
    public boolean fs_cd(String path)
    {
    	String[] fileds = path.split("/");
    	int filedsLen = fileds.length;
    	
    	for (int i = 0; i < filedsLen; i ++){
    		if (fileds[i].equals("")) continue;
    		if (fileds[i].equals(".")){
    			;
    		}
    		else if (fileds[i].equals("..")){
    			pwd = shorten(pwd);
    		}
    		else if (fileds[i].equals(""))
    			pwd = home;
    		else 
    			pwd += "/" + fileds[i];
    	}
      // 	postMessage("pwd is: "+pwd + "\n");
    	return true;
    }
    
    // TODO
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
    		if (src.startsWith("/")) fsrc = pwd+src;
    		else fsrc = pwd + "/" + src;
    	}
    	
    	if (destpath[0].equals("C:"))
    		fdest = dest;
    	else
    	{
    		if (dest.startsWith("/")) fdest = header+dest;
    		else fdest = header + "/" + dest;
    	}
    		
    	
    	File srcFile = new File (fsrc);
    	if (!srcFile.exists())
    		MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, "Q|"+fsrc.substring(21)+"|"+fdest.substring(21));
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
    
    
    // TODO
    public boolean fs_tree()
    {
    	postMessage("[System] Tree view:\n\n");
    	Tree("/mnt/sdcard", 0);
    	return true;
    }
    
    // TODO
    public boolean fs_search(String res)
    {
    	MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, "Search|"+res);
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
    	
    	if (filename.contains("/mnt/sdcard/mobiHome"))
    	{
    		postMessage("[System] You are not in your home directory.\n");
    		return false;
    	}
    	
    	String [] name = filename.split("/");
    	int n = name.length;
    	String xname = "";
    	for (int i = n-1; i >= 0; i--)
    		if (name[i] != "")
    		{
    			xname = name[i];
    			break;
    		}
    	String [] row = new String[7];
   		row[0] = xname;
    	row[1] = filename.substring(21);
    	row[2] = "1";
    	row[3] = "";
    	row[4] = getLocalFileStatus(filename); // add the file status to this string bucket.
    	//row[5] = String.valueOf(timestamp+1); // put it into new round, waiting for update.
    	
    	String updateMsg = "Dir|+,"+row[1]+","+row[2]+","+row[3]+","+row[4];
        MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, updateMsg);
    	
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
    	if (filename.contains("/mnt/sdcard/mobiHome"))
    	{
    		postMessage("[System] You are not in your home directory.\n");
    		return false;
    	}
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
    	
    	String [] row = new String[7];
    		
    	row[0] = xname;
    	row[1] = filename.substring(21);
    	row[2] = "0";
    	row[3] = accesslist;
    	row[4] = getLocalFileStatus(filename); // add the file status to this string bucket.
    	//row[5] = String.valueOf(timestamp+1); // put it into new round, waiting for update.
    	String updateMsg = "Dir|+,"+row[1]+","+row[2]+","+row[3]+","+row[4];
        MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, updateMsg);

    	return true;
    }
    // TODO
    
    // TODO
    public boolean fs_stat(String filename) 
    {
    	String [] path = filename.split("/");
    	String filepath = "";
    	if (!path[0].equals(""))
    		filepath = pwd+"/"+filename;
    	else
    		filepath = filename;
    	
    	if (filepath.contains("/mnt/sdcard/mobiHome"))
    		MsgSocket.tcpSend(MsgParser.MASTERIP, 11314, "R|file|"+filepath);
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
    
    // TODO
    public boolean fs_rm(String filename) // ONLY local file are removable.
    {
    	return true;
    }
    
    public void postMessage(String str)
    {
    	System.out.append(str);
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
    		postMessage("Read file status failed.\n");
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
}
