package com.scs.gmc;

public interface IGameClient {

	void playerLeft(String name);
	
	void playerJoined(ClientPlayerData player);
	
	void gameStarted();
	
	void gameEnded(boolean you_won); // todo - use, todo - reason (no-one left, won)
	
	void basicDataReceived(int fromplayerid, int i1, int i2);
	
	void error(String msg);
}
