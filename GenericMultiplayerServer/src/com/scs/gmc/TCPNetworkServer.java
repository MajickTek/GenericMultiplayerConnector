package com.scs.gmc;

import java.io.IOException;
import java.net.Socket;

import ssmith.lang.ErrorHandler;
import ssmith.net.TCPNetworkMultiServer3;

public final class TCPNetworkServer extends TCPNetworkMultiServer3 {

	private ServerMain main;

	public TCPNetworkServer(ServerMain _main, int _port, ErrorHandler handler) throws IOException {
		super(_port, -1, handler);
		
		main = _main;
	}


	@Override
	public void createConnection(Socket sck) throws IOException {
		TCPClientConnection conn = new TCPClientConnection(this, sck);
		main.addConnection(conn);
	}

}
