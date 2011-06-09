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
        pass;
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
                    return "OFFLINE|SENSOR|DONE";
                else:
                    query = "UPDATE `sensors` SET `"+sensor+"`='0' WHERE `SENSOR_OWNER`='"+\
                            owner+"' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    return "OFFLINE|SENSOR|DONE";
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
                    return "SETPUBLIC|SENSOR|DONE";
                else:
                    query = "UPDATE `sensors` SET `"+sensor+"`='1' WHERE `SENSOR_OWNER`='"+\
                            owner+"' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    return "SETPUBLIC|SENSOR|DONE";
                    
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
                    return "SETPRIVATE|SENSOR|DONE";
                else:
                    query = "SELECT `"+sensor+"` FROM `sensors` WHERE `SENSOR_OWNER`='"+owner \
                        +"' AND `SENSOR_DEVICE`='"+devname+"';";
                    res = Database.matrixReadDB(conn, query);
                    if len(res) > 0 and res[0][0] != "0":
                        acclist += "&"+res[0][0];
                    query = "UPDATE `sensors` SET `"+sensor+"`='"+acclist \
                            +"' WHERE `SENSOR_OWNER`='"+owner+ \
                            "' AND `SENSOR_DEVICE`='"+devname+"';";
                    Database.updateDB(conn, query);
                    return "SETPRIVATE|SENSOR|DONE";
                
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

    else:
        pass;
