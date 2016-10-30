package com.scs.gmc.exampleapp;

import java.awt.event.WindowEvent;

import com.scs.gmc.ChatWindow;
import com.scs.gmc.ConnectorMain;
import com.scs.gmc.IGameClient;
import com.scs.gmc.SimpleStartGameOptions;
import com.scs.gmc.StartGameOptions;

public class SimpleChat extends ChatWindow implements IGameClient {

	private ConnectorMain connector; 

	public static void main(String[] args) {
		new SimpleChat();
	}
	
	public SimpleChat() {
		connector = StartGameOptions.ShowOptionsAndConnect(this, "Multiplayer Tetris", new SimpleStartGameOptions("Simple Chat"));
		if (connector == null) {
			// User pressed cancel or connection failed.
			System.exit(0);
		}
		this.connector.joinGame();
		this.setVisible(true);
	}

	
	@Override
	protected void sendChat(String s) {
		this.connector.sendStringDataByTCP(this.connector.getPlayerName() + ": " + s);
	}


	@Override
	public void playerLeft(String name) {
		this.appendChat(name + " has left");
		
	}


	@Override
	public void playerJoined(String name) {
		this.appendChat(name + " has joined");
		
	}


	@Override
	public void gameStarted() {
		
	}


	@Override
	public void gameEnded(String winner) {
		
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, int code, int value) {
		
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, int code, int value) {
		
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, String data) {
		this.appendChat(data);
		
	}


	@Override
	public void dataReceivedByTCP(int fromplayerid, byte[] data) {
		
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, String data) {
		
	}


	@Override
	public void dataReceivedByUDP(long time, int fromplayerid, byte[] data) {
		
	}


	@Override
	public void error(int error_code, String msg) {
		
	}


	@Override
	public void serverDown(long ms_since_response) {
		
	}


	@Override
	public void windowClosing(WindowEvent e) {
		this.connector.disconnect();
	}


}
