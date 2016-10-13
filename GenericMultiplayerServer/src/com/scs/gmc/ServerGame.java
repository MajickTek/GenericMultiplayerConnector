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

import java.util.HashMap;

public class ServerGame {
	
	public String gameid;
	public int min_players, max_players;
	public final HashMap<Integer, PlayerData> players_by_id = new HashMap<Integer, PlayerData>(); // Player ID
	public boolean game_started = false;
	public PlayerData winner = null;
	
	public ServerGame(String _gameid, int _min_players, int _max_players) {
		super();
		
		gameid = _gameid;
		min_players = _min_players;
		max_players = _max_players;
	}
}
