package com.scs.gmc.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Iterator;

import com.scs.gmc.shared.DataCommand;
import com.scs.gmc.shared.Statics;

import ssmith.lang.DataArrayOutputStream;
import ssmith.lang.ErrorHandler;

public final class UDPConnectionListener extends Thread {

	private DatagramSocket socket;
	private volatile boolean stop_now = false;
	private final ServerMain main;
	private ErrorHandler error_handler;

	public static int next_packet_id = 0;

	public UDPConnectionListener(ServerMain _main, int port, ErrorHandler _handler) throws IOException {
		super(UDPConnectionListener.class.getSimpleName());

		main = _main;
		error_handler = _handler;

		socket = new DatagramSocket(port);
		socket.setSoTimeout(10000);

		this.setDaemon(true);

		ServerMain.p("Waiting for UDP on port " + port);
	}


	public void run() {
		try {
			while (stop_now == false) {
				try {
					DatagramPacket packet;
					byte[] buf = new byte[256];
					packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					//ServerMain.p("Received packet");
					DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
					DataCommand cmd = DataCommand.get(dis.readByte());
					switch(cmd) {
					case C2S_UDP_CONN:
						String gameid = dis.readUTF();
						int playerid = dis.readInt();
						byte check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						ServerGame game = main.getGame(gameid);
						if (game != null) {
							PlayerData player = game.players_by_id.get(playerid);
							if (player != null) {
								if (player.address == null) { // Haven't got the data yet
									player.address = packet.getAddress();
									player.port = packet.getPort();
								}

								// Send response
								DataArrayOutputStream bos = new DataArrayOutputStream();
								bos.write(DataCommand.S2C_UDP_CONN_OK.getID());
								bos.writeByte(Statics.CHECK_BYTE);
								byte b[] = bos.getByteArray();
								DatagramPacket sendPacket = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
								this.socket.send(sendPacket);
								bos.close();
							}
						}
						break;

					case C2S_CHECK_ALIVE:
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						DataArrayOutputStream bos = new DataArrayOutputStream();
						bos.write(DataCommand.S2C_I_AM_ALIVE.getID());
						bos.writeByte(Statics.CHECK_BYTE);
						byte b[] = bos.getByteArray();
						DatagramPacket sendPacket = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
						this.socket.send(sendPacket);
						bos.close();
						break;

					case C2S_UDP_KEYVALUE_DATA:
						long time = dis.readLong();
						gameid = dis.readUTF();
						playerid = dis.readInt();
						int code = dis.readInt();
						int value = dis.readInt();
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						DataArrayOutputStream daos = new DataArrayOutputStream();
						daos.writeByte(DataCommand.S2C_UDP_KEYVALUE_DATA.getID());
						daos.writeLong(time);
						daos.writeInt(playerid);
						daos.writeInt(code);
						daos.writeInt(value);
						daos.writeByte(Statics.CHECK_BYTE);
						this.sendPacketToAll(gameid, daos.getByteArray(), playerid);
						daos.close();
						break;

					case C2S_UDP_STRING_DATA:
						time = dis.readLong();
						gameid = dis.readUTF();
						playerid = dis.readInt();
						String data = dis.readUTF();
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						// Send it to all other clients
						daos = new DataArrayOutputStream();
						daos.writeByte(DataCommand.S2C_UDP_STRING_DATA.getID());
						daos.writeLong(time);
						daos.writeInt(playerid);
						daos.writeUTF(data);
						daos.writeByte(Statics.CHECK_BYTE);
						this.sendPacketToAll(gameid, daos.getByteArray(), playerid);
						daos.close();
						break;

					case C2S_UDP_BYTEARRAY_DATA:
						time = dis.readLong();
						gameid = dis.readUTF();
						playerid = dis.readInt();
						int len = dis.readInt();
						byte ba[] = new byte[len];
						dis.read(ba);
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						// Send it to all other clients
						daos = new DataArrayOutputStream();
						daos.writeByte(DataCommand.S2C_UDP_BYTEARRAY_DATA.getID());
						daos.writeLong(time);
						daos.writeInt(playerid);
						daos.writeInt(ba.length);
						daos.write(ba, 0, ba.length);
						daos.writeByte(Statics.CHECK_BYTE);
						this.sendPacketToAll(gameid, daos.getByteArray(), playerid);
						daos.close();
						break;

					case C2S_UDP_OBJECT_DATA:
						time = dis.readLong();
						gameid = dis.readUTF();
						playerid = dis.readInt();
						len = dis.readInt();
						ba = new byte[len];
						dis.read(ba);
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}

						// Send it to all other clients
						daos = new DataArrayOutputStream();
						daos.writeByte(DataCommand.S2C_UDP_OBJECT_DATA.getID());
						daos.writeLong(time);
						daos.writeInt(playerid);
						daos.writeInt(ba.length);
						daos.write(ba, 0, ba.length);
						daos.writeByte(Statics.CHECK_BYTE);
						this.sendPacketToAll(gameid, daos.getByteArray(), playerid);
						daos.close();
						break;

					}

				} catch (SocketTimeoutException ex) {
					// Loop around
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		} catch (Exception ex) {
			this.error_handler.handleError(ex);
		} finally {
			socket.close();
		}
	}


	public void sendPacketToAll(String gameid, byte sendData[], int exceptplayerid) throws IOException {
		ServerGame game = main.getGame(gameid);
		if (game != null) {
			synchronized (game.players_by_id) {
				Iterator<PlayerData> it = game.players_by_id.values().iterator();
				while (it.hasNext()) {
					PlayerData pd = it.next();
					if (pd.id != exceptplayerid && pd != null && pd.address != null) { // Might be null if player's data hasn't been created
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, pd.address, pd.port);
						this.socket.send(sendPacket);
						//ServerMain.p("Sent UDP packet to " + pd.name);
					}
				}
			}
		}
	}


	public void stopNow() {
		this.interrupt();
		this.stop_now = true;
	}

}
