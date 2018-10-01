package com.scs.gmc.server;

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
