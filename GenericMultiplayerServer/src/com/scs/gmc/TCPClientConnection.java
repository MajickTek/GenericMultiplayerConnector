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

import ssmith.net.TCPNetworkMultiServer3;
import ssmith.net.TCPNetworkMultiServerConn3;

public final class TCPClientConnection extends TCPNetworkMultiServerConn3 {

	public TCPClientConnection(TCPNetworkMultiServer3 svr, Socket sck) throws IOException {
		super(svr, sck);
	}

}
