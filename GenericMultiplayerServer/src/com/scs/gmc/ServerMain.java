/*
 * Copyright (c)2016 Stephen Carlyle-Smith

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.scs.gmc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ssmith.lang.DataArrayOutputStream;
import ssmith.lang.Dates;
import ssmith.lang.ErrorHandler;
import ssmith.lang.Functions;
import ssmith.net.IConnectionCollector;
import ssmith.net.TCPConnectionListener;

public final class ServerMain implements ErrorHandler, IConnectionCollector {

	private final List<TCPClientConnection> connections = new ArrayList<TCPClientConnection>();
	private final Map<String, ServerGame> games = new HashMap<String, ServerGame>();

	private final List<Socket> new_players = new ArrayList<Socket>(); // temp list to avoid sync issues
	private final List<TCPClientConnection> to_remove = new ArrayList<TCPClientConnection>();  // temp list to avoid sync issues

	private final UDPConnectionListener udp_listener;
	private boolean stop = false;
	public static boolean debug;

	public ServerMain(boolean _debug) throws IOException {
		super();

		debug = _debug;

		p("Starting " + Statics.TITLE + " server v" + Statics.CODE_VERSION);
		if (debug) {
			p("####### DEBUG MODE! ############");
		}

		Properties props = new Properties();
		try {
			File f = new File(Statics.SERVER_PROPS);
			if (f.exists()) {
				InputStream inStream = new FileInputStream( f );
				props.load(inStream);
				inStream.close();
			}
		}
		catch (Exception e ) {
			e.printStackTrace();
		}

		int port = Statics.DEF_PORT;
		try {
			port = Integer.parseInt(props.getProperty("port", ""+Statics.DEF_PORT));
		} catch (NumberFormatException ex) {
			pe("Error parsing port - using default");
		}

		TCPConnectionListener new_conn_listener = new TCPConnectionListener(port, -1, this, this);
		new_conn_listener.setDaemon(true);
		new_conn_listener.start();

		udp_listener = new UDPConnectionListener(this, port, this);
		udp_listener.start();

		gameLoop();
	}


	private void gameLoop() {
		while (!stop) {
			try {
				synchronized (new_players) {
					/*for (TCPClientConnection conn : this.new_players.keySet()) {
						synchronized (players_by_sck) {
							this.players_by_sck.put(conn, new_players.get(conn));
							new Thread(conn, "TCPClientConnection").start();
						}
					}*/
					while (new_players.isEmpty() == false) {
						Socket sck = this.new_players.remove(0);
						TCPClientConnection conn = new TCPClientConnection(sck, this);
						synchronized (connections) {
							PlayerData pd = new PlayerData(this, conn);
							this.connections.add(conn);
							conn.playerdata = pd;
							new Thread(conn, "TCPClientConnection").start();
						}
					}
					//new_players.clear();
				}

				synchronized (to_remove) {
					while (to_remove.isEmpty() == false) {
						this.removePlayer(to_remove.remove(0));
					}
				}

				Functions.delay(200);
			} catch (Exception ex) {
				handleError(ex);
			}
		}
		this.udp_listener.stopNow();
		p("Server exiting");
		// loop through all connections and close them
		synchronized (connections) {
			Iterator<TCPClientConnection> it = connections.iterator();//.keySet().iterator();
			while (it.hasNext()) {
				TCPClientConnection conn = it.next();
				conn.close();
			}
		}
	}


	/**
	 * Do not call this directly, use schedulePlayerRemoval().
	 * 
	 * @param conn
	 * @throws IOException
	 */
	private void removePlayer(TCPClientConnection conn) throws IOException {
		synchronized (connections) {
			if (connections.contains(conn)) { // Have they already been removed?
				PlayerData playerdata = conn.playerdata;
				connections.remove(conn); // So we don't send them the "remove object" messages
				if (playerdata != null) {
					ServerMain.p("Removed player " + playerdata.name + ".");
					ServerGame game = this.games.get(playerdata.gameid);
					if (game != null) {
						game.players_by_id.remove(playerdata.id);
						ServerMain.p("Removed player " + playerdata.name + " from game " + game.gameid);
						this.sendCurrentPlayers(game); // Must send this before we send the "player left" cmd
					}

					// send cmd to client
					DataArrayOutputStream daos = new DataArrayOutputStream();
					daos.writeByte(DataCommand.S2C_PLAYER_LEFT.getID());
					daos.writeUTF(playerdata.name);
					daos.writeByte(Statics.CHECK_BYTE);
					this.sendTCPToAll(game, daos, playerdata.id);
					if (game != null) {
						checkForWinner(game.gameid); // Must do this after we've told the clients that a player has gone
					}
				}
				conn.close(); // This gets passed separately as player may be null if they haven't join
				ServerMain.p("There are now " + connections.size() + " connections.");
			}
		}
	}


	public void checkForWinner(String gameid) throws IOException {
		ServerGame game = this.games.get(gameid);
		if (game != null) {
			if (game.game_started) {
				int num_players = 0;
				PlayerData only_player = null;
				try {
					synchronized (game.players_by_id) {
						if (game.players_by_id != null) {
							Iterator<PlayerData> it = game.players_by_id.values().iterator();
							while (it.hasNext()) {
								PlayerData pd = it.next();
								if (pd.in_game) {
									only_player = pd;
									num_players++;
								}
							}
						}
					}
					if (num_players <= 1) {
						if (num_players == 1 && game.min_players > 0) {
							this.sendGameOverAndRemoveGame(game, only_player); // Remove the game so when all players leave, there's still a winner
							only_player.in_game = false;
						} else if (num_players == 0) {
							this.sendGameOverAndRemoveGame(game, null);
						}
					}
				} catch (NullPointerException ex) {
					ex.printStackTrace();
				}
			}
		}
	}


	public void sendGameOverAndRemoveGame(ServerGame game, PlayerData winner) throws IOException {
		if (winner != null) {
			p("Player " + winner.name + " has won!");
		} else {
			p("No-one has won.");
		}
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.S2C_GAME_OVER.getID());
		if (winner != null) {
			daos.writeInt(winner.id);
			daos.writeUTF(winner.name);
		} else {
			daos.writeInt(-1);
			daos.writeUTF("No-one");
		}
		daos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, daos, -1);
		daos.close();

		synchronized (games) {
			this.games.remove(game.gameid);
		}
		p("Removed game " + game.gameid);

	}


	public void broadcastKeyValueData(int fromplayerid, int code, int value, ServerGame game) throws IOException {
		DataArrayOutputStream dos = new DataArrayOutputStream();
		dos.writeByte(DataCommand.S2C_TCP_KEYVALUE_DATA.getID());
		dos.writeInt(fromplayerid);
		dos.writeInt(code);
		dos.writeInt(value);
		dos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, dos, fromplayerid);
		dos.close();

	}


	public void broadcastStringData(int fromplayerid, String data, ServerGame game) throws IOException {
		DataArrayOutputStream dos = new DataArrayOutputStream();
		dos.writeByte(DataCommand.S2C_TCP_STRING_DATA.getID());
		dos.writeInt(fromplayerid);
		dos.writeUTF(data);
		dos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, dos, fromplayerid);
		dos.close();

	}


	public void broadcastByteArray(int fromplayerid, byte[] data, ServerGame game) throws IOException {
		DataArrayOutputStream dos = new DataArrayOutputStream();
		dos.writeByte(DataCommand.S2C_TCP_BYTEARRAY_DATA.getID());
		dos.writeInt(fromplayerid);
		dos.writeInt(data.length);
		dos.write(data, 0, data.length);
		dos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, dos, fromplayerid);
		dos.close();

	}


	public void broadcastObject(int fromplayerid, byte data[], ServerGame game) throws IOException {
		DataArrayOutputStream dos = new DataArrayOutputStream();
		dos.writeByte(DataCommand.S2C_TCP_OBJECT_DATA.getID());
		dos.writeInt(fromplayerid);
		dos.writeInt(data.length); // todo - use long
		dos.write(data, 0, data.length);
		dos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, dos, fromplayerid);
		dos.close();

	}


	public void sendCurrentPlayers(ServerGame game) throws IOException {
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.S2C_CURRENT_PLAYERS.getID());
		synchronized (game.players_by_id) {
			daos.writeByte(game.players_by_id.size());
			Iterator<PlayerData> it_p = game.players_by_id.values().iterator();
			while (it_p.hasNext()) {
				PlayerData pd = it_p.next();
				if (pd.gameid.equalsIgnoreCase(game.gameid)) {
					daos.writeInt(pd.id);
					daos.writeUTF(pd.name);
				}
			}
		}
		daos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, daos, -1);
	}


	public void sendTCPToAll(ServerGame game, DataArrayOutputStream daos, int exceptplayerid) throws IOException {
		if (game != null) {
			synchronized (game.players_by_id) {
				Iterator<PlayerData> it_p = game.players_by_id.values().iterator();
				while (it_p.hasNext()) {
					PlayerData pd = it_p.next();
					if (pd.id != exceptplayerid) {
						try {
							pd.conn.getDataOutputStream().write(daos.getByteArray());
							pd.conn.getDataOutputStream().flush();
						} catch (IOException e) {
							this.schedulePlayerRemoval(pd.conn);
						}
					}
				}
			}
		}
	}


	public void schedulePlayerRemoval(TCPClientConnection conn) {
		synchronized (to_remove) {
			if (to_remove.contains(conn) == false) {
				to_remove.add(conn);
			}
		}
	}


	public static void p(String s) {
		System.out.println("Server: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	public static void debug(String s) {
		if (debug) {
			System.out.println("Debug: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
		}
	}


	public static void pe(String s) {
		System.err.println("Server: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	/*	public void addConnection(TCPClientConnection conn) throws IOException {
		synchronized (new_players) {
			PlayerData pd = new PlayerData(this, conn);
			this.new_players.put(conn, pd);
			conn.playerdata = pd;
		}
	}

	 */
	@Override
	public void handleError(Exception ex) {
		ex.printStackTrace();
		try {
			String err = Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "\nVersion:" + Statics.CODE_VERSION + "\n" + Functions.Throwable2String(ex) + "\n\n";
			pe("Error: " + err);
		} catch (Exception ex2) {
			ex2.printStackTrace();
		}
	}


	public ServerGame getGame(String id) {
		return this.games.get(id);
	}


	public ServerGame createGame(String gameid, int min_players, int max_players) {
		synchronized (games) {
			if (!games.containsKey(gameid)) {
				ServerGame game = new ServerGame(gameid, min_players, max_players); 
				p("Created new game " + gameid);
				games.put(gameid, game);
			}
			return games.get(gameid);
		}

	}

	@Override
	public void newConnection(Socket sck) throws IOException {
		synchronized (new_players) {
			//TCPClientConnection conn = new TCPClientConnection(sck, this);
			//PlayerData pd = new PlayerData(this, conn);
			//this.new_players.put(conn, pd);
			//conn.playerdata = pd;
			this.new_players.add(sck);
		}

	}

	//---------------------------------------------------

	public static void main(String[] args) {
		try {
			new ServerMain(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.exit(0);
	}


}
