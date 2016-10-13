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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public final class ClientUDPConnection extends Thread {

	private DatagramSocket socket;
	private InetAddress address;
	private int port;
	private volatile boolean stop_now = false;
	private ConnectorMain main;

	public static int next_packet_id = 0;

	public ClientUDPConnection(ConnectorMain _main, String server, int _port, String name) throws IOException {
		super("UDPConnection_" + name);

		this.setDaemon(true);

		main = _main;

		address = InetAddress.getByName(server);
		socket = new DatagramSocket();
		port = _port;

		socket.setSoTimeout(10000);
	}


	public void run() {
		try {
			while (stop_now == false) {
				try {
					DatagramPacket packet;
					byte[] buf = new byte[256];
					packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
					DataCommand cmd = DataCommand.get(dis.readByte()); // todo - check sender is the server?
					//ConnectorMain.p("Rcvd new packet: " + cmd + "(len=" + packet.getLength() + ")");
					switch (cmd) {
					case S2C_UDP_CONN_OK:
						byte check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						main.setServerHasResponded();
						break;
						
					case S2C_I_AM_ALIVE:
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						main.setLastServerResponseTime();
						break;
						
					case S2C_UDP_RAW_DATA:
						long time = dis.readLong();
						int fromplayerid = dis.readInt();
						int code = dis.readInt();
						int value = dis.readInt();
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						
						main.client.dataReceivedByUDP(time, fromplayerid, code, value);
						break;
						
					case S2C_UDP_STRING_DATA:
						time = dis.readLong();
						fromplayerid = dis.readInt();
						String data = dis.readUTF();
						check = dis.readByte();
						if (check != Statics.CHECK_BYTE) {
							throw new IOException("Invalid check byte");
						}
						
						main.client.dataReceivedByUDP(time, fromplayerid, data);
						break;
						
					default:
						//throw new IllegalArgumentException("Unknown command: " + cmd);
						// Ignore it
					}
				} catch (SocketTimeoutException ex) {
					//ex.printStackTrace();
					// Loop around
				}
			}
			// DOn't catch IOException as we need it to drop out if the socket is closed.
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			socket.close();
		}
	}


	public void sendPacket(byte sendData[]) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		socket.send(sendPacket);
	}


	public void stopNow() {
		this.interrupt();
		this.stop_now = true;
	}

}
