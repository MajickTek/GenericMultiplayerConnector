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

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import ssmith.lang.DataArrayOutputStream;
import ssmith.lang.Dates;
import ssmith.lang.Functions;
import ssmith.util.Interval;

public class ConnectorMain implements Runnable {

	public enum GameStage {WAITING_FOR_PLAYERS, IN_PROGRESS, FINISHED}

	// Networking
	private Socket sck;
	private long last_server_alive_response_time;
	private boolean server_responded_to_udpconn = false;
	protected IGameClient client;
	private TCPConnection tcpconn;
	private ClientUDPConnection udpconn;
	private volatile boolean stop_now = false;
	private Interval check_server_interval = new Interval(Statics.CHECK_SERVER_ALIVE_INTERVAL);
	private int port;
	
	private String last_error;
	private int last_error_code;

	// Our data
	private String playername, server;
	private int min_players, max_players;
	private String gameid;

	// Game data
	private int player_id = -1;
	//private volatile ClientPlayerData players[];
	public final Map<Integer, ClientPlayerData> players = new HashMap<Integer, ClientPlayerData>();
	private volatile GameStage game_stage = GameStage.WAITING_FOR_PLAYERS;
	private volatile int winner;
	private volatile String winner_name;

	/**
	 * Create an instance of the ConnectorMain.
	 * 
	 * @param _client The client that wants to be notified of events.
	 * @param _server The IP address of the server running the GMC server software to connect to.
	 * @param _port The port to connect to
	 * @param _playername Our name as it will be sent to other players.
	 * @param _gameid A unique code identifying this game.  Players will be joined to other players with the same code.
	 * @param _min_players The number of players required before the players are notified that the game has started.
	 * @param _max_players The maximum number of players to accept in the game.
	 * 
	 */
	public ConnectorMain(IGameClient _client, String _server, int _port, String _playername, String _gameid, int _min_players, int _max_players) {
		super();

		client = _client;
		playername = _playername;
		server = _server;
		port = _port;
		gameid = _gameid;
		min_players = _min_players;
		max_players = _max_players;

		if (playername == null || playername.isEmpty()) {
			throw new RuntimeException("Invalid player name");
		}
		if (gameid == null || gameid.isEmpty()) {
			throw new RuntimeException("Invalid game ID");
		}
		if (server == null || server.isEmpty()) {
			throw new RuntimeException("Invalid server");
		}
		if (min_players < 2) {
			throw new RuntimeException("A minimum of 2 players are required");
		}
		if (max_players > 0 && max_players < min_players) {
			throw new RuntimeException("Invalid maximum players");
		}

	}


	/**
	 * Actually connect to the server.
	 * @return A boolean indicating success.
	 */
	public boolean connect() {
		try {
			server_responded_to_udpconn = false;

			if (sck != null) {
				try {
					p("Already connected, so closing current connection");
					sck.close();
				} catch (IOException ex) {
					// Do nothing
				}
			}

			sck = new Socket(server, port);
			tcpconn = new TCPConnection(sck);
			udpconn = new ClientUDPConnection(this, server, port, playername);
			udpconn.start();

			checkVersion();

			new Thread(this, "GMCConnector_" + this.playername).start();;

			return true;
		} catch (IOException ex) {
			this.last_error = ex.getMessage();
			this.last_error_code = ErrorCodes.IO_ERROR;
		} catch (Exception ex) {
			this.last_error = ex.getMessage();
			this.last_error_code = ErrorCodes.MISC;
		}
		return false;
	}


