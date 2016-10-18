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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import ssmith.util.Interval;

public final class ServerMain implements ErrorHandler {

	private final Map<TCPClientConnection, PlayerData> players_by_sck = new HashMap<TCPClientConnection, PlayerData>();
	private final Map<String, ServerGame> games = new HashMap<String, ServerGame>();

	private final Map<TCPClientConnection, PlayerData> new_players = new HashMap<TCPClientConnection, PlayerData>(); // temp list to avoid sync issues
	private final List<TCPClientConnection> to_remove = new ArrayList<TCPClientConnection>();  // temp list to avoid sync issues

	private final Interval alive_int = new Interval(15 * 1000);
	private final UDPConnection udpconn_4_receiving;
	private boolean stop = false;

	public ServerMain() throws IOException {
		super();

		p("Starting " + Statics.TITLE + " server v" + Statics.CODE_VERSION);
		if (Statics.DEBUG) {
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

		int port = Integer.parseInt(props.getProperty("port", ""+Statics.DEF_PORT));

		TCPNetworkServer tcp_server = new TCPNetworkServer(this, port, this);
		tcp_server.setDaemon(true);
		tcp_server.start();

		udpconn_4_receiving = new UDPConnection(this, port, this);
		udpconn_4_receiving.start();

		gameLoop();
	}


	private void gameLoop() {
		while (!stop) {
			try {
				boolean check_ping = alive_int.hitInterval();
				Iterator<TCPClientConnection> it_conn = players_by_sck.keySet().iterator();
				while (it_conn.hasNext()) {
					TCPClientConnection conn = it_conn.next();
					PlayerData playerdata = this.players_by_sck.get(conn);
					try {
						DataInputStream dis = conn.getDataInputStream();
						DataOutputStream dos = conn.getDataOutputStream();

						if (dis.available() > 1) {
							DataCommand cmd = DataCommand.get(dis.readByte());
							//p("Got cmd: " + cmd.name());
							switch (cmd) {
							case C2S_VERSION:
								int client_version = dis.readInt();
								byte check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								//p("Got version " + client_version);

								if (client_version >= Statics.COMMS_VERSION) {
									synchronized (dos) { 
										dos.writeByte(DataCommand.S2C_VERSION_OK.getID());
										dos.writeByte(Statics.CHECK_BYTE);
									}
								} else {
									sendErrorToClient(dos, ErrorCodes.INCOMPATIBLE_CODE_VERSION, "Invalid version; " + Statics.CODE_VERSION + " required.  Run update to get latest version.");
									this.schedulePlayerRemoval(conn);
								}
								break;

							case C2S_PING_RESPONSE:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}

								playerdata.ping = System.currentTimeMillis() - playerdata.pingme_time;
								//ServerMain.p("Client ping time is " + playerdata.ping);
								playerdata.awaiting_ping_response = false;
								break;

							case C2S_JOIN_GAME:
								String name = dis.readUTF().trim();
								String gameid = dis.readUTF().trim();
								int min_players = dis.readInt();
								int max_players = dis.readInt();
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								if (name.length() > 0) { 
									if (playerdata == null) {
										playerdata = new PlayerData(this, conn, name, gameid);
									} else {
										playerdata.gameid = gameid;
										playerdata.in_game = true;
									}
									p(name + " has joined game " + gameid);
									synchronized (players_by_sck) {
										new_players.put(conn, playerdata);
									}
									// tell player their id
									synchronized (conn) {
										conn.getDataOutputStream().writeByte(DataCommand.S2C_PLAYER_ID.getID());
										conn.getDataOutputStream().writeInt(playerdata.id);
										conn.getDataOutputStream().writeByte(Statics.CHECK_BYTE);
									}
									// Check game exists
									ServerGame game;
									synchronized (games) {
										if (!games.containsKey(gameid)) {
											game = new ServerGame(gameid, min_players, max_players); 
											p("Created new game " + gameid);
											games.put(gameid, game);
										}
										game = games.get(gameid);
									}
									synchronized (game.players_by_id) {
									if (game.players_by_id.size() < game.max_players || game.max_players < 0) {
										game.players_by_id.put(playerdata.id, playerdata);
										this.sendCurrentPlayers(game);
										// tell all other players new joiner
										DataArrayOutputStream daos = new DataArrayOutputStream();
										daos.writeByte(DataCommand.S2C_NEW_PLAYER.getID());
										daos.writeUTF(playerdata.name);
										daos.writeByte(Statics.CHECK_BYTE);
										this.sendTCPToAll(game, daos, playerdata.id);

										// tell other players if min reached
										if (game.players_by_id.size() >= game.min_players && game.game_started == false) {
											p("Game " + game.gameid + " has started with " + game.players_by_id.size() + " players");
											game.game_started = true;
											daos = new DataArrayOutputStream();
											daos.writeByte(DataCommand.S2C_GAME_STARTED.getID());
											daos.writeByte(Statics.CHECK_BYTE);
											this.sendTCPToAll(game, daos, -1);
										}
									} else {
										this.sendErrorToClient(dos, ErrorCodes.TOO_MANY_PLAYERS, "Too many players");
									}
									}
								} else {
									this.sendErrorToClient(dos, ErrorCodes.INVALID_NAME, "Invalid name: '" + name + "'");
								}
								break;


							case C2S_DISCONNECTING:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}

								p("Player " + playerdata.name + " disconnecting.");
								this.schedulePlayerRemoval(conn);
								break;

							case C2S_TCP_KEYVALUE_DATA:
								int code = dis.readInt();
								int value = dis.readInt();
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								this.broadcastKeyValueData(playerdata.id, code, value, games.get(playerdata.gameid));
								break;

							case C2S_TCP_STRING_DATA:
								String data = dis.readUTF();
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								this.broadcastStringData(playerdata.id, data, games.get(playerdata.gameid));
								break;

							case C2S_TCP_BYTEARRAY_DATA:
								int len = dis.readInt();
								byte b[] = new byte[len];
								dis.read(b);
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								this.broadcastByteArray(playerdata.id, b, games.get(playerdata.gameid));
								break;

							case C2S_OUT_OF_GAME:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								p("Player " + playerdata.name + " is out of the game.");
								playerdata.in_game = false; // todo - send warning if already out of game
								checkEnoughPlayers(playerdata.gameid);
								break;

							case C2S_WINNER:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte after receiving " + cmd);
								}
								ServerGame game = games.get(playerdata.gameid);
								if (game != null) {
									if (game.winner == null) {
										game.winner = playerdata;
										p("Player " + playerdata.name + " claims to have won");
										this.sendGameOverAndRemoveGame(game, playerdata);
									}
								}
								break;

							case C2S_SEND_ERROR:
								this.decodeError(dis);
								break;

							default:
								/*if (Statics.STRICT) {
									throw new IOException("Unknown command: " + cmd);
								} else {*/
								p("Unknown command: " + cmd);
								this.schedulePlayerRemoval(conn);
								//}
							}
						}

						if (check_ping && playerdata != null && playerdata.awaiting_ping_response == false) {
							synchronized (dos) { 
								dos.writeByte(DataCommand.S2C_PING_ME.getID());
								dos.writeByte(Statics.CHECK_BYTE);
							}
							playerdata.pingme_time = System.currentTimeMillis();
							playerdata.awaiting_ping_response = true;
						}
					} catch (IOException ex) {
						System.err.println("Error sending to player " + playerdata.name);
						ex.printStackTrace(); // Don't log it since there could be loads
						this.schedulePlayerRemoval(conn);
					} catch (Exception ex) {
						handleError(ex);
					}
				}

				synchronized (to_remove) {
					while (to_remove.isEmpty() == false) {
						this.removePlayer(to_remove.remove(0));
					}
				}


				synchronized (new_players) {
					for (TCPClientConnection conn : this.new_players.keySet()) {
						synchronized (players_by_sck) {
							this.players_by_sck.put(conn, new_players.get(conn));
						}
					}
					new_players.clear();
				}


				Functions.delay(200);
			} catch (Exception ex) {
				handleError(ex);
				/*if (Statics.STRICT) {
					System.exit(0);				
				}*/
			}
		}
		this.udpconn_4_receiving.stopNow();
		p("Server exiting");
		// loop through all connections and close them
		synchronized (players_by_sck) {
			Iterator<TCPClientConnection> it = players_by_sck.keySet().iterator();
			while (it.hasNext()) {
				TCPClientConnection conn = it.next();
				conn.close();
			}
		}
		System.exit(0);
	}


	private void removePlayer(TCPClientConnection conn) throws IOException {
		synchronized (players_by_sck) {
			PlayerData player = this.players_by_sck.get(conn);
			players_by_sck.remove(conn); // So we don't send them "remove object" messages
			if (player != null) {
				ServerGame game = this.games.get(player.gameid);
				if (game != null) {
					game.players_by_id.remove(player.id);
					this.sendCurrentPlayers(game); // Must send this before we send the "player left" cmd
				}
				ServerMain.p("Removed player " + player.name + ".");
				//this.broadcastMsg("Player " + player.name + " has left");
				// send cmd to client
				DataArrayOutputStream daos = new DataArrayOutputStream();
				daos.writeByte(DataCommand.S2C_PLAYER_LEFT.getID());
				daos.writeUTF(player.name);
				daos.writeByte(Statics.CHECK_BYTE);
				this.sendTCPToAll(game, daos, player.id);
				if (game != null) {
					checkEnoughPlayers(game.gameid);
				}
			}
			conn.close(); // This gets passed separately as player may be null if they haven't join
			ServerMain.p("There are now " + players_by_sck.size() + " connections.");
		}
	}


	private void checkEnoughPlayers(String gameid) throws IOException {
		ServerGame game = this.games.get(gameid);
		if (game != null) {
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
					if (num_players == 1) {
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


	private void sendGameOverAndRemoveGame(ServerGame game, PlayerData winner) throws IOException {
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


	private void sendCurrentPlayers(ServerGame game) throws IOException {
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


	private void sendTCPToAll(ServerGame game, DataArrayOutputStream daos, int exceptplayerid) throws IOException {
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


	private void schedulePlayerRemoval(TCPClientConnection conn) {
		synchronized (to_remove) {
			if (to_remove.contains(conn) == false) {
				to_remove.add(conn);
			}
		}
	}


	private void sendErrorToClient(DataOutputStream dos, int code, String error) throws IOException {
		synchronized (dos) {
			dos.writeByte(DataCommand.S2C_ERROR.getID());
			dos.writeInt(code);
			dos.writeUTF(error);
			dos.writeByte(Statics.CHECK_BYTE);
		}
	}


	public void decodeError(DataInputStream dis) throws IOException {
		String error = dis.readUTF();
		byte check = dis.readByte();
		if (check != Statics.CHECK_BYTE) {
			throw new IOException("Invalid check byte");
		}

		//todo TextFile.QuickAppend(Statics.CLIENT_ERROR_LOG, error, false);

	}


	public static void p(String s) {
		System.out.println("Server: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	public static void debug(String s) {
		if (Statics.DEBUG) {
			System.out.println("Debug: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
		}
	}


	public static void pe(String s) {
		System.err.println("Server: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	public void addConnection(TCPClientConnection conn) throws IOException {
		synchronized (new_players) {
			this.new_players.put(conn, null);
		}
	}


	@Override
	public void handleError(Exception ex) {
		ex.printStackTrace();
		try {
			String err = Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "\nVersion:" + Statics.CODE_VERSION + "\n" + Functions.Throwable2String(ex) + "\n\n";
			//todo TextFile.QuickAppend(Statics.SERVER_ERROR_LOG, err, false);
		} catch (Exception ex2) {
			ex2.printStackTrace();
		}
	}


	public static void main(String[] args) {
		try {
			new ServerMain();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.exit(0);
	}


	public ServerGame getGame(String id) {
		return this.games.get(id);
	}

}
