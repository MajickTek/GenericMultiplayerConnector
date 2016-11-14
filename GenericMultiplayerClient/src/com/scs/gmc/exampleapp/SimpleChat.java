package com.scs.gmc.exampleapp;

import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

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
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		connector = StartGameOptions.ShowOptionsAndConnect(this, "Simple Chat", new SimpleStartGameOptions("Simple Chat", -1, -1));
		if (connector != null) {
			this.connector.joinGame();
			this.setVisible(true);
			this.appendChat("Hello " + connector.getPlayerName());
		}
	}

	@Override
	protected void sendChat(String chat) {
		try {
			this.connector.sendStringDataByTCP(this.connector.getPlayerName() + ": " + chat);
		} catch (IOException e) {
			this.appendChat(e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void playerLeft(String name) {
		this.appendChat(name + " has left"); // todo - showing this twice
	}

	@Override
	public void playerJoined(String name) {
		this.appendChat(name + " has joined");
	}

	@Override
	public void stringReceivedByTCP(int fromplayerid, String data) {
		this.appendChat(data);

	}

	@Override
	public void windowClosing(WindowEvent e) {
		this.connector.disconnect();
	}

	@Override
	public void gameStarted() {

	}


	@Override
	public void gameEnded(String winner) {
		this.appendChat("The chatroom has closed.");
	}


	@Override
	public void keyValueReceivedByTCP(int fromplayerid, int code, int value) {

	}


	@Override
	public void keyValueReceivedByUDP(long time, int fromplayerid, int code, int value) {

	}


	@Override
	public void byteArrayReceivedByTCP(int fromplayerid, byte[] data) {

	}


	@Override
	public void stringReceivedByUDP(long time, int fromplayerid, String data) {

	}


	@Override
	public void byteArrayReceivedByUDP(long time, int fromplayerid, byte[] data) {

	}


	@Override
	public void error(int error_code, String msg) {

	}


	@Override
	public void serverDown(long ms_since_response) {

	}

	@Override
	public void objectReceivedByTCP(int fromplayerid, Object obj) {
		
	}

	@Override
	public void objectReceivedByUDP(long time, int fromplayerid, Object obj) {
		
	}


}
