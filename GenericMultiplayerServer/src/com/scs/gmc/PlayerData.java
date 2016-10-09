package com.scs.gmc;

import java.net.InetAddress;

public class PlayerData {

	public int id;
	public String name;
	public long pingme_time, ping;
	public boolean awaiting_ping_response;
	public TCPClientConnection conn;
	public InetAddress address;
	public int port;
	public ServerMain main;
	private static int next_id=1;

	public PlayerData(ServerMain _main, TCPClientConnection _conn, String _name) {
		super();
		
		main = _main;
		conn = _conn;
		name = _name;
		id = next_id++;
		
	}
}
