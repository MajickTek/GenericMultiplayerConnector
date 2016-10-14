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

package com.scs.gmc.exampleapp;


import java.util.Calendar;
import java.util.Random;

import ssmith.lang.Dates;
import ssmith.lang.Functions;

import com.scs.gmc.ConnectorMain;
import com.scs.gmc.ConnectorMain.GameStage;
import com.scs.gmc.IGameClient;
import com.scs.gmc.Statics;

/**
 * This is an example of the simplest usage of GMC. 
 *
 */
public class TestClient implements Runnable, IGameClient {

	private static final int SCORE_CODE = 1;

	private static final int MIN_PLAYERS = 2;
	private static final int MAX_PLAYERS = 99;

	private ConnectorMain connector;

	public static void main(String[] args) {
		new Thread(new TestClient("Jeff", "XYZ"), "TestClient_Jeff").start();
		new Thread(new TestClient("Bill", "XYZ"), "TestClient_Bill").start();
		//new Thread(new TestClient("Jeff2", "XYZ2"), "TestClient2_Jeff").start();
		//new Thread(new TestClient("Bill2", "XYZ2"), "TestClient2_Bill").start();

		/*for (int num_players=0 ; num_players<10 ; num_players++) {
			new Thread(new TestClient("P" + num_players, "XYZ"), "PlayerThread"+num_players).start();
		}*/

	}


	public TestClient(String name, String gamecode) {
		// Create the connector
		connector = new ConnectorMain(this, "127.0.0.1", Statics.DEF_PORT, name, gamecode, MIN_PLAYERS, MAX_PLAYERS);
	}


	@Override
	public void run() {
		boolean success = connector.connect(); // Connect to the server
		if (success) {
			for (int i=0 ; i<3 ; i++) { // Run through 3 games
				success = connector.joinGame();
				if (success) {

					// Wait for enough players to join
					while (connector.getGameStage() == GameStage.WAITING_FOR_PLAYERS) {
						Functions.delay(200);
					}

					p("Current Players:\n" + connector.getCurrentPlayersAsString());
					p("Starting game");

					// Play a very simple game where we add a random number to our score.
					byte score = 0;
					// For testing sending byte arrays
					byte b[] = new byte[2];
					b[0] = SCORE_CODE;
					while (connector.getGameStage() == GameStage.IN_PROGRESS) {
						score += new Random().nextInt(10);

						// Send our data to the server
						b[1] = score;
						connector.sendByteArrayByTCP(b);
						connector.sendByteArrayByUDP(b);
						connector.sendKeyValueDataByTCP(SCORE_CODE, score);
						connector.sendKeyValueDataByUDP(SCORE_CODE, score);
						connector.sendStringDataByTCP("Hello from " + this.connector.getPlayerName());
						connector.sendStringDataByUDP("Hello from " + this.connector.getPlayerName());

						// Have we won?
						if (score >= 100) {
							p("I have won!");
							connector.sendIAmTheWinner();
							break;
						}
						Functions.delay(500);
					}
					connector.waitForStage(GameStage.FINISHED); // To have the winner confirmed, since two clients might finished at the same time.
					p("Finished game.  The winner was " + connector.getWinnersName());
				} else {
					pe("Error: " + connector.getLastError());
				}
			}
		} else {
			pe("Error: " + connector.getLastError());
		}

		// All done, so disconnect. 
		connector.disconnect();
	}


	@Override
	public void playerJoined(String name) {
		p("Player '" + name + "' joined.  There are now " + connector.getCurrentPlayers().size() + " players");
	}


	@Override
	public void playerLeft(String name) {
		p("Player '" + name + "' left.  There are now " + connector.getCurrentPlayers().size() + " players");
	}


	@Override
	public void gameStarted() {
		p("Game started!");
	}


	@Override
	public void gameEnded(String name) {
		p("Game finished.  " + name + " won the game!");
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, int key, int value) {
		p("Data received from player " + fromplayerid + ".  " + key + "=" + value);
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, int key, int value) {
		p("Data received from player " + fromplayerid + ".  " + key + "=" +value);
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, String data) {
		p("Data received from player " + fromplayerid + ".  " + data);
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, String data) {
		p("Data received from player " + fromplayerid + ".  " + data);
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, byte[] data) {
		p("Data: " + data[0] + ", " + data[1]);
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, byte[] data) {
		p("Data: " + data[0] + ", " + data[1]);
	}


	@Override
	public void serverDown(long ms_since_response) {
		pe("Server down!  " + ms_since_response + "ms since last response");
	}


	@Override
	public void error(int error_code, String msg) {
		System.err.println("Error! " + msg + " (" + error_code + ")");
	}


	private void p(String s) {
		System.out.println(connector.getPlayerName() + ": " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	private void pe(String s) {
		System.err.println(connector.getPlayerName() + ": " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


}
