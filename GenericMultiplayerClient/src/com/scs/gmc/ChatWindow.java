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
    
    GenericMultiplayerConnector (C)Stephen Carlyle-Smith

 */

package com.scs.gmc;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


/**
 * Simple form for getting user's chat input.
 * 
 *
 */
public abstract class ChatWindow extends JFrame implements KeyListener, WindowListener, FocusListener {
	
	private static final String ECH = "[Enter chat here]";
	private static final int MAX_LENGTH = 2000;
	
	private StringBuilder str = new StringBuilder();
	private JTextArea textarea = new JTextArea();
	private JTextField textbox = new JTextField(ECH);

	public ChatWindow() {
		super();

		this.setTitle("TT Chat");//Statics.TITLE + " - Chat");
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(textarea), BorderLayout.CENTER);
		this.add(textbox, BorderLayout.SOUTH);
		this.setSize(300, 400);
		
		textarea.setLineWrap(true);
		textarea.setWrapStyleWord(true);
		textarea.setEditable(false);

		this.addWindowListener(this);
		textbox.addKeyListener(this);
		textbox.addFocusListener(this);
		
	}


	protected abstract void sendChat(String s);

	
	public void appendChat(String chat) {
		str.append(chat + "\n");
		
		while (str.length() > MAX_LENGTH) {
			int pos = str.indexOf("\n");
			str.delete(0, pos+1);
		}
		
		this.textarea.setText(str.toString());
		textarea.setCaretPosition(textarea.getDocument().getLength());

	}


	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == '\n') {//KeyEvent.VK_ENTER) {
			sendChat(this.textbox.getText());
			this.textbox.setText("");
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {

	}


	@Override
	public void windowOpened(WindowEvent e) {
	}


	@Override
	public void windowClosing(WindowEvent e) {

	}


	@Override
	public void windowClosed(WindowEvent e) {

	}


	@Override
	public void windowIconified(WindowEvent e) {

	}


	@Override
	public void windowDeiconified(WindowEvent e) {

	}


	@Override
	public void windowActivated(WindowEvent e) {

	}


	@Override
	public void windowDeactivated(WindowEvent e) {

	}


	@Override
	public void focusGained(FocusEvent arg0) {
		if (this.textbox.getText().equalsIgnoreCase(ECH)) {
			this.textbox.setText("");
		}
		
	}


	@Override
	public void focusLost(FocusEvent e) {
		
	}

}
