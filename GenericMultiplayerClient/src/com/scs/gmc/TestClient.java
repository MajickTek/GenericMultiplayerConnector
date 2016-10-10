package com.scs.gmc;

import java.io.IOException;
import java.net.UnknownHostException;

import ssmith.lang.Functions;

public class TestClient implements IGameClient { // todo - change package

	ClientMain main;
	private boolean game_started = false;
	
	public TestClient() {
		try {
			main = new ClientMain(this, "127.0.0.01", Statics.DEF_PORT, "Jeff", "xyz", 1, 99);
			main.joinGame();
			while (!game_started) {
				Functions.delay(200);
			}
			main.sendBasicDataByTCS(1, 2);
			main.sendBasicDataByUDP(1, 2);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		new TestClient();

	}


	@Override
	public void currentPlayers(ClientPlayerData players[]) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void playerJoined(ClientPlayerData player, int numjoined) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void playerLeft(ClientPlayerData player, int numjoined) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void gameStarted() {
		game_started = true;		
	}


	@Override
	public void gameEnded() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void basicDataReceived(int fromplayerid, int i1, int i2) {
		// TODO Auto-generated method stub
		
	}

}
