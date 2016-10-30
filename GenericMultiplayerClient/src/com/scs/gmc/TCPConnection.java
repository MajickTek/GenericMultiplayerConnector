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

    GenericMultiplayerConnector (C)Stephen Carlyle-Smith

 */

package com.scs.gmc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class TCPConnection {

	private Socket sck;
	public DataInputStream dis;
	public DataOutputStream dos;

	public TCPConnection(Socket _sck) throws IOException {
		super();
		
		sck = _sck;
		
		sck.setTcpNoDelay(true);

		dis = new DataInputStream(sck.getInputStream());
		dos = new DataOutputStream(sck.getOutputStream());
	}
	
	
	public boolean isConnected() {
		return sck.isConnected();
	}
	

}
