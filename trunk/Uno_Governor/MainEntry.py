import SocketServer;
import socket;
import Interpreter;
import GlobalVariables;
import threading;
import thread;
import time, os;

# TCP

class ThreadedTCPRequestHandler(SocketServer.BaseRequestHandler):

    def handle(self):
        reload(Interpreter);
        self.recvMsg = self.request.recv(1024).strip();
        self.sndMsg = Interpreter.parseCommand(self.recvMsg, self.client_address);
        cur_thread = threading.currentThread();
        
        print "======== New Event (TCP) @ %s =========" % time.ctime(time.time());
        print "Thread: %s" % cur_thread.getName();
        print "Client: %s" % self.client_address[0];
        print "Incoming Msg: %s" % self.recvMsg;
        print "Outgoing Msg: %s\n\n" % self.sndMsg;
        print "----------------end--------------------";

        if self.sndMsg != None:
            self.request.send(self.sndMsg);
##            try:
##                socket.setdefaulttimeout(10);
##                sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
##                sck.connect((self.client_address[0],5000));
##                sck.send(self.sndMsg);
##            except socket.error, socket.timeout:
##                print "back connect client timeout";

class ThreadedTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass;

# UDP

class ThreadedUDPRequestHandler(SocketServer.BaseRequestHandler):

    def handle(self):
        reload(Interpreter);
        self.recvMsg = self.request[0].strip();
        self.sndMsg = Interpreter.parseCommand(self.recvMsg, self.client_address);
        cur_thread = threading.currentThread();
        
        print "======== New Event (UDP) @ %s =========" % time.ctime(time.time());
        print "Thread: %s" % cur_thread.getName();
        print "Client: %s" % self.client_address[0];
        print "Incoming Msg: %s" % self.recvMsg;
        print "Outgoing Msg: %s\n\n" % self.sndMsg;
        print "----------------end--------------------";

        sck = socket.socket(socket.AF_INET, socket.SOCK_DGRAM);
        if self.sndMsg != None:
            sck.sendto(self.sndMsg, (self.client_address[0], 11315));

class ThreadedUDPServer(SocketServer.ThreadingMixIn, SocketServer.UDPServer):
    pass;


# File Socket
# Currently, file transfer are all in sync mode.
class ThreadedFileSocketRequestHandler(SocketServer.BaseRequestHandler):

    def handle(self):
        self.recvMsg = self.request.recv(1024).strip();
        cur_thread = threading.currentThread();
        msg = self.recvMsg.split("|");

        print "======== New Event (File Request) @ %s =========" % time.ctime(time.time());
        print "Thread: %s" % cur_thread.getName();
        print "Client: %s" % self.client_address[0];
        print "Incoming Msg: %s" % self.recvMsg;
        
        
        if msg[0] == "FTP":
            print "Request File: %s" % msg[1];
            home = "C:/cygwin/home/jliao2";
            f = open(home+msg[1][7:len(msg[1])], "rb");
            data = f.read();
            f.close();
            self.request.send(data);
            print "File send done.\n\n";
        print "----------------end--------------------";

class ThreadedFileSocketServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass;

if __name__ == "__main__":
    
    cmd = raw_input("TCP, UDP, FTP service: ");
    
    if cmd == "TCP" or cmd == "tcp":
        tcpServer = ThreadedTCPServer(GlobalVariables.TCP_HOST, ThreadedTCPRequestHandler);
        tcpServerThread = threading.Thread(target=tcpServer.serve_forever);
        tcpServerThread.setDaemon(True);
        tcpServerThread.start();
        print "==== Now TCP service started @ %s ====" % time.ctime(time.time());
        while 1:
            pass;
        tcpServer.shutdown();
        
    elif cmd == "UDP" or cmd == "udp":
        udpServer = ThreadedUDPServer(GlobalVariables.UDP_HOST, ThreadedUDPRequestHandler);
        udpServerThread = threading.Thread(target=udpServer.serve_forever);
        udpServerThread.setDaemon(True);
        udpServerThread.start();
        print "==== Now UDP service started @ %s ====" % time.ctime(time.time());
        while 1:
            pass;
        udpServer.shutdown();

    elif cmd == "FTP" or cmd == "ftp":
        ftpServer = ThreadedFileSocketServer(GlobalVariables.FTP_HOST, ThreadedFileSocketRequestHandler);
        ftpServerThread = threading.Thread(target=ftpServer.serve_forever);
        ftpServerThread.setDaemon(True);
        ftpServerThread.start();
        print "==== Now FTP service started @ %s ====" % time.ctime(time.time());
        while 1:
            pass;
        ftpServer.shutdown();
    else:
        print "Nothing started.";
