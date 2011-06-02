import socket;
import os;
import time;

try:
    socket.setdefaulttimeout(10);
    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    sck.connect(("216.96.238.20", 11316));
except socket.error:
    print "connection timeout.";

sck.send("FTP|/mnt/sdcard/a.pdf");

data = "";
while True:
    line = sck.recv(socket.SO_RCVBUF);
    if not line:
        break;
    data += line;
    print "*****";



home = "C:/cygwin/home/jliao2/mobiHome/";
f = open(home+"a.pdf", "wb");
f.write(data);
f.flush();
f.close();


