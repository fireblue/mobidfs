import Database;
import socket;

def Optimize(ip, src, dest): # return the sorted resource list based on src.

##    # Because we only have ONE phone. Cache method is the ONLY choice.
##    xp = src.split("/");
##    serverPath = "/"+xp[4]+"/"+xp[len(xp)-1];
##    msg = "Try|WiFi|160.36.26.199|"+serverPath+"|"+dest;        
##    try:
##        socket.setdefaulttimeout(10);
##        sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
##        sck.connect((ip, 11315));
##        sck.send(msg); # send command back to C1
##        print "Analyze message: %s" % msg;
##    except socket.error, socket.timeout:
##        print "Cannot connect C1";
##    return;
##    #-----------------------------------------------------------------
      
    res = src.split("/");
    usr = res[4];
    # file path transfer.
    f = "";
    for i in range(1, len(res)):
        if i in [3, 4]: continue;
        f += "/"+res[i];

    # select file
    conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
    query = "SELECT * FROM `source` WHERE `DIR`='"+f+"' AND `USER`='"+usr+"';";
    res = Database.matrixReadDB(conn, query);

    if len(res) == 0:
        print "No file retrieve."
    else:
        query = "SELECT * FROM `user` WHERE `NAME`='"+usr+"';";
        res = Database.matrixReadDB(conn, query);

        if res[0][1] == "1":
            print "branch 1";
            # if cached, find the nearest way.
            ip_s1 = ip.split(".");
            ip_s2 = res[0][2].split(".");
            ip_s3 = ("160.36.26.199").split(".");
            cnt1 = 0;
            cnt2 = 0;
            for i in range(4):
                cnt1 = cnt1*256 + int(abs(ip_s1[i]-ip_s2[i]));
                cnt2 = cnt2*256 + int(abs(ip_s1[i]-ip_s3[i]));

            # choose mobile client then send contact info to the other mobile client    
            if cnt1 < cnt2: 
                if cnt1 < 100 and res[0][11] in ["1", "potential"]:
                    msg = msg = "Open|BT|"+ip+"|"+f+"|"+dest;
                else:
                    msg = "Open|WiFi|"+ip+"|"+f+"|"+dest;
                try:
                    socket.setdefaulttimeout(10);
                    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                    sck.connect((res[0][2], 11314));
                    sck.send(msg);
                    print "Send out prob packet to C2 @ %s" % res[0][2];
                    print "Analyze message: %s" % msg;
                    return;
                except socket.error, socket.timeout:
                    print "Prob packet to C2 %s failed. Direct connect to server." % res[0][2];

            # choose server and src path will change a little.
            xp = src.split("/");
            serverPath = "/"+xp[4]+"/"+xp[len(xp)-1];
            msg = "Try|WiFi|160.36.26.199|"+serverPath+"|"+dest;        
            try:
                socket.setdefaulttimeout(10);
                sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                sck.connect((ip, 11315));
                sck.send(msg); # send command back to C1
                print "Analyze message: %s" % msg;
            except socket.error, socket.timeout:
                print "Cannot connect C1";
        else:
            print "branch 2.";
            ip_s1 = ip.split(".");
            ip_s2 = res[0][2].split(".");

##            for i in range(4):
##                cnt1 = cnt1*256 + int(abs(int(ip_s1[i])-int(ip_s2[i])));
##
##            # decide which way to go.    
##            if cnt1 < 100 and res[0][11] in ["1", "potential"]:
##                msg = "Open|BT|"+ip+"|"+f+"|"+dest;
##            else:
##                msg = "Open|WiFi|"+ip+"|"+f+"|"+dest;

            msg = "Open|WiFi|"+ip+"|"+f+"|"+dest;
            # sending out packet.
            try:
                socket.setdefaulttimeout(10);
                sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                sck.connect((res[0][2], 11314));
                sck.send(msg);
                print "Send out prob packet to C2 @ %s" % res[0][2];
                print "Analyze message: %s" % msg;
                
            except socket.error, socket.timeout:
                print "Prob packet to C2 %s failed." % res[0][2];
            
            
    
