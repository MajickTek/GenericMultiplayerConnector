package com.scs.gmc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.scs.gmc.shared.DataCommand;
import com.scs.gmc.shared.Statics;

import ssmith.io.Serialization;
import ssmith.lang.DataArrayOutputStream;
import ssmith.lang.Functions;
import ssmith.net.TCPConnectionWrapper;
import ssmith.util.Interval;

public final class TCPClientConnection extends TCPConnectionWrapper implements Runnable {

	public ServerMain main;
	public PlayerData playerdata;
	private final Interval alive_int = new Interval(60 * 1000);

	public TCPClientConnection(Socket sck, ServerMain _main) throws IOException {
		super(sck);

		main = _main;

	}


	@Override
	public void run() {
		try {
			while (true) {
				checkForData();
				Functions.delay(200);
			}
		} catch (IOException ex) {
			System.err.println("Error sending to player " + playerdata.name + ".  They will be removed.");
			//ex.printStackTrace(); // Don't log it since there could be loads
			main.schedulePlayerRemoval(this);
		} catch (Exception ex) {
			main.handleError(ex);
		}
	}


	private void checkForData() throws IOException {
		boolean check_ping = alive_int.hitInterval();
		TCPClientConnection conn = this;
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
					sendErrorToClient(dos, new RuntimeException("Invalid version; " + Statics.CODE_VERSION + " required.  Run update to get latest version."));
					main.schedulePlayerRemoval(conn);
				}
				break;

