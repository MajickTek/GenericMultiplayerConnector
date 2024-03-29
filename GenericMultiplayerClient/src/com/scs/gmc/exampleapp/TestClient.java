package com.scs.gmc.exampleapp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import com.scs.gmc.client.ConnectorMain;
import com.scs.gmc.client.ConnectorMain.GameStage;
import com.scs.gmc.client.IGameClient;
import com.scs.gmc.shared.Statics;

import ssmith.lang.Dates;
import ssmith.lang.Functions;

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
		connector = new ConnectorMain(this, "127.0.0.1", Statics.DEFAULT_PORT, name, gamecode, MIN_PLAYERS, MAX_PLAYERS);
	}


	@Override
	public void run() {
		try {
			connector.connect(); // Connect to the server
			for (int i=0 ; i<3 ; i++) { // Run through 3 games
				connector.joinGame();
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
			}

			// All done, so disconnect. 
			connector.disconnect();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


	@Override
	public void playerJoined(String name) {
		p("Player '" + name + "' joined.  There are now " + connector.getNumPlayers() + " players");
	}


	@Override
	public void playerLeft(String name) {
		p("Player '" + name + "' left.  There are now " + connector.getNumPlayers() + " players");
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
	public void keyValueReceivedByTCP(int fromplayerid, int key, int value) {
		p("Data received from player " + fromplayerid + ".  " + key + "=" + value);
	}


	@Override
	public void keyValueReceivedByUDP(long time, int fromplayerid, int key, int value) {
		p("Data received from player " + fromplayerid + ".  " + key + "=" +value);
	}


	@Override
	public void stringReceivedByTCP(int fromplayerid, String data) {
		p("Data received from player " + fromplayerid + ".  " + data);
	}


	@Override
	public void stringReceivedByUDP(long time, int fromplayerid, String data) {
		p("Data received from player " + fromplayerid + ".  " + data);
	}


	@Override
	public void byteArrayReceivedByTCP(int fromplayerid, byte[] data) {
		p("Data: " + data[0] + ", " + data[1]);
	}


	@Override
	public void byteArrayReceivedByUDP(long time, int fromplayerid, byte[] data) {
		p("Data: " + data[0] + ", " + data[1]);
	}


	@Override
	public void serverDown(long ms_since_response) {
		pe("Server down!  " + ms_since_response + "ms since last response");
	}


	@Override
	public void error(Throwable ex) {
		System.err.println("Error! " + ex.getMessage());
	}


	private void p(String s) {
		System.out.println(connector.getPlayerName() + ": " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	private void pe(String s) {
		System.err.println(connector.getPlayerName() + ": " + Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + "-" + s);
	}


	@Override
	public void objectReceivedByTCP(int fromplayerid, Object obj) {

	}


	@Override
	public void objectReceivedByUDP(long time, int fromplayerid, Object obj) {

	}


}
