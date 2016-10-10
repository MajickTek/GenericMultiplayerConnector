package com.scs.gmc;

import java.net.InetAddress;

public class PlayerData {

	public int id;
	public String name, gameid;
	public long pingme_time, ping;
	public boolean awaiting_ping_response;
	public TCPClientConnection conn;
	public InetAddress address;
	public int port;
	public ServerMain main;
	//public boolean connected = true;
	private static int next_id=1;

	public PlayerData(ServerMain _main, TCPClientConnection _conn, String _name, String _gameid) {
		super();
		
		id = next_id++;
		main = _main;
		conn = _conn;
		name = _name;
		gameid = _gameid;
		
	}
}
