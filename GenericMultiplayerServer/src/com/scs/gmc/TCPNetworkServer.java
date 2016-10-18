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

package com.scs.gmc;

import java.io.IOException;
import java.net.Socket;

import ssmith.lang.ErrorHandler;
import ssmith.net.TCPNetworkMultiServer3;

/**
 * 
 * Thread class for collecting TCP connections.
 *
 */
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
