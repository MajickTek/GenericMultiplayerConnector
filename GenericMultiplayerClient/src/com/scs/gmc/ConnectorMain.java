package com.scs.gmc;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import javax.swing.JOptionPane;

import ssmith.lang.DataArrayOutputStream;
import ssmith.lang.Dates;
import ssmith.lang.Functions;
import ssmith.util.Interval;

public class ConnectorMain implements Runnable {

	public long last_server_alive_response_time;
	public boolean server_responded_to_udpconn = false;
	protected IGameClient client;
	private TCPConnection tcpconn;
	private UDPConnection udpconn;
	private volatile boolean stop_now = false;
	private int player_id = -1;
	private Interval check_server_interval = new Interval(Statics.CHECK_SERVER_ALIVE_INTERVAL);
	private String playername, server;
	private int port;
	public String gameid;
	public int min_players, max_players;
	public ClientPlayerData players[];

	public ConnectorMain(IGameClient _client, String _server, int _port, String _playername, String _gameid, int _min_players, int _max_players) throws UnknownHostException, IOException {
		super();

		client = _client;
		playername = _playername;
		server = _server;
		port = _port;
		gameid = _gameid;
		min_players = _min_players;
		max_players = _max_players;
		
	}
	
	
	public void joinGame() {
		new Thread(this).start();
	}
	
	
	public void run() {
		try {
			Socket sck = new Socket(server, port);
			tcpconn = new TCPConnection(sck);
			udpconn = new UDPConnection(this, server, port);
			udpconn.start();

			checkVersion();
			sendData();

			while (!stop_now) {
				//long start = System.currentTimeMillis();

				while (tcpconn.dis.available() > 1) {
					DataCommand cmd = DataCommand.get(tcpconn.dis.readByte());
					if (Statics.VERBOSE) {
						p("Got cmd: " + cmd.name());
					}
					switch (cmd) {
					case S2C_OK:
						byte check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						p("Got OK");
						break;
						
					case S2C_ERROR:
						String err = tcpconn.dis.readUTF();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						throw new RuntimeException("Error: " + err);
						
					case S2C_PLAYER_ID:
						player_id = tcpconn.dis.readInt();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						break;
						
					case S2C_CURRENT_PLAYERS:
						byte len = tcpconn.dis.readByte();
						this.players = new ClientPlayerData[len];
						for (int i=0 ; i<len ; i++) {
							ClientPlayerData cpd = new ClientPlayerData();
							cpd.id = tcpconn.dis.readInt();
							cpd.name = tcpconn.dis.readUTF();
							players[i] = cpd;
						}
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						break;
						
					case S2C_NEW_PLAYER:
						int id = tcpconn.dis.readInt();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						
						client.playerJoined(players[0]); // todo - find by id
						break;
						
					case S2C_GAME_STARTED:
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						
						client.gameStarted();
						break;
						
					case S2C_PLAYER_LEFT:
						String name = tcpconn.dis.readUTF();
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

					case S2C_RAW_DATA:
						int fromplayerid = tcpconn.dis.readInt();
						int i1 = tcpconn.dis.readInt();
						int i2 = tcpconn.dis.readInt();
						check = tcpconn.dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						
						client.basicDataReceived(fromplayerid, i1, i2);
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
							pe("Server seems to have died?");
						}
					}
				}
				Functions.delay(200);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
			if (ex instanceof IOException == false) {
				sendError(ex);
			}
		}
		sendExit();
		udpconn.stopNow();


	}


	// TCP requests
	public void sendBasicDataByTCS(int i1, int i2) throws IOException {
		p("Sending basic data...");
		synchronized (tcpconn.dos) {
			tcpconn.dos.writeByte(DataCommand.C2S_RAW_DATA.getID());
			tcpconn.dos.writeInt(this.player_id);
			tcpconn.dos.writeInt(i1);
			tcpconn.dos.writeInt(i2);
			tcpconn.dos.writeByte(Statics.CHECK_BYTE);
		}
	}

	
	public void sendBasicDataByUDP(int i1, int i2) throws IOException {
		DataArrayOutputStream daos = new DataArrayOutputStream();
		daos.writeByte(DataCommand.C2S_RAW_DATA.getID());
		daos.writeUTF(gameid);
		daos.writeInt(player_id);
		daos.writeInt(i1);
		daos.writeInt(i2);
		daos.writeByte(Statics.CHECK_BYTE);
		this.udpconn.sendPacket(daos.getByteArray());
		daos.close();
	}




	private void checkVersion() throws IOException {
		p("Checking version...");
		synchronized (tcpconn.dos) {
			tcpconn.dos.writeByte(DataCommand.C2S_VERSION.getID());
			tcpconn.dos.writeInt(Statics.COMMS_VERSION);
			tcpconn.dos.writeByte(Statics.CHECK_BYTE);
		}
	}


	private void sendData() throws IOException {
		//p("Sending our data...");
		synchronized (tcpconn.dos) {
			tcpconn.dos.writeByte(DataCommand.C2S_JOIN_GAME.getID());
			tcpconn.dos.writeUTF(this.playername);
			tcpconn.dos.writeUTF(this.gameid);
			tcpconn.dos.writeInt(min_players);
			tcpconn.dos.writeInt(max_players);
			tcpconn.dos.writeByte(Statics.CHECK_BYTE);
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


	private void sendExit() {//throws IOException {
		if (tcpconn.isConnected()) {
			p("Sending exit...");
			try {
				synchronized (tcpconn.dos) {
					tcpconn.dos.writeByte(DataCommand.C2S_EXIT.getID());
					tcpconn.dos.writeByte(Statics.CHECK_BYTE);
				}
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
	}


	// UDP requests
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


	public void stopNow() {
		this.stop_now = true;
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



}
