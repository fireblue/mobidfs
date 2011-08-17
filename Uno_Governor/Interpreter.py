import Database;
import GlobalVariables;
import socket;
import os;

def parseCommand(message, clientAddr):
    #return "***Welcome***\nNow server is under construction.\n\nPlease contact jliao2@utk.edu for more information";
    argv = message.split("|");
    argc = len(argv);
    
    conn = Database.openDB("localhost", "unodev", "11235813", "unodata");
    #-----------------------check wrong command------------------------
    reload(GlobalVariables);
    if argv[0] not in GlobalVariables.CommandSet:
        return "Wrong command, please contact jliao2@utk.edu for more info.";
    #-------------------------------end--------------------------------
    #------------------------------------------------------------------

    #return "Wrong command, please contact jliao2@utk.edu for more info.";
    
    if argc == 1:
        pass;
    elif argc == 2:
        if argv[0] == "GET":
            if argv[1] == "P2P":
                query = "SELECT * FROM `devices` WHERE `ALIVE`='1';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    return "POST|P2P|";
                ret = "POST|P2P|";
                for row in res:
                    # owner, device, ip
                    ret += row[1]+","+row[2]+","+row[3]+";";
                return ret;
    elif argc == 3:
        if argv[0] == "OFFLINE":
            if argv[1] == "FILE":
                ip = clientAddr[0];
                filepath = argv[2];
                xfilepath = filepath.split("/");
                filename = xfilepath[len(xfilepath)-1];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"';";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+ \
                        owner+"' AND `RESOURCE_DEVICE`='"+devname+ \
                        "' AND `RESOURCE_PATH`='"+filepath+"';"
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, \
                            `RESOURCE_OWNER`, `RESOURCE_DEVICE`, `RESOURCE_PATH`, \
                            `ACCESS_LIST`, `HOT`) VALUES ('"+filename+"','"+owner \
                            +"','"+devname+"','"+filepath+"','0','0');";
                    Database.updateDB(conn, query);
                    return "OFFLINE|FILE|DONE";
                else:
                    query = "UPDATE `resources` SET `ACCESS_LIST`='0' WHERE \
                            `RESOURCE_OWNER`='"+owner+"' AND `RESOURCE_DEVICE`='"+\
                            devname+"' AND `RESOURCE_PATH`='"+filepath+"';";
                    Database.updateDB(conn, query);
                    return "OFFLINE|FILE|DONE";
            if argv[1] == "SENSOR":
                sensor = argv[2];
                ip = clientAddr[0];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"';";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                query = "SELECT * FROM `sensors` WHERE `SENSOR_OWNER`='"+owner \
                        +"' AND `SENSOR_DEVICE`='"+devname+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `sensors` (`SENSOR_OWNER`, `SENSOR_DEVICE`, `"+ \
                            sensor+"`) VALUES ('"+owner+"', '"+devname+"', '0');";
                    Database.updateDB(conn, query);
                    #return "OFFLINE|SENSOR|DONE";
                else:
                    query = "UPDATE `sensors` SET `"+sensor+"`='0' WHERE `SENSOR_OWNER`='"+\
                            owner+"' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    #return "OFFLINE|SENSOR|DONE";

                #----- Setup sensor as a sensor file in the resource table. ------
                sensorpath = "/mnt/sdcard/Sensors/"+sensor;
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
                        +"' AND `RESOURCE_DEVICE`='"+devname \
                        +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, `RESOURCE_OWNER`,\
                            `RESOURCE_DEVICE`, `RESOURCE_PATH`, `ACCESS_LIST`, `HOT`) VALUES \
                            ('"+sensor+"', '"+owner+"', '"+devname+"', '"+sensorpath+ \
                            "', '0', '0');";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                else:
                    query = "UPDATE `resources` SET `ACCESS_LIST`='0' WHERE `RESOURCE_OWNER`='"+\
                            owner+"' AND `RESOURCE_DEVICE`='"+devname \
                            +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                #------
        if argv[0] == "SETPUBLIC":
            if argv[1] == "SENSOR":
                ip = clientAddr[0];
                sensor = argv[2];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"';";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                query = "SELECT * FROM `sensors` WHERE `SENSOR_OWNER`='"+owner \
                        +"' AND `SENSOR_DEVICE`='"+devname+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `sensors` (`SENSOR_OWNER`, `SENSOR_DEVICE`, `"+ \
                            sensor+"`) VALUES ('"+owner+"', '"+devname+"', '1');";
                    Database.updateDB(conn, query);
                    #return "SETPUBLIC|SENSOR|DONE";
                else:
                    query = "UPDATE `sensors` SET `"+sensor+"`='1' WHERE `SENSOR_OWNER`='"+\
                            owner+"' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    #return "SETPUBLIC|SENSOR|DONE";
                    
                #----- Setup sensor as a sensor file in the resource table. ------
                sensorpath = "/mnt/sdcard/Sensors/"+sensor;
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
                        +"' AND `RESOURCE_DEVICE`='"+devname \
                        +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, `RESOURCE_OWNER`,\
                            `RESOURCE_DEVICE`, `RESOURCE_PATH`, `ACCESS_LIST`, `HOT`) VALUES \
                            ('"+sensor+"', '"+owner+"', '"+devname+"', '"+sensorpath+ \
                            "', '1', '0');";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                else:
                    query = "UPDATE `resources` SET `ACCESS_LIST`='1' WHERE `RESOURCE_OWNER`='"+\
                            owner+"' AND `RESOURCE_DEVICE`='"+devname \
                            +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                #------
        if argv[0] == "GET":
            # list the available child in pwd.
            if argv[1] == "DIR":
                ip = clientAddr[0];
                resdir = argv[2];
                if resdir == "/":
                    query = "SELECT * FROM `users`;";
                    res = Database.matrixReadDB(conn, query);
                    # Empty, no resource.
                    if len(res) == 0:
                        return "POST|DIR|NO_RESOURCE";
                    reply = "POST|DIR|";
                    for i in range(len(res)):
                        reply += res[i][1]+"/^-1;";
                    return reply;
                else:
                    tmp = resdir.split("/");
                    if len(tmp) == 2: # In the user root, list all devices.
                        print resdir;
                        owner = tmp[1];
                        query = "SELECT * FROM `users` WHERE `USER_NAME`='"+owner+"';";
                        res = Database.matrixReadDB(conn, query);
                        if len(res) == 0:
                            return "POST|DIR|NO_RESOURCE";
                        dev = res[0][3];
                        devlist = dev.split("&");
                        reply = "POST|DIR|";
                        for item in devlist:
                            reply += item + "/^-1;";
                        return reply;
                    else: # In the device root.
                        owner = tmp[1];
                        device = tmp[2];
                        query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ \
                                ip+"' AND `ALIVE`='1';";
                        res = Database.matrixReadDB(conn, query);
                        if len(res) == 0:
                            return "POST|DIR|NO_RESOURCE";
                        user = res[0][1];
                        query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+ \
                                owner+"' AND `RESOURCE_DEVICE`='"+device+"';";
                        res = Database.matrixReadDB(conn, query);
                        if len(res) == 0:
                            return "POST|DIR|NO_RESOURCE";
                        reply = "POST|DIR|";
                        pool = [];
                        for i in range(len(res)):
                            cmpdir = "/"+owner+"/"+device+res[i][4];
                            acclist = res[i][5];
                            tcmpdir = cmpdir.split("/");
                            tacclist = acclist.split("&");
                            n1 = len(tmp);
                            n2 = len(tcmpdir);

                            #print "tmp = ",tmp;
                            #print "tcmpdir = ", tcmpdir;
                            
                            if n2 == n1+1: # This means a file:                                    
                                if (acclist == "1" or user in tacclist) and res[i][1] not in pool:
                                    m = 0;
                                    for r in range(n1):
                                        if tmp[r] != tcmpdir[r]:
                                            break;
                                        m += 1;
                                    if m < n1:
                                        continue;
                                    reply += res[i][1] + "^" + str(res[i][0]) + ";";
                                    pool.append(res[i][1]);
                            elif n2 > n1+1: # This means a common directory
                                if (acclist == "1" or user in tacclist):
                                    m = 0;
                                    for r in range(n1):
                                        if tmp[r] != tcmpdir[r]:
                                            break;
                                        m += 1;
                                    if m < n1:
                                        continue;
                                    if tcmpdir[n1] not in pool:
                                        reply += tcmpdir[n1] + "/^-1;";
                                        pool.append(tcmpdir[n1]);
                        
                        return reply;
                    
    elif argc == 4:
        if argv[0] == "LOGIN":
            usr = argv[1];
            pwd = argv[2];
            ip = clientAddr[0];
            dev = argv[3].split(";");

            
            query = "SELECT * FROM `users` WHERE `USER_NAME`='"+usr+"';";
            res = Database.matrixReadDB(conn, query);
            # No such user, need to register.
            if len(res) == 0:
                return "LOGIN|NO_USER";
            query = "SELECT * FROM `users` WHERE `USER_NAME`='"+usr+ \
                    "' AND `USER_PASSWORD`='"+pwd+"';";
            res = Database.matrixReadDB(conn, query);
            # Login failed.
            if len(res) == 0:
                return "LOGIN|FAILED";
            # Update "users" table's device list.
            if dev[0] not in res[0][3].split("&"):
                devlist = dev[0] + "&" + res[0][3];
            else:
                devlist = res[0][3];
            query = "UPDATE `users` SET `USER_DEVICE_LIST`='"+devlist+ \
                    "' WHERE `USER_NAME`='"+usr+"';";
            Database.updateDB(conn, query);
            # Update "devices" table
            query = "UPDATE `devices` SET `DEVICE_NAME`='"+dev[0]+ \
                    "', `DEVICE_IP`='"+ip+"', `WIFI_INFO`='"+dev[1]+ \
                    "', `3G_INFO`='"+dev[2]+"', `BT_INFO`='"+dev[3]+ \
                    "', `BATTERY_LEVEL`='"+dev[4]+ \
                    "', `ALIVE`='1' WHERE `DEVICE_OWNER`='"+usr+ \
                    "' AND `DEVICE_NAME`='"+dev[0]+"';";
            Database.updateDB(conn, query);
            # No need to update "sensors" table, once initialized, this table
            # only changed when sensor commands comes.
            return "LOGIN|DONE";
        if argv[0] == "REGISTER":
            usr = argv[1];
            pwd = argv[2];
            ip = clientAddr[0];
            dev = argv[3].split(";");

            #conn = Database.openDB("localhost", "unodev", "11235813", "unodata");
            query = "SELECT * FROM `users` WHERE `USER_NAME`='"+usr+"';";
            res = Database.matrixReadDB(conn, query);
            # User exist.
            if len(res) > 0:
                return "REGISTER|USER_EXIST";
            # Insert user into "users" table.
            query = "INSERT INTO `users` (`USER_NAME`, `USER_PASSWORD`, \
                    `USER_DEVICE_LIST`) VALUES ('"+ usr+"', '"+pwd+"', '"+dev[0]+"');";
            Database.updateDB(conn, query);
            # Insert device into "devices" table.
            query = "INSERT INTO `devices` (`DEVICE_OWNER`, `DEVICE_NAME`, \
                    `DEVICE_IP`, `WIFI_INFO`, `3G_INFO`, `BT_INFO`, `BATTERY_LEVEL`, `ALIVE`) \
                    VALUES ('"+usr+"', '"+dev[0]+"', '"+ip+"', '"+dev[1]+"', '"+dev[2]+"', '"+ \
                    dev[3]+"', '"+dev[4]+"', '1');";
            Database.updateDB(conn, query);
            # Initiate "sensors" table.
            query = "INSERT INTO `sensors` (`SENSOR_OWNER`, `SENSOR_DEVICE`) VALUES ('"+ \
                    usr+"', '"+dev[0]+"');";
            Database.updateDB(conn, query);
            return "REGISTER|DONE";
        if argv[0] == "SETPUBLIC":
            if argv[1] == "FILE":
                path = argv[2];
                xpath = path.split("/");
                resname = xpath[len(xpath)-1];
                metadata = argv[3];
                ip = clientAddr[0];
                #conn = Database.openDB("localhost", "unodev", "11235813", "unodata");
                # Find owner and device
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"';";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                # New resource. Insert into "resources" table.
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner+ \
                        "' AND `RESOURCE_DEVICE`='"+devname+"' AND `RESOURCE_PATH`='"+ \
                        path+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, `RESOURCE_OWNER`, \
                            `RESOURCE_DEVICE`, `RESOURCE_PATH`, `ACCESS_LIST`, `METADATA`, \
                            `HOT`) VALUES ('"+resname+"','"+owner+"','"+devname+"','"+path \
                            +"','1','"+metadata+"','0');";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|FILE|DONE";
                else:
                    query = "UPDATE `resources` SET `METADATA`='"+metadata+ \
                            "', `ACCESS_LIST`='1' WHERE `RESOURCE_OWNER`='"+owner \
                            +"' AND `RESOURCE_DEVICE`='"+devname+ \
                            "' AND `RESOURCE_PATH`='"+path+"';";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|FILE|DONE";
        if argv[0] == "SETPRIVATE":
            if argv[1] == "SENSOR":
                sensor = argv[2];
                acclist = argv[3];
                ip = clientAddr[0];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"';";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                query = "SELECT * FROM `sensors` WHERE `SENSOR_OWNER`='"+owner \
                        +"' AND `SENSOR_DEVICE`='"+devname+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `sensors` (`SENSOR_OWNER`, `SENSOR_DEVICE`, `"+ \
                            sensor+"`) VALUES ('"+owner+"', '"+devname+"', '"+acclist+"');";
                    Database.updateDB(conn, query);
                    #return "SETPRIVATE|SENSOR|DONE";
                else:
                    query = "SELECT `"+sensor+"` FROM `sensors` WHERE `SENSOR_OWNER`='"+owner \
                        +"' AND `SENSOR_DEVICE`='"+devname+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) > 0 and res[0][0] != "0":
                        if res[0][0] not in [None, ""]:
                            acclist += "&"+res[0][0];
                    query = "UPDATE `sensors` SET `"+sensor+"`='"+acclist \
                            +"' WHERE `SENSOR_OWNER`='"+owner+ \
                            "' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    #return "SETPRIVATE|SENSOR|DONE";
                
                #----- Setup sensor as a sensor file in the resource table. ------
                sensorpath = "/mnt/sdcard/Sensors/"+sensor;
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
                        +"' AND `RESOURCE_DEVICE`='"+devname \
                        +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, `RESOURCE_OWNER`,\
                            `RESOURCE_DEVICE`, `RESOURCE_PATH`, `ACCESS_LIST`, `HOT`) VALUES \
                            ('"+sensor+"', '"+owner+"', '"+devname+"', '"+sensorpath+ \
                            "', '"+acclist+"', '0');";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                else:
                    query = "UPDATE `resources` SET `ACCESS_LIST`='"+acclist+"' WHERE `RESOURCE_OWNER`='"+\
                            owner+"' AND `RESOURCE_DEVICE`='"+devname \
                            +"' AND `RESOURCE_PATH`='"+sensorpath+"';";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                #------
        if argv[0] == "GET":
            if argv[1] == "SENSOR":
                sensor = argv[2];
                sensorid = argv[3];
                ip = clientAddr[0];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip \
                        +"' AND `ALIVE`='1';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    return "POST|SENSOR|NO_SENSOR";
                user = res[0][1];
                query = "SELECT * FROM `resources` WHERE `ID`='"+sensorid \
                        +"' AND `RESOURCE_NAME`='"+sensor+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    return "POST|SENSOR|NO_SENSOR";
                acclist = res[0][5];
                tacclist = acclist.split("&");
                if acclist == "0" or (user not in tacclist and acclist != "1"):
                    return "POST|SENSOR|ACCESS_DENY";
                owner = res[0][2];
                devname = res[0][3];
                query = "SELECT * FROM `devices` WHERE `DEVICE_OWNER`='"+ \
                        owner+"' AND `DEVICE_NAME`='"+devname+"';";
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    return "POST|SENSOR|NO_SENSOR";
                ownerip = res[0][3];
                return "POST|SENSOR|"+ownerip;
                
    elif argc == 5:
        if argv[0] == "SETPRIVATE":
            if argv[1] == "FILE":
                path = argv[2];
                xpath = path.split("/");
                filename = xpath[len(xpath)-1];
                metadata = argv[3];
                acclist = argv[4];
                ip = clientAddr[0];
                query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip+"'";
                res = Database.matrixReadDB(conn, query);
                owner = res[0][1];
                devname = res[0][2];
                query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+ \
                        owner+"' AND `RESOURCE_DEVICE`='"+devname+ \
                        "' AND `RESOURCE_PATH`='"+path+"';"
                res = Database.matrixReadDB(conn, query);
                if len(res) == 0:
                    query = "INSERT INTO `resources` (`RESOURCE_NAME`, \
                            `RESOURCE_OWNER`, `RESOURCE_DEVICE`, `RESOURCE_PATH`, \
                            `ACCESS_LIST`, `HOT`) VALUES ('"+filename+"','"+owner \
                            +"','"+devname+"','"+path+"','"+acclist+"','0');";
                    Database.updateDB(conn, query);
                    return "SETPRIVATE|FILE|DONE";
                else:
                    new_acclist = res[0][5]+"&"+acclist;
                    query = "UPDATE `resources` SET `ACCESS_LIST`='"+new_acclist+"' WHERE \
                            `RESOURCE_OWNER`='"+owner+"' AND `RESOURCE_DEVICE`='"+\
                            devname+"' AND `RESOURCE_PATH`='"+path+"';";
                    Database.updateDB(conn, query);
                    return "SETPRIVATE|FILE|DONE";
        if argv[0] == "GET":
            if argv[1] == "SENSOR":
                if argv[2] == "LOG":
                    sensor = argv[3];
                    sensorid = argv[4];
                    ip = clientAddr[0];
                    query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+ip \
                            +"' AND `ALIVE`='1';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|SENSOR|LOG|NO_RESOURCE";
                    user = res[0][1];
                    query = "SELECT * FROM `resources` WHERE `ID`='"+sensorid \
                            +"' AND `RESOURCE_NAME`='"+sensor+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|SENSOR|LOG|NO_RESOURCE";
                    acclist = res[0][5];
                    tacclist = acclist.split("&");
                    if acclist == "0" or (user not in tacclist and acclist != "1"):
                        return "POST|SENSOR|LOG|NO_RESOURCE";
                    owner = res[0][2];
                    devname = res[0][3];
                    query = "SELECT * FROM `devices` WHERE `DEVICE_OWNER`='"+ \
                            owner+"' AND `DEVICE_NAME`='"+devname+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|SENSOR|LOG|NO_RESOURCE";
                    ownerip = res[0][3];
                    return "POST|SENSOR|LOG|"+ownerip;
                
            if argv[1] == "FILE":
                if argv[2] == "METADATA":
                    resname = argv[3];
                    resid = argv[4];
                    query = "SELECT * FROM `resources` WHERE `RESOURCE_NAME`='"+resname \
                            +"' AND `ID`='"+resid+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|FILE|METADATA|NO_RESOURCE";
                    metadata = res[0][6];
                    return "POST|FILE|METADATA|"+metadata;
                elif argv[2] == "PIN":
                    resname = argv[3];
                    resid = argv[4];
                    query = "SELECT * FROM `resources` WHERE `RESOURCE_NAME`='"+resname \
                            +"' AND `ID`='"+resid+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|FILE|PIN|NO_RESOURCE";
                    path = res[0][4];
                    owner = res[0][2];
                    device = res[0][3];
                    query = "SELECT * FROM `devices` WHERE `DEVICE_OWNER`='"+owner \
                            +"' AND `DEVICE_NAME`='"+device+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|FILE|PIN|NO_RESOURCE";
                    ip = res[0][3];
                    return "POST|FILE|"+ip+"|"+path;
				elif argv[2] == "PREVIEW":
                    resname = argv[3];
                    resid = argv[4];
                    query = "SELECT * FROM `resources` WHERE `RESOURCE_NAME`='"+resname \
                            +"' AND `ID`='"+resid+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|FILE|PREVIEW|NO_RESOURCE";
                    path = res[0][4];
                    owner = res[0][2];
                    device = res[0][3];
                    query = "SELECT * FROM `devices` WHERE `DEVICE_OWNER`='"+owner \
                            +"' AND `DEVICE_NAME`='"+device+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) == 0:
                        return "POST|FILE|PREVIEW|NO_RESOURCE";
                    ip = res[0][3];
                    return "POST|FILE|"+ip+"|"+path;
		if argv[0] == "API":
			if argv[1] == "GET":
				if argv[2] == "REMOTEDATA":
					if argv[3] == "ACCESS":
						t = argv[4].split("/");
						owner = t[1];
						device = t[2];
						path = argv[4][len(owner)+len(device)+2:];
						query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+clientAddr[0]+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTEDATA|ACCESS|NO";
						else:
							user = res[0][1];
						query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
							+"' AND `RESOURCE_DEVICE`='"+device \
							+"' AND `RESOURCE_PATH`='"+path+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTEDATA|ACCESS|NO";
						acclist = res[0][5].split("&");
						if user not in acclist:
							return "API|POST|REMOTEDATA|ACCESS|NO";
						else:
							return "API|POST|REMOTEDATA|ACCESS|YES";
					elif argv[3] == "METADATA":
						t = argv[4].split("/");
						owner = t[1];
						device = t[2];
						path = argv[4][len(owner)+len(device)+2:];
						query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+clientAddr[0]+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTEDATA|METADATA|DENIED";
						else:
							user = res[0][1];
						query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
							+"' AND `RESOURCE_DEVICE`='"+device \
							+"' AND `RESOURCE_PATH`='"+path+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTEDATA|METADATA|DENIED";
						acclist = res[0][5].split("&");
						if user not in acclist:
							return "API|POST|REMOTEDATA|METADATA|DENIED";
						else:
							return "API|POST|REMOTEDATA|METADATA|"+res[0][6];
				elif argv[2] == "REMOTESENSOR":
					if argv[3] == "ACCESS":
						t = argv[4].split("/");
						owner = t[1];
						device = t[2];
						path = argv[4][len(owner)+len(device)+2:];
						query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+clientAddr[0]+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTESENSOR|ACCESS|NO";
						else:
							user = res[0][1];
						query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
							+"' AND `RESOURCE_DEVICE`='"+device \
							+"' AND `RESOURCE_PATH`='"+path+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTESENSOR|ACCESS|NO";
						acclist = res[0][5].split("&");
						if user not in acclist:
							return "API|POST|REMOTESENSOR|ACCESS|NO";
						else:
							return "API|POST|REMOTESENSOR|ACCESS|YES";
					if argv[3] == "INSTANTREADING":
						t = argv[4].split("/");
						owner = t[1];
						device = t[2];
						path = argv[4][len(owner)+len(device)+2:];
						query = "SELECT * FROM `devices` WHERE `DEVICE_IP`='"+clientAddr[0]+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTESENSOR|INSTANTREADING|DENIED";
						else:
							user = res[0][1];
						query = "SELECT * FROM `resources` WHERE `RESOURCE_OWNER`='"+owner \
							+"' AND `RESOURCE_DEVICE`='"+device \
							+"' AND `RESOURCE_PATH`='"+path+"';";
						res = Database.matrixReadDB(conn, query);
						if len(res) == 0:
							return "API|POST|REMOTESENSOR|INSTANTREADING|DENIED";
						acclist = res[0][5].split("&");
						if user not in acclist:
							return "API|POST|REMOTESENSOR|INSTANTREADING|DENIED";
						else:
							query = "SELECT * FROM `devices` WHERE `DEVICE_OWNER`='"+res[0][2] \
								+"' AND `DEVICE_NAME`='"+res[0][3]+"';";
							xres = Database.matrixReadDB(conn, query);
							if len(xres) == 0:
								return "API|POST|REMOTESENSOR|INSTANTREADING|DENIED";
							else:
								return "API|POST|REMOTESENSOR|INSTANTREADING|"+xres[0][3];

    else:
        pass;
