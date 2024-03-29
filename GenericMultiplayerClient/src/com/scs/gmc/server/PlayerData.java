package com.scs.gmc.server;

import java.net.InetAddress;

public class PlayerData {

	private static int next_id=1;
	public int id;

	public final ServerMain main;

	public long ping_req_sent_time, ping;
	public boolean awaiting_ping_response;
	public final TCPClientConnection conn;
	public InetAddress address;
	public int port;
	
	public String name;
	public String gameid;
	public boolean in_game = true; // Set to false when they're out of the game, e.g. killed by baddies


	public PlayerData(ServerMain _main, TCPClientConnection _conn) {//, String _name, String _gameid) {
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
		//name = _name;
		//gameid = _gameid;

	}
	
	
	@Override
	public String toString() {
		return "PlayerData:" + this.id + " (" + name + ")";
	}
}
