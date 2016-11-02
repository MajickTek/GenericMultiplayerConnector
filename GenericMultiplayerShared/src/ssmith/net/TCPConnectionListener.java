/*
 *  This file is part of GenericMultiplayerConnector.

    GenericMultiplayerConnector is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GenericMultiplayerConnector is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GenericMultiplayerConnector.  If not, see <http://www.gnu.org/licenses/>.

 */

package ssmith.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ssmith.lang.ErrorHandler;

public final class TCPConnectionListener extends Thread {

    private ServerSocket sckListener;
    public boolean debug = false;
    private static volatile boolean stopNow = false;
    private ErrorHandler error_handler;
    private IConnectionCollector collector;

    public TCPConnectionListener(int port, int backlog, IConnectionCollector _collector, ErrorHandler _error_handler) throws IOException {
	    super(TCPConnectionListener.class.getSimpleName());
	    
	    collector = _collector;
	    error_handler = _error_handler;
	    
        this.setDaemon(true);
        sckListener = new ServerSocket(port, backlog);
    }

    
    public final void run() {
        try {
            System.out.println("Waiting for connections on port " + this.sckListener.getLocalPort() + "...");
            while (!stopNow) {
                Socket s = sckListener.accept();
                System.out.println("Client connected.");
                createConnectionPre(s);
            }
            System.out.println("NetworkMultiServer2 stopped.");
        } catch (Exception e) {
        	error_handler.handleError(e);
        }
    }


    public void createConnectionPre(Socket sck) throws IOException {
    	//createConnection(sck);
    	collector.newConnection(sck);
        System.out.println("Connection made from " + sck.getInetAddress().toString() + ".");//  There are now " + num_conns + " users connected.");
    }


    public static void StopListening() { // todo - use IConnectionCollector
        stopNow = true;
    }

}
