package com.scs.gmc;

public interface IGameClient {

	void waitingForPlayers();
	
	void playerJoined(String name);
	
	void gameStarted();
	
	void gameEnded();
	
	void dataReceived();
}
