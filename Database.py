import MySQLdb;

def openDB(HOST, USR, PWD, DB):
    try:
        conn = MySQLdb. connect(host = HOST,
                               user = USR,
                               passwd = PWD,
                               db = DB);
        return conn;
    except (MySQLdb.Error):
        print "MySQL Database Error @ openDB";
        return False;

def matrixReadDB(conn, queryStr):
    try:
        cursor = conn.cursor();
        cursor.execute(queryStr);
        item = cursor.fetchall();
        cursor.close();
        return item;
    except (MySQLdb.Error):
        print "MySQL Database Error @ matrixReadDB -> %s" % queryStr;
        return False;

def dictReadDB(conn, queryStr):
    try:
        cursor = conn.cursor(MySQLdb.cursors.DictCursor);
        cursor.execute(queryStr);
        res = cursor.fetchall();
        cursor.close();
        return res;
    except (MySQLdb.Error):
        print "MySQL Database Error @ dictReadDB -> %s" % queryStr;
        return False;

def closeDB(conn):
    try:
        conn.commit();
        conn.close();
        return True;
    except (MySQLdb.Error):
        print "MySQL Database Error @ closeDB";
        return False;

def updateDB(conn, queryStr):
    try:
        cursor = conn.cursor();
        cursor.execute(queryStr);
        cursor.close();
        conn.commit();
        return True;
    except (MySQLdb.Error):
        print "MySQL Database Error @ updateDB -> %s" % queryStr;
        return False;
