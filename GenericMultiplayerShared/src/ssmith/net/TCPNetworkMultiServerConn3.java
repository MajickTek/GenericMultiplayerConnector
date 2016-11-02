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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public abstract class TCPNetworkMultiServerConn3 { // todo - rename to TCPConnectionWrapper

	public Socket sck;
	private DataOutputStream dos;
	private DataInputStream dis;
	public long started_time;

	
	public TCPNetworkMultiServerConn3(Socket sck) throws IOException {
		super();
		
		//this.setDaemon(false); //No, as we don't want to stop it abruptly
		this.sck = sck;
		
		dos = new DataOutputStream(sck.getOutputStream());
		dis = new DataInputStream(sck.getInputStream());
		this.started_time = System.currentTimeMillis();
		
		sck.setTcpNoDelay(true);
	}

	
	public void close() {
		try {
			dis.close();
			dos.close();
			sck.close();
		} catch (IOException e) {
			// Nothing
		}
	}

	
	public DataOutputStream getDataOutputStream() {
		return dos;
	}

	
	public DataInputStream getDataInputStream() {
		return dis;
	}


	public InetAddress getINetAddress() {
		return sck.getInetAddress();
	}

}
