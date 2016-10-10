package com.scs.gmc;

public interface IGameClient {

	void currentPlayers(ClientPlayerData players[]);
	
	void playerLeft(ClientPlayerData player, int numjoined);
	
	void playerJoined(ClientPlayerData player, int numjoined);
	
	void gameStarted();
	
	void gameEnded();
	
	void basicDataReceived(int fromplayerid, int i1, int i2);
}