	/**
	 * Join a game, which will be created if it doesn't exist.
	 * @return A boolean indicating success.
	 */
	public boolean joinGame() {
		if (sck == null) {
			throw new RuntimeException("Not connected!");
		}
		
		if (this.game_stage == GameStage.IN_PROGRESS) {
			p("Warning: joining a game before the last game has finished.");
		}
		
		// Reset values
		player_id = -1;
		players.clear();
		winner = -1;
		winner_name = null;
		game_stage = GameStage.WAITING_FOR_PLAYERS;

		try {
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_JOIN_GAME.getID());
				tcpconn.dos.writeUTF(this.playername);
				tcpconn.dos.writeUTF(this.gameid);
				tcpconn.dos.writeInt(min_players);
				tcpconn.dos.writeInt(max_players);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
			// Wait until we've got an id, so we know we were successful.
			long wait_until = System.currentTimeMillis() + 60000;
			while (this.player_id < 0) {
				Functions.delay(200);
				if (System.currentTimeMillis() > wait_until) {
					throw new IOException("Timed out waiting for server");
				}
			}
			return true;
		} catch (IOException ex) {
			this.last_error = ex.getMessage();
			this.last_error_code = ErrorCodes.IO_ERROR;
		}
		return false;
	}


	@Override
	public void run() {
		try {
			while (!stop_now) {
				while (tcpconn.dis.available() > 1) {
					DataCommand cmd = DataCommand.get(tcpconn.dis.readByte());
					//p("Got cmd: " + cmd.name());
					switch (cmd) {
					case S2C_PLAYER_ID:
						player_id = tcpconn.dis.readInt();
						byte check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						break;

					case S2C_VERSION_OK:
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						//p("Got OK");
						break;

					case S2C_ERROR:
						last_error_code = tcpconn.dis.readInt();
						last_error = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						client.error(last_error_code, last_error);
						throw new RuntimeException(last_error);

					case S2C_CURRENT_PLAYERS:
						byte len = tcpconn.dis.readByte();
						this.players.clear();
						for (int i=0 ; i<len ; i++) {
							ClientPlayerData cpd = new ClientPlayerData();
							cpd.id = tcpconn.dis.readInt();
							cpd.name = tcpconn.dis.readUTF();
							players.put(cpd.id, cpd);
						}
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						break;

					case S2C_NEW_PLAYER:
						String name = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						client.playerJoined(name);
						break;

					case S2C_GAME_STARTED:
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						game_stage = GameStage.IN_PROGRESS;
						client.gameStarted();
						break;

					case S2C_PLAYER_LEFT:
						name = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						client.playerLeft(name);
						break;


					case S2C_PING_ME:
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						synchronized (tcpconn.dos) {
							tcpconn.dos.writeByte(DataCommand.C2S_PING_RESPONSE.getID());
							tcpconn.dos.writeByte(Statics.CHECK_BYTE);
						}
						//ClientMain.p("Responded to ping");
						break;

					case S2C_TCP_KEYVALUE_DATA:
						int fromplayerid = tcpconn.dis.readInt();
						int code = tcpconn.dis.readInt();
						int value = tcpconn.dis.readInt();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						client.dataReceivedByTCP(fromplayerid, code, value);
						break;

					case S2C_TCP_STRING_DATA:
						fromplayerid = tcpconn.dis.readInt();
						String data = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						client.dataReceivedByTCP(fromplayerid, data);
						break;

					case S2C_TCP_BYTEARRAY_DATA:
						fromplayerid = tcpconn.dis.readInt();
						int ln = tcpconn.dis.readInt();
						byte b[] = new byte[ln];
						tcpconn.dis.read(b);
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						client.dataReceivedByTCP(fromplayerid, b);
						break;

					case S2C_GAME_OVER:
						winner = tcpconn.dis.readInt();
						this.winner_name = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						game_stage = GameStage.FINISHED;
						client.gameEnded(winner_name);
						break;						

					default:
						throw new IOException("Unknown cmd: " +cmd);
					}
				}
				if (this.server_responded_to_udpconn == false && this.player_id > 0) {
					this.sendUDPConn();
				}

				if (Statics.CHECK_SERVER_IS_ALIVE) {
					if (check_server_interval.hitInterval()) {
						this.checkServerIsAlive();
						if (last_server_alive_response_time > 0 && System.currentTimeMillis() - last_server_alive_response_time > Statics.SERVER_DIED_DURATION ) {
							client.serverDown(System.currentTimeMillis() - last_server_alive_response_time);
						}
					}
				}
				Functions.delay(200);
			}
			// Don't catch IOException here is we need it to drop out if we close the socket
		} catch (Exception ex) {
			//ex.printStackTrace();
			//JOptionPane.showMessageDialog(null, "Error: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
			client.error(0, ex.toString());
			if (ex instanceof IOException == false) {
				sendError(ex);
			}
		}
		internalDisconnect();

	}


	private void internalDisconnect() {
		this.stop_now = true;
		sendExit();
		if (sck != null) {
			try {
				sck.close();
			} catch (IOException e) {
				// Do nothing
			}
		}

	}


	public void disconnect() {
		stop_now = true;
	}


	/**
	 * Send data to all other players via TCP.
	 * @param code The code to send
	 * @param value The value.
	 */
	public void sendKeyValueDataByTCP(int code, int value) {
		try {
			//p("Sending basic data...");
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_TCP_KEYVALUE_DATA.getID());
				tcpconn.dos.writeInt(code);
				tcpconn.dos.writeInt(value);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Send data to all other players via TCP.
	 * @param data The string to send
	 */
	public void sendStringDataByTCP(String data) {
		try {
			//p("Sending basic data...");
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_TCP_STRING_DATA.getID());
				tcpconn.dos.writeUTF(data);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void sendByteArrayByTCP(byte[] data) {
		try {
			//p("Sending basic data...");
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_TCP_BYTEARRAY_DATA.getID());
				tcpconn.dos.writeInt(data.length);
				tcpconn.dos.write(data, 0, data.length);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Send data to all other players via UDP.
	 * @param code The code to send
	 * @param value The value.
	 */
	public void sendKeyValueDataByUDP(int code, int value) {
		try {
			DataArrayOutputStream daos = new DataArrayOutputStream();
			daos.writeByte(DataCommand.C2S_UDP_KEYVALUE_DATA.getID());
			daos.writeLong(System.currentTimeMillis());
			daos.writeUTF(gameid);
			daos.writeInt(player_id);
			daos.writeInt(code);
			daos.writeInt(value);
			daos.writeByte(Statics.CHECK_BYTE);
			this.udpconn.sendPacket(daos.getByteArray());
			daos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Send data to all other players via UDP.
	 * @param data The string to send
	 */
	public void sendStringDataByUDP(String data) {
		try {
			DataArrayOutputStream daos = new DataArrayOutputStream();
			daos.writeByte(DataCommand.C2S_UDP_STRING_DATA.getID());
			daos.writeLong(System.currentTimeMillis());
			daos.writeUTF(gameid);
			daos.writeInt(player_id);
			daos.writeUTF(data);
			daos.writeByte(Statics.CHECK_BYTE);
			this.udpconn.sendPacket(daos.getByteArray());
			daos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void sendByteArrayByUDP(byte b[]) {
		try {
			DataArrayOutputStream daos = new DataArrayOutputStream();
			daos.writeByte(DataCommand.C2S_UDP_BYTEARRAY_DATA.getID());
			daos.writeLong(System.currentTimeMillis());
			daos.writeUTF(gameid);
			daos.writeInt(player_id);
			daos.writeInt(b.length);
			daos.write(b, 0, b.length);
			daos.writeByte(Statics.CHECK_BYTE);
			this.udpconn.sendPacket(daos.getByteArray());
			daos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Tell the server that we are out of the game, e.g. we have died and run out of lives.
	 * 
	 */
	public void sendOutOfGame() {
		try {
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_OUT_OF_GAME.getID());
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	/**
	 * Tell the server that we are probably the winners as we have reached the end.
	 * Note that the server will confirm this or otherwise, as multiple clients could send this at the same time.
	 * 
	 */
	public void sendIAmTheWinner() {
		try {
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_WINNER.getID());
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void checkVersion() { 
		try {
			//p("Checking version...");
			synchronized (tcpconn.dos) {
				tcpconn.dos.writeByte(DataCommand.C2S_VERSION.getID());
				tcpconn.dos.writeInt(Statics.COMMS_VERSION);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * @return A boolean indicating if we were the winner in the last game. 
	 */
	public boolean areWeTheWinner() {
		if (this.getGameStage() == GameStage.FINISHED) {
			return this.player_id == winner;
		} else {
			throw new RuntimeException("Game not finished yet");
		}
	}


	/**
	 * Get the player name we provided at the start.
	 * @return The name
	 */
	public String getPlayerName() {
		return this.playername;
	}
	
	
	/**
	 * Gets the current game stage.
	 * @return The enum of the current game stage.
	 */
	public synchronized GameStage getGameStage() {
		return this.game_stage;
	}
	
	
	/**
	 * 
	 * @return The name of the winning player.
	 */
	public String getWinnersName() {
		if (this.getGameStage() == GameStage.FINISHED) {
			return this.winner_name;
		} else {
			throw new RuntimeException("Game not finished yet");
		}
	}


	private void sendError(Exception ex) {
		synchronized (tcpconn.dos) {
			try {
				tcpconn.dos.writeByte(DataCommand.C2S_SEND_ERROR.getID());
				String data = "Version:" + Statics.CODE_VERSION + "\n" + Functions.Throwable2String(ex); 
				tcpconn.dos.writeUTF(data);
				tcpconn.dos.writeByte(Statics.CHECK_BYTE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private void sendExit() {
		if (tcpconn != null) {
			if (tcpconn.isConnected()) {
				//p("Sending exit...");
				try {
					synchronized (tcpconn.dos) {
						tcpconn.dos.writeByte(DataCommand.C2S_DISCONNECTING.getID());
						tcpconn.dos.writeByte(Statics.CHECK_BYTE);
						tcpconn.dos.flush();
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}
	}


	private void sendUDPConn() throws IOException {
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.C2S_UDP_CONN.getID());
		daos.writeUTF(gameid);
		daos.writeInt(player_id);
		daos.writeByte(Statics.CHECK_BYTE);
		this.udpconn.sendPacket(daos.getByteArray());
		daos.close();
	}


	private void checkServerIsAlive() throws IOException {
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.C2S_CHECK_ALIVE.getID());
		daos.writeByte(Statics.CHECK_BYTE);
		this.udpconn.sendPacket(daos.getByteArray());
		daos.close();
	}


	/**
	 * 
	 * @return A string describing the last error.
	 */
	public String getLastError() {
		return this.last_error;
	}


	/**
	 * 
	 * @return The error code of the last error.  See ErrorCodes.java. 
	 */
	public int getLastErrorCode() {
		return this.last_error_code;
	}


	public static void p(String s) {
		System.out.println("Client: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	public static void pe(String s) {
		System.err.println("Client: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	public static void debug(String s) {
		if (Statics.DEBUG) {
			System.out.println("Debug: " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
		}
	}


	/**
	 * Wait for the game to finish.  Typically called if we've told the server that we're the winner
	 * or that we're out of the game.
	 */
	public void waitForStage(GameStage stage) {
		while (this.getGameStage() != stage) {
			Functions.delay(200);
		}
	}

	
	public void setLastServerResponseTime() {
		this.last_server_alive_response_time = System.currentTimeMillis();
	}
	
	
	public void setServerHasResponded() {
		this.server_responded_to_udpconn = true;
	}
	
	
	/**
	 * Get a map of the current players.  Note that this is often recreated.
	 * @return A map of the current players.
	 */
	public Map<Integer, ClientPlayerData> getCurrentPlayers() {
		return this.players;
	}
	
	
	/**
	 * Get a player's data by id.
	 * @param id
	 * @return The player's data.
	 */
	public ClientPlayerData getPlayerByID(int id) {
		return this.players.get(id);
	}

	
	/**
	 * @return a string show a list of all the current players.
	 */
	public String getCurrentPlayersAsString() {
		StringBuilder str = new StringBuilder();
		for (ClientPlayerData pd : this.players.values()) {
			str.append(pd.toString() + "\n");
			
		}
		return str.toString();
	}
	
}
