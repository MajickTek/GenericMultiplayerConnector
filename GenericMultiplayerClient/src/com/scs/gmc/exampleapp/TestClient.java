package com.scs.gmc.exampleapp;

import java.io.IOException;
import java.net.UnknownHostException;

import com.scs.gmc.ClientPlayerData;
import com.scs.gmc.ConnectorMain;
import com.scs.gmc.IGameClient;
import com.scs.gmc.Statics;

import ssmith.lang.Functions;

public class TestClient implements IGameClient {

	private ConnectorMain main;
	private boolean game_started = false;

	public TestClient() {
		try {
			main = new ConnectorMain(this, "127.0.0.01", Statics.DEF_PORT, "Jeff", "xyz", 1, 99);
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
	public void playerJoined(ClientPlayerData player) {
		System.out.println("Player joined.  There are now " + main.players.length + " + players");
	}


	@Override
	public void playerLeft(String name) {
		System.out.println("Player " + name + " left.  There are now " + main.players.length + " + players");
	}


	@Override
	public void gameStarted() {
		System.out.println("Game started!");
		game_started = true;		
	}


	@Override
	public void gameEnded(boolean you_won) {
		if (you_won) {
			System.out.println("Game finished.  I won!");
		} else {
			System.out.println("Game finished.  I lost");
		}
	}


	@Override
	public void basicDataReceived(int fromplayerid, int i1, int i2) {
		System.out.println("Data received from player " + fromplayerid + ".  " + i1 + "=" +i2);
	}

}
