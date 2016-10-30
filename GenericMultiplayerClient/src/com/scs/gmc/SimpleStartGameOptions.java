package com.scs.gmc;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ssmith.awt.AWTFunctions;

public class SimpleStartGameOptions extends StartGameOptions {

	private static final long serialVersionUID = 1L;

	public SimpleStartGameOptions(String title) {
		super(title);

		this.remove(panel); // Clear all stuff
		
		panel = new JPanel();

		panel.setLayout(new GridLayout(5, 2));

		panel.add(new JLabel("Server IP"));
		panel.add(txt_server);
		panel.add(new JLabel("Port"));
		panel.add(txt_port);
		panel.add(new JLabel("Your Name"));
		panel.add(txt_player_name);
		panel.add(new JLabel("Game Code"));
		panel.add(txt_game_code);

		JButton submit = new JButton("Connect");
		panel.add(submit);
		submit.addActionListener(this);

		SwingUtilities.getRootPane(this).setDefaultButton(submit);

		this.add(panel);

		txt_server.setEditable(true);
		this.showDefaultValues();
		this.setResizable(false);

		this.setSize(300, 200);
		AWTFunctions.CentreWindow(this);

		
	}



}