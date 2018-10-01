package com.scs.gmc.client;

import java.io.IOException;

/**
 * This interface needs to be implemented by a class from the game.
 *
 */
public interface IGameClient {

	void playerLeft(String name);
	
	void playerJoined(String name);
	
	/**
	 * This indicates that enough players have joined to start the game.
	 */
	void gameStarted();
	
	void gameEnded(String winner);
	
	void keyValueReceivedByTCP(int fromplayerid, int code, int value) throws IOException;
	
	void keyValueReceivedByUDP(long time, int fromplayerid, int code, int value) throws IOException;
	
	void stringReceivedByTCP(int fromplayerid, String data) throws IOException;
	
	void stringReceivedByUDP(long time, int fromplayerid, String data) throws IOException;
	
	void byteArrayReceivedByTCP(int fromplayerid, byte[] data) throws IOException;
	
	void byteArrayReceivedByUDP(long time, int fromplayerid, byte[] data) throws IOException;
	
	void objectReceivedByTCP(int fromplayerid, Object obj) throws IOException;
	
	void objectReceivedByUDP(long time, int fromplayerid, Object obj) throws IOException;
	
	void error(Throwable ex);
	
	void serverDown(long ms_since_response);
}
