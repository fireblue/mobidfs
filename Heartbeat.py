import socket;
import Database;
import time;


while True:
    print "======== New Heartbeat Round @ %s =========" % time.ctime(time.time());
    conn = Database.openDB("localhost", "cs560", "11235813", "mobidfs");
    query = "SELECT * from `user`;";
    ret = Database.matrixReadDB(conn, query);
    nn = len(ret);
    if (nn == 0):
        print "Database is empty. Nothing need to do.";
        continue;
    for item in ret:
        print item;
        # first clean the alive status and then start heartbeating.
        query = "UPDATE `user` SET `ALIVE`='0' WHERE `IP`='"+str(item[2])+"';";
        Database.updateDB(conn, query);
        try:
            # Try 1.
            udpsender = socket.socket(socket.AF_INET, socket.SOCK_DGRAM);
            udpsender.sendto("HB", (str(item[2]), 11315));
            print "send 1.";

            time.sleep(5); # Sleep 5 seconds to check and try.

            query = "SELECT * from `user` WHERE `IP`='"+str(item[2])+"' AND `ALIVE`='1';";
            xret = Database.matrixReadDB(conn, query);
            xn = len(xret);
            if (xn > 0):
                print "Target alive: %s" % item[2];
                udpsender.close();
                continue;

            # Try 2.
            udpsender.sendto("HB", (str(item[2]), 11315));
            print "send 2.";

            time.sleep(5); # Sleep 5 seconds to check and try.

            query = "SELECT * from `user` WHERE `IP`='"+str(item[2])+"' AND `ALIVE`='1';";
            xret = Database.matrixReadDB(conn, query);
            xn = len(xret);
            if (xn > 0):
                print "Target alive: %s" % item[2];
                udpsender.close();
                continue;

            # Try 3.
            udpsender.sendto("HB", (str(item[2]), 11315));
            print "send 3.";

            time.sleep(5); # Sleep 5 seconds to check and try.

            query = "SELECT * from `user` WHERE `IP`='"+str(item[2])+"' AND `ALIVE`='1';";
            xret = Database.matrixReadDB(conn, query);
            xn = len(xret);
            if (xn > 0):
                print "Target alive: %s" % item[2];
                udpsender.close();
                continue;

            # Clean
            query = "DELETE * from `user` WHERE `IP`='"+str(item[2])+"';";
            Database.updateDB(conn, query);
            print "Target dead: %s" % item[2];
        except socket.error:
            print "Heartbeat packet did not send out @ %s" % item[2];

    # Next round after 10 minutes.
    time.sleep(600);

