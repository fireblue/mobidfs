package com.ClientSim;

public class cmdParser {
	
	public dirHandler dh = new dirHandler();
	
	public void cmdParser(String cmd)
    {
    	String [] argv = cmd.split(" ");
    	int argc = argv.length;
    	if (argc == 1)
    	{
    		if (argv[0].equals("ls"))
    		{
    			dh.fs_ls();
    			return;
    		}
    		if (argv[0].equals("tree"))
    		{
    			dh.fs_tree();
    			return;
    		}
    		if (argv[0].equals("cd"))
    		{
    			dh.fs_cd("");
    			return;
    		}
    		if (argv[0].equals("pwd"))
    		{
    			dh.fs_pwd();
    			return;
    		}
    	}
    	else if (argc == 2)
    	{
    		if (argv[0].equals("cd"))
    		{
    			dh.fs_cd(argv[1]);
    			return;
    		}
    		if (argv[0].equals("mkdir"))
    		{
    			dh.fs_mkdir(argv[1]);
    			return;
    		}
    		if (argv[0].equals("rmdir"))
    		{
    			dh.fs_rmdir(argv[1]);
    			return;
    		}
    		if (argv[0].equals("stat"))
    		{
    			dh.fs_stat(argv[1]);
    			return;
    		}
    		if (argv[0].equals("search"))
    		{
    			dh.fs_search(argv[1]);
    			return;
    		}
    		if (argv[0].equals("setpublic"))
    		{
    			dh.fs_setPublic(argv[1]);
    			return;
    		}
    		if (argv[0].equals("rm"))
    		{
    			dh.fs_rmdir(argv[1]);
    			return;
    		}
    	}
    	else if (argc == 3)
    	{
    		if (argv[0].equals("cp"))
    		{
    			dh.fs_cp(argv[1], argv[2]);
    			return;
    		}
    		if (argv[0].equals("setprivate")) // setprivate a.txt jliao2,mmc,eyes,mushroom
    		{
    			dh.fs_setPrivate(argv[1], argv[2]);
    			return;
    		}
    	}
    	else
    		postMessage("[System] Command not found.\n");
    	return;
    }
	
	public void postMessage(String msg)
    {
    	System.out.append(msg);
    }
}
