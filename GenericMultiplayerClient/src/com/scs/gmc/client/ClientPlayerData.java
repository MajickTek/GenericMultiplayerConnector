package com.scs.gmc.client;

/**
 * Class to store data about all players in a game.
 *
 */
public class ClientPlayerData {
	
	public String name;
	public int id;

	
	@Override
	public String toString() {
		return "Player " + name + " (id:" + id + ")";
	}
}