			case C2S_PING_RESPONSE:
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}

				if (playerdata != null) { // Otherwise not joined a game
					playerdata.ping = System.currentTimeMillis() - playerdata.ping_req_sent_time;
					//ServerMain.p("Client ping time is " + playerdata.ping);
					playerdata.awaiting_ping_response = false;
				}
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
					/*if (playerdata == null) {
							playerdata = new PlayerData(this, conn, name, gameid);
						} else {*/
					playerdata.name = name;
					playerdata.gameid = gameid;
					playerdata.in_game = true;
					//}
					main.p(name + " has joined game " + gameid);
					/*synchronized (players_by_sck) {
							new_players.put(conn, playerdata);
						}*/
					// tell player their id
					synchronized (conn) {
						conn.getDataOutputStream().writeByte(DataCommand.S2C_PLAYER_ID.getID());
						conn.getDataOutputStream().writeInt(playerdata.id);
						conn.getDataOutputStream().writeByte(Statics.CHECK_BYTE);
					}
					// Check game exists
					ServerGame game = main.createGame(gameid, min_players, max_players);
					synchronized (game.players_by_id) {
						if (game.players_by_id.size() < game.max_players || game.max_players < 0) {
							game.players_by_id.put(playerdata.id, playerdata);
							main.p("There are now " + game.players_by_id.size() + " players in game " + gameid);
							main.sendCurrentPlayers(game);
							// tell all other players new joiner
							DataArrayOutputStream daos = new DataArrayOutputStream();
							daos.writeByte(DataCommand.S2C_NEW_PLAYER.getID());
							daos.writeUTF(playerdata.name);
							daos.writeByte(Statics.CHECK_BYTE);
							main.sendTCPToAll(game, daos, playerdata.id);

							// tell other players if min reached
							if (game.players_by_id.size() >= game.min_players) {
								daos = new DataArrayOutputStream();
								daos.writeByte(DataCommand.S2C_GAME_STARTED.getID());
								daos.writeByte(Statics.CHECK_BYTE);
								if (game.game_started == false) {
									main.p("Game " + game.gameid + " has started with " + game.players_by_id.size() + " players");
									game.game_started = true;
									main.sendTCPToAll(game, daos, -1);
								} else {
									// Only send to the new player so they know the game is in progress
									dos.write(daos.getByteArray());
								}
							}
						} else {
							this.sendErrorToClient(dos, new RuntimeException("Too many players"));
						}
					}
				} else {
					this.sendErrorToClient(dos, new RuntimeException("Invalid name: '" + name + "'"));
				}
				break;


			case C2S_DISCONNECTING:
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}

				if (playerdata != null) { // Otherwise not joined a game
					main.p("Player " + playerdata.name + " disconnecting.");
				}
				main.schedulePlayerRemoval(conn);
				break;

			case C2S_TCP_KEYVALUE_DATA:
				int code = dis.readInt();
				int value = dis.readInt();
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}
				if (playerdata != null) { // Otherwise not joined a game
					main.broadcastKeyValueData(playerdata.id, code, value, main.getGame(playerdata.gameid));
				}
				break;

			case C2S_TCP_STRING_DATA:
				String data = dis.readUTF();
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}
				if (playerdata != null) { // Otherwise not joined a game
					main.broadcastStringData(playerdata.id, data, main.getGame(playerdata.gameid));
				}
				break;

			case C2S_TCP_BYTEARRAY_DATA:
				int len = dis.readInt();
				byte b[] = new byte[len];
				dis.read(b);
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}

				if (playerdata != null) { // Otherwise not joined a game
					main.broadcastByteArray(playerdata.id, b, main.getGame(playerdata.gameid));
				}
				break;

			case C2S_TCP_OBJECT_DATA:
				len = dis.readInt();
				b = new byte[len];
				dis.read(b);
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}

				if (playerdata != null) { // Otherwise not joined a game
					main.broadcastObject(playerdata.id, b, main.getGame(playerdata.gameid));
				}
				break;

			case C2S_OUT_OF_GAME:
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}
				if (playerdata != null) { // Otherwise not joined a game
					main.p("Player " + playerdata.name + " is out of the game.");
					playerdata.in_game = false; // todo - send warning if already out of game
					main.checkForWinner(playerdata.gameid);
				}
				break;

			case C2S_WINNER:
				check = dis.readByte();
				if (check != Statics.CHECK_BYTE) {
					throw new IOException("Invalid check byte after receiving " + cmd);
				}
				if (playerdata != null) { // Otherwise not joined a game
					ServerGame game = main.getGame(playerdata.gameid);
					if (game != null) {
						if (game.winner == null) {
							game.winner = playerdata;
							main.p("Player " + playerdata.name + " claims to have won");
							main.sendGameOverAndRemoveGame(game, playerdata);
						}
					}
				}
				break;

			default:
				/*if (Statics.STRICT) {
						throw new IOException("Unknown command: " + cmd);
					} else {*/
				main.p("Unknown command: " + cmd);
				main.schedulePlayerRemoval(conn);
				//}
			}
		}

		// Send ping request?
		if (check_ping && playerdata != null) {
			if (playerdata.awaiting_ping_response == false) {
				synchronized (dos) { 
					dos.writeByte(DataCommand.S2C_PING_ME.getID());
					dos.writeByte(Statics.CHECK_BYTE);
				}
				playerdata.ping_req_sent_time = System.currentTimeMillis();
				playerdata.awaiting_ping_response = true;
			} else {
				if (System.currentTimeMillis() - Statics.SERVER_DIED_DURATION_MILLIS > playerdata.ping_req_sent_time) {
					main.p("Removing " + playerdata.name + " as received no ping");
					main.schedulePlayerRemoval(conn);
				}
			}
		}
	}


	/**
	 * This will send (and cause) an exception in the client, typically with a reason they can't join.
	 * 
	 * @param dos
	 * @param code
	 * @param error
	 * @throws IOException
	 */
	private void sendErrorToClient(DataOutputStream dos, Throwable ex) throws IOException {
		synchronized (dos) {
			dos.writeByte(DataCommand.S2C_ERROR.getID());
			byte b[] = Serialization.Serialize(ex);
			dos.writeInt(b.length);
			dos.write(b);
			dos.writeByte(Statics.CHECK_BYTE);
		}
	}

}
