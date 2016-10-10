package com.scs.gmc;

import java.util.HashMap;

public class ServerGame {
	
	public String gameid;
	public int min_players, max_players;
	public final HashMap<Integer, PlayerData> players_by_id = new HashMap<Integer, PlayerData>(); // Player ID todo - remove when conn removed
	public boolean game_started = false;
	
	public ServerGame(String _gameid, int _min_players, int _max_players) {
		super();
		
		gameid = _gameid;
		min_players = _min_players;
		max_players = _max_players;
	}
}
