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

import java.net.InetAddress;

public class PlayerData {

	private static int next_id=1;
	public int id;

	public final ServerMain main;
	public String name;
	public String gameid;

	public long pingme_time, ping;
	public boolean awaiting_ping_response;

	public final TCPClientConnection conn;
	public InetAddress address;
	public int port;
	public boolean in_game = true; // Set to false when they're killed in the game


	public PlayerData(ServerMain _main, TCPClientConnection _conn, String _name, String _gameid) {
		super();

		synchronized (_main) {
			id = next_id++;
			if (id == Integer.MAX_VALUE) {
				id = 1;
				next_id = 2;
			}
		}
		main = _main;
		conn = _conn;
		name = _name;
		gameid = _gameid;

	}
}
