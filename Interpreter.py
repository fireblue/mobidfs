import Database;
import Analyzer;
import socket;
import os;

def parseCommand(message, clientAddr):
    #return "***Welcome***\nNow server is under construction.\n\nPlease contact jliao2@utk.edu for more information";
    argv = message.split("|");
    argc = len(argv);
    #-----------------------check wrong command------------------------
    if argv[0] not in ["Join", "ACK0", "ACK1", "ACK2", "ACK4", "ACK5", "ACK6", "Search", "Info", "Dir", "FIN", "R", "Open", "D", "Q"]:
        return "Wrong command, please contact jliao2@utk.edu for more info.";
    #-------------------------------end--------------------------------
    #--------------------------------------------------------------------
    if argc == 1:
        
        if argv[0] == "Join":
            return "ACK0|Info";
        if argv[0] == "Exit":
            # update client
            pass;
        if argv[0] == "ACK5":
            print "Client start sharing file."

    #--------------------------------------------------------------------    
    elif argc == 2:
        
        if argv[0] == "Info":
            conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
            deviceInfo = argv[1].split(";");
            if len(deviceInfo) == 0:
                query = "UPDATE `user` SET `ALIVE`='1' WHERE `IP`='"+str(clientAddr[0])+"';";
                Database.updateDB(conn, query);
                Database.closeDB(conn);
                return "";
            #query = "SELECT * FROM `user` WHERE `IP`='"+str(clientAddr[0])+"' AND `NAME`='"+str(deviceInfo[0])+"';";
            query = "SELECT * FROM `user` WHERE `IP`='"+str(clientAddr[0])+"';";

            # check if user exist in database
            ret = Database.matrixReadDB(conn, query);
            nn = len(ret);
            if nn > 0:
                print "Find old user, update....";
                query = "UPDATE `user` SET ";
                query += "`NAME`='"+str(deviceInfo[0])+"',";
                query += "`WIFI`='"+str(deviceInfo[1])+"',";
                query += "`QUAL_WIFI`='"+str(deviceInfo[2])+"',";
                query += "`NAME_WIFI`='"+str(deviceInfo[3])+"',";
                query += "`3G`='"+str(deviceInfo[4])+"',";
                query += "`QUAL_3G`='"+str(deviceInfo[5])+"',";
                query += "`NAME_3G`='"+str(deviceInfo[6])+"',";
                query += "`BLUETOOTH`='"+str(deviceInfo[7])+"',";
                query += "`QUAL_BLUETOOTH`='"+str(deviceInfo[8])+"',";
                query += "`NAME_BLUETOOTH`='"+str(deviceInfo[9])+"',";
                query += "`GPS`='"+str(deviceInfo[10])+"',";
                query += "`BATTERY`='"+str(deviceInfo[11])+"',";
                query += "`ALIVE`='1' WHERE `IP`='"+str(clientAddr[0])+"';";
                Database.updateDB(conn, query);
                home = "C:/cygwin/home/jliao2/mobiHome";
                if not os.path.exists(home+"/"+deviceInfo[0]):
                    os.mkdir(home+"/"+deviceInfo[0]);
                return "ACK1|dirUpdate";

            # Count users in the database
            query = "SELECT * FROM `user`";
            ret = Database.matrixReadDB(conn, query);
            nn = len(ret);
            
            # user does not exist in database
            print "Adding New User";
            query = "INSERT INTO `user` (`ID`, `CACHE`, `IP`, `PORT`, `NAME`, `WIFI`, `QUAL_WIFI`, `NAME_WIFI`, `3G`, `QUAL_3G`, `NAME_3G`, `BLUETOOTH`, `QUAL_BLUETOOTH`, `NAME_BLUETOOTH`, `GPS`, `BATTERY`, `ALIVE`) VALUES (";
            query += "'"+str(nn)+"', '0',";
            query += "'"+str(clientAddr[0])+"', ";
            query += "'11314', ";
            for i in deviceInfo:
                query += "'"+(str(i)+"', ");
            query += "'1');";
            Database.updateDB(conn, query);
            Database.closeDB(conn);

            # Create user file in server-side.
            home = "C:/cygwin/home/jliao2/mobiHome";
            if not os.path.exists(home+deviceInfo[0]):
                os.mkdir(home+deviceInfo[0]);
            return "ACK1|dirUpdate";
        
        if argv[0] == "Dir":
            dirUpdate = argv[1].split(";");
            conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");

            # put client back to alive status
            query = "UPDATE `user` SET `ALIVE`='1' WHERE `IP`='"+str(clientAddr[0])+"'";
            Database.updateDB(conn, query);

            for item in dirUpdate: # commit each update in the message
                col = item.split(",");
                if col[0] == "+": # insert or modify
                    query = "SELECT * FROM `source` WHERE `DIR`='"+str(col[1])+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) > 0: # found and modify
                        print "Modify source..."
                        query = "UPDATE `source` SET `PUBLIC`='"+str(col[2])+"', ";
                        query += "`ACCESSLIST`='"+str(col[3])+"', ";
                        query += "`STATUS`='"+str(col[4])+"' WHERE `DIR`='"+str(col[1])+"';";
                        Database.updateDB(conn, query);
                    else: # not found then insert
                        print "Insert new source..."
                        query = "SELECT * FROM `user` WHERE `IP`='"+str(clientAddr[0])+"';";
                        ret = Database.matrixReadDB(conn, query);
                        if len(ret) == 0:
                            print "Can't found user!!!";
                            return "";
                        else:
                            usr = ret[0][4];
                        fname = col[1].split("/");
                        nn = len(fname);
                        query = "INSERT INTO `source` (`FILENAME`, `DIR`, `USER`, `PUBLIC`, `ACCESSLIST`, `STATUS`, `SERVER_CACHE`, `HOT`) VALUES ('";
                        query += str(fname[nn-1])+"', '"+str(col[1])+"', '"+usr+"', '";
                        query += str(col[2])+"', '"+str(col[3])+"', '"+str(col[4])+"', '0', '0');";
                        Database.updateDB(conn, query);
                elif col[0] == "-": # delete
                    print "Delete source..."
                    query = "DELETE FROM `source` WHERE `DIR`='"+str(col[1])+"';";
                    Database.updateDB(conn, query);
            Database.closeDB(conn);
            return "ACK2";
        
        if argv[0] == "Search":
            conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
            query = "SELECT * FROM `source` WHERE `FILENAME`='"+str(argv[1])+"';";
            res = Database.matrixReadDB(conn, query);
            if len(res) == 0:
                return "ACK3|Failed";
            msg = "ACK3|";
            for d in res:
                line = "/mnt/sdcard/mobiHome/"+d[2];
                xt = d[1].split("/");
                for i in range(3, len(xt)):
                    line += ("/"+xt[i]);
                msg += line+";";
            Database.closeDB(conn);
            return msg;
        
    #------------------------------------------------------------------------------------
    elif argc == 3:

        if argv[0] == "FIN":
            #return "***Welcome***\nNow server is under construction.\n\nPlease contact jliao2@utk.edu for more information";
            if argv[1] == "160.36.26.199":
                return "";
            try:
                socket.setdefaulttimeout(1);
                sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                sck.connect((argv[1],11314));
                sck.send("Close|"+str(argv[2]));
                return "";
            except socket.error, socket.timeout:
                print "Close C2 timeout.";
                return "";
        
        if argv[0] == "R": # ls or stat function response. this will use the original tcp connection.

            # find out requst user to check access list.
            conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
            query = "SELECT * FROM `user` WHERE `IP`='"+str(clientAddr[0])+"';"
            print query;
            res = Database.matrixReadDB(conn, query);
            if len(res) > 0:
                usr = res[0][4];
            else:
                print "**Request client never exist in list, request to re-sync.";
                return "Sync";

            if argv[1] == "dir":

                # just in the home directory, return users.
                print argv[2];
                if argv[2] == "/mnt/sdcard/mobiHome":
                    query = "SELECT * FROM `user` WHERE `ALIVE`='1';";
                    ausr = Database.matrixReadDB(conn, query);
                    msg = "S|dir|";
                    for i in range(len(ausr)):
                        msg += (str(ausr[i][4])+"/;");
                    return msg;

                # directory translate.
                tmp = argv[2].split("/");
                par = "";
                for i in range(len(tmp)):
                    if i in [3, 4]: continue;
                    par += tmp[i]+"/";
                
                query = "SELECT * FROM `source` WHERE `USER`='"+tmp[4]+"' AND SUBSTRING(`DIR`, '1', '"+str(len(par))+"')='"+str(par)+"';";
                print query;
                res = Database.matrixReadDB(conn, query);
                msg = "S|dir|";
                pool = [];
                x = par.split("/");
                nx = len(x);
                for item in res:
                    y = item[1].split("/");
                    ny = len(y)-1;
                    z = item[4].split("&");
                    if (item[3] == "0") and (usr not in z):
                        continue;
                    #print x
                    #print y
                    if (y[nx-1]+"/" or y[nx-1]) not in pool:
                        if ny >= nx:
                            pool.append(str(y[nx-1])+"/");
                        else:
                            pool.append(str(y[nx-1]));
                Database.closeDB(conn);

                print pool;
                for i in pool:
                    msg += (i+";");
                if msg != "S|dir|":
                    return msg;
                else:
                    return "E|Access Denied."
            elif argv[1] == "file":
                query = "SELECT * FROM `source` WHERE `DIR`='"+str(argv[2])+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    return "E|Removed";
                z = res[0][4].split("&");
                if (res[0][3] == "0") and (usr not in z):
                    return "E|Access Denied.";
                msg = "S|file|"+str(res[0][5]);
                return msg;
            
        if argv[0] == "Q": # user want to copy, send query to server, server return how to connect to the resource.
            # optimize the source, hope to listen to ACK4.
            # optimize will connect C1 with command.
            reload(Analyzer);
            print argv[1];
            print argv[2];
            Analyzer.Optimize(clientAddr[0], argv[1], argv[2]);
            return "Ongoing";

    #----------------------------------------------------------------------------------------------------------------
    elif argc == 5:
        
        if argv[0] == "ACK4": # receive msg from C2, send command to C1. TCP
            if argv[1] == "BT": # if bluetooth, send C1: "Try|BT|<name>|src|dest"
                conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
                query = "SELECT * FROM `user` WHERE `IP`='"+str(clientAddr[0])+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    print "Error: User not found.";
                    return "";
                msg = "Try|BT|"+str(res[0][13])+"|"+argv[3]+"|"+argv[4];
                try:
                    socket.setdefaulttimeout(10);
                    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                    sck.connect((str(argv[2]), 11314));
                    sck.sendto(msg);
                except socket.error, socket.timeout:
                    print "Connect C1 timeout @ ACK4 received.";
                return "";
            elif argv[1] in ["WiFi", "3G"]: # if wi-fi or 3g, send C1: "Try|WiFi|<IP>|src|dest"
                msg = "Try|"+str(argv[1])+"|"+str(clientAddr[0])+"|"+argv[3]+"|"+argv[4];
                try:
                    socket.setdefaulttimeout(10);
                    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                    sck.connect((str(argv[2]), 11314));
                    sck.send(msg);
                except socket.error, socket.timeout:
                    print "Connect C1 timeout @ ACK4 received."
                return "";
            elif argv[1] == "F": # Receive hardware failed in C2, then try other methods in C2, if not available, send request failed message.
                msg = "Try|F|"+str(clientAddr[0])+"|F|F";
                try:
                    socket.setdefaulttimeout(10);
                    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
                    sck.connect((str(argv[2]), 11314));
                    sck.send(msg);
                except socket.error, socket.timeout:
                    print "Connect C1 timeout @ ACK4 received."
                return "";
