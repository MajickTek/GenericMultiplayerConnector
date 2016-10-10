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

	public Map<TCPClientConnection, PlayerData> players_by_sck = new HashMap<TCPClientConnection, PlayerData>();
	public Map<String, ServerGame> games = new HashMap<String, ServerGame>();
	private List<TCPClientConnection> new_players = new ArrayList<TCPClientConnection>(); // temp list to avoid sync issues

	private Interval alive_int = new Interval(15 * 1000);
	private UDPConnection udpconn_4_receiving;
	private boolean stop = false;

	public ServerMain() throws IOException {
		super();

		p("Starting " + Statics.TITLE + " server v" + Statics.CODE_VERSION);
		//p("Game Type = " + Statics.GAME_TYPE.name());
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
		tcp_server.start();

		udpconn_4_receiving = new UDPConnection(this, port, this);
		udpconn_4_receiving.start();

		gameLoop();
	}


	private void gameLoop() {
		//long interpol_ms = 1;
		ArrayList<TCPClientConnection> to_remove = new ArrayList<TCPClientConnection>(); 
		while (!stop) {
			try {
				//long start = System.currentTimeMillis();
				boolean check_ping = alive_int.hitInterval();
				//synchronized (players_by_sck) {
				Iterator<TCPClientConnection> it_conn = players_by_sck.keySet().iterator();
				while (it_conn.hasNext()) {
					TCPClientConnection conn = it_conn.next();
					PlayerData playerdata = this.players_by_sck.get(conn);
					try {
						DataInputStream dis = conn.getDataInputStream();
						DataOutputStream dos = conn.getDataOutputStream();

						if (dis.available() > 1) {
							DataCommand cmd = DataCommand.get(dis.readByte());
							if (Statics.VERBOSE) {
								p("Got cmd: " + cmd.name());
							}
							switch (cmd) {
							case C2S_VERSION:
								int client_version = dis.readInt();
								byte check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte");
								}
								p("Got version " + client_version);

								synchronized (dos) { 
									if (client_version >= Statics.COMMS_VERSION) {
										dos.writeByte(DataCommand.S2C_OK.getID());
										dos.writeByte(Statics.CHECK_BYTE);
									} else {
										//todo sendErrorToClient(dos, "Invalid version; " + Statics.CODE_VERSION + " required.  Run update to get latest version.");
										//this.removePlayer(it_conn, conn);
										to_remove.add(conn);
									}
								}
								break;

							case C2S_PING_RESPONSE:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte");
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
									throw new IOException("Invalid check byte");
								}
								if (name.length() > 0) {// && name.equalsIgnoreCase("admin") == false) { 
									playerdata = new PlayerData(this, conn, name, gameid);
									synchronized (players_by_sck) {
										players_by_sck.put(conn, playerdata);
									}
									// tell player their id
									conn.getDataOutputStream().writeByte(DataCommand.S2C_PLAYER_ID.getID());
									conn.getDataOutputStream().writeInt(playerdata.id);
									conn.getDataOutputStream().writeByte(Statics.CHECK_BYTE);

									// Check game exists
									ServerGame game;
									if (!games.containsKey(gameid)) {
										game = new ServerGame(gameid, min_players, max_players); 
										p("Created new game " + gameid);
										games.put(gameid, game);
									}
									game = games.get(gameid);
									if (game.players_by_id.size() < game.max_players) {
										game.players_by_id.put(playerdata.id, playerdata);
										this.sendCurrentPlayers(game);
										// tell all other players new joiner
										DataArrayOutputStream daos = new DataArrayOutputStream();
										daos.writeByte(DataCommand.S2C_NEW_PLAYER.getID());
										daos.writeInt(playerdata.id);
										daos.writeByte(Statics.CHECK_BYTE);
										this.sendTCPToAll(game, daos);
										// tell other players if min reached
										if (game.players_by_id.size() >= game.min_players && game.game_started == false) {
											game.game_started = true;
											daos = new DataArrayOutputStream();
											daos.writeByte(DataCommand.S2C_GAME_STARTED.getID());
											daos.writeByte(Statics.CHECK_BYTE);
											this.sendTCPToAll(game, daos);
										}
									} else {
										// todo - say max players reached
									}
								} else {
									//this.sendErrorToClient(dos, "Invalid name: '" + name + "'");
									// todo - say name invalid
								}
								break;


							case C2S_EXIT:
								check = dis.readByte();
								if (check != Statics.CHECK_BYTE) {
									throw new IOException("Invalid check byte");
								}
								to_remove.add(conn);
								break;

							case C2S_SEND_ERROR:
								this.decodeError(dis);
								break;

							default:
								if (Statics.STRICT) {
									throw new IOException("Unknown command: " + cmd);
								} else {
									p("Unknown command: " + cmd);
									to_remove.add(conn);
								}
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
						ex.printStackTrace(); // Don't log it since there could be loads
						to_remove.add(conn);
					} catch (Exception ex) {
						handleError(ex);
						//to_remove.add(conn);
					}
				}

				for (TCPClientConnection conn : to_remove) {
					this.removePlayer(conn);
				}
				to_remove.clear();


				synchronized (new_players) {
					for (TCPClientConnection conn : this.new_players) {
						synchronized (players_by_sck) {
							this.players_by_sck.put(conn, null);
						}
					}
					new_players.clear();
				}


				Functions.delay(200);
			} catch (Exception ex) {
				handleError(ex);
				if (Statics.STRICT) {
					System.exit(0);				
				}
			}
		}
		this.udpconn_4_receiving.stopNow();
		p("Server exiting");
		// loop through all connections and close them
		Iterator<TCPClientConnection> it = players_by_sck.keySet().iterator();
		while (it.hasNext()) {
			TCPClientConnection conn = it.next();
			conn.close();
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
				}
				this.sendCurrentPlayers(game); // Must send this before we send the "player left" cmd
				ServerMain.p("Removed player " + player.name + ".");
				//this.broadcastMsg("Player " + player.name + " has left");
				// send cmd to client
				DataArrayOutputStream daos = new DataArrayOutputStream();
				daos.writeByte(DataCommand.S2C_PLAYER_LEFT.getID());
				daos.writeUTF(player.name);
				daos.writeByte(Statics.CHECK_BYTE);
				this.sendTCPToAll(game, daos);
				// todo - say who the winners is if only one player left
			}
			conn.close(); // This gets passed separately as player may be null if they haven't join
			ServerMain.p("There are now " + players_by_sck.size() + " connections.");
		}
	}


	/*public void broadcastMsg(String msg) throws IOException {
		//p("Sending msg: " + msg);
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.S2C_MESSAGE.getID());
		daos.writeUTF(msg);
		daos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(daos);
		daos.close();
	}
	 */

	public void broadcastData(int fromplayerid, int i1, int i2, ServerGame game) throws IOException {
		DataArrayOutputStream dos = new DataArrayOutputStream();
		dos.writeByte(DataCommand.S2C_RAW_DATA.getID());
		dos.writeInt(fromplayerid);
		dos.writeInt(i1);
		dos.writeInt(i2);
		dos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, dos);
		dos.close();

	}


	private void sendCurrentPlayers(ServerGame game) throws IOException {
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.S2C_CURRENT_PLAYERS.getID());
		daos.writeByte(game.players_by_id.size());
		synchronized (players_by_sck) {
			Iterator<TCPClientConnection> it_conn = players_by_sck.keySet().iterator(); // todo - loop thru players_by_id
			while (it_conn.hasNext()) {
				TCPClientConnection conn = it_conn.next();
				PlayerData pd = players_by_sck.get(conn);
				if (pd.gameid.equalsIgnoreCase(game.gameid)) {
					daos.writeInt(pd.id);
					daos.writeUTF(pd.name);
				}
			}
		}
		daos.writeByte(Statics.CHECK_BYTE);
		this.sendTCPToAll(game, daos);
	}


	private void sendTCPToAll(ServerGame game, DataArrayOutputStream daos) throws IOException {
		ArrayList<TCPClientConnection> to_remove = new ArrayList<TCPClientConnection>(); // Remove any broken ones
		synchronized (players_by_sck) {
			Iterator<TCPClientConnection> it_conn = players_by_sck.keySet().iterator(); // todo - loop thru players_by_id
			while (it_conn.hasNext()) {
				TCPClientConnection conn = it_conn.next();
				PlayerData pd = players_by_sck.get(conn);
				if (pd.gameid.equalsIgnoreCase(game.gameid)) {
					try {
						conn.getDataOutputStream().write(daos.getByteArray());
					} catch (IOException e) {
						//e.printStackTrace();
						to_remove.add(conn);//this.removePlayer(it_conn, conn);
					}
				}
			}
		}
		for (TCPClientConnection conn : to_remove) {
			this.removePlayer(conn);
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
			this.new_players.add(conn);
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


}
