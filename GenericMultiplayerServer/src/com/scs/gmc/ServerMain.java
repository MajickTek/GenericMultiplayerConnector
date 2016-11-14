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

    GenericMultiplayerConnector (C)Stephen Carlyle-Smith

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

	//private final Map<TCPClientConnection, PlayerData> players_by_sck = new HashMap<TCPClientConnection, PlayerData>();
	private final List<TCPClientConnection> players_by_sck = new ArrayList<TCPClientConnection>(); // todo - rename
	private final Map<String, ServerGame> games = new HashMap<String, ServerGame>();

	//private final Map<TCPClientConnection, PlayerData> new_players = new HashMap<TCPClientConnection, PlayerData>(); // temp list to avoid sync issues
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
						synchronized (players_by_sck) {
							PlayerData pd = new PlayerData(this, conn);
							this.players_by_sck.add(conn);
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
		synchronized (players_by_sck) {
			Iterator<TCPClientConnection> it = players_by_sck.iterator();//.keySet().iterator();
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
		synchronized (players_by_sck) {
			if (players_by_sck.contains(conn)) { // Have they already been removed?
				PlayerData playerdata = conn.playerdata;
				players_by_sck.remove(conn); // So we don't send them the "remove object" messages
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
				ServerMain.p("There are now " + players_by_sck.size() + " connections.");
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
		dos.writeInt(data.length);
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
