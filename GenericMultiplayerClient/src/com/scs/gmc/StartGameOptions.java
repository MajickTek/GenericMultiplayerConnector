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

package com.scs.gmc;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ssmith.awt.AWTFunctions;

/**
 * Helpful class which brings up a simple JFrame, for the user to enter details of which server to connect to.
 *
 */
public class StartGameOptions extends JFrame implements ActionListener, WindowListener {

	private static final long serialVersionUID = 1L;

	public static final String PUBLIC_IP = "178.62.91.22";

	private static final String FILENAME = "user.properties";
	// Code for properties
	private static final String SERVER = "server";
	private static final String PORT = "port";
	private static final String NAME = "name";
	private static final String GAME_CODE = "game_code";
	private static final String MIN_PLAYERS = "min_players";
	private static final String MAX_PLAYERS = "max_players";
	private static final String OTHER_IPS = "other_ips";

	public boolean OKClicked = false;

	private JComboBox txt_server = new JComboBox(new DefaultComboBoxModel(new String[] {PUBLIC_IP}));
	private JTextField txt_port = new JTextField();
	private JTextField txt_player_name = new JTextField();
	private JTextField txt_game_code = new JTextField();
	private JTextField txt_min_players = new JTextField();
	private JTextField txt_max_players = new JTextField();

	
	/**
	 * Utility function.  This will bring up a simple form asking for the users details, then connect to the server.
	 * If everything works, an instance of ConnectorMain will be returned, already connected.  If there is a connection problem, the user
	 * will be asked again.
	 * @param game_client 
	 * @return An instance of ConnectorMain that is already connected, or null if the user bailed.
	 */
	public static ConnectorMain ShowOptionsAndConnect(IGameClient game_client, String title) {
		while (true) {
			StartGameOptions options = new StartGameOptions(title);
			options.setVisible(true);
			synchronized (options) {
				try {
					options.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (options.OKClicked == false) {
				return null;
			}
			
			JDialog dialog = new JDialog();
			dialog.setTitle("Please Wait...");
			JLabel label = new JLabel("Please wait, connecting to " + options.getServer() + "...");
			dialog.setLocationRelativeTo(null);
			dialog.add(label);
			dialog.pack();
			dialog.setModal(false);
			dialog.setVisible(true);
			
			ConnectorMain connector = new ConnectorMain(game_client, options.getServer().trim(), options.getPort(), options.getPlayersName(), options.getGameCode(), options.getMinPlayers(), options.getMaxPlayers());
			if (connector.connect()) {
				dialog.setVisible(false);
				return connector;
			} else {
				dialog.setVisible(false);
				game_client.error(connector.getLastErrorCode(), connector.getLastError());
				int dialogResult = JOptionPane.showConfirmDialog (null, "Error connecting to server: " + connector.getLastError() + ".  Do you wish to retry?", "Error", JOptionPane.YES_NO_OPTION);
				if(dialogResult != JOptionPane.YES_OPTION){
					break;
				}
			}
		}
		return null;
	}


	public StartGameOptions(String title) {
		super();

		if (title != null) {
			this.setTitle(title);
		} else {
			this.setTitle("Options");
		}
		this.setLayout(new GridLayout());

		JPanel panel = new JPanel();

		panel.setLayout(new GridLayout(7, 2));

		//panel.add(new JLabel("Version"));
		//panel.add(new JLabel(Statics.CODE_VERSION + "/" + Statics.COMMS_VERSION));

		panel.add(new JLabel("Server IP"));
		panel.add(txt_server);
		panel.add(new JLabel("Port"));
		panel.add(txt_port);
		panel.add(new JLabel("Your Name"));
		panel.add(txt_player_name);
		panel.add(new JLabel("Game Code"));
		panel.add(txt_game_code);
		panel.add(new JLabel("Min Players"));
		panel.add(txt_min_players);
		panel.add(new JLabel("Max Players"));
		panel.add(txt_max_players);

		JButton submit = new JButton("Connect");
		panel.add(submit);
		submit.addActionListener(this);

		SwingUtilities.getRootPane(this).setDefaultButton(submit);

		this.add(panel);

		txt_server.setEditable(true);
		this.setResizable(false);

		//this.pack();
		this.setSize(300, 200);
		AWTFunctions.CentreWindow(this);

		try {
			Properties props = new Properties();
			File f = new File(FILENAME);
			if (f.exists()) {
				InputStream inStream = new FileInputStream( f );
				props.load(inStream);
				inStream.close();
				this.txt_server.getModel().setSelectedItem(props.getProperty(SERVER));
				this.txt_port.setText(props.getProperty(PORT));
				this.txt_player_name.setText(props.getProperty(NAME));
				this.txt_game_code.setText(props.getProperty(GAME_CODE));
				this.txt_min_players.setText(props.getProperty(MIN_PLAYERS));
				this.txt_max_players.setText(props.getProperty(MAX_PLAYERS));
				String other_ips = props.getProperty(OTHER_IPS);
				if (other_ips != null && other_ips.isEmpty() == false) {
					String split[] = other_ips.split(",");
					for (String s : split) {
						if (s.trim().isEmpty() == false) {
							if (!s.equalsIgnoreCase(PUBLIC_IP)) {
								this.txt_server.addItem(s.trim());
							}
						}
					}
				}
			} else {
				this.txt_server.getModel().setSelectedItem("127.0.0.1");//"178.62.91.22");
				this.txt_port.setText(""+Statics.DEF_PORT);
				this.txt_min_players.setText("2");
				this.txt_max_players.setText("99");

			}
		}
		catch (Exception e ) {
			e.printStackTrace();
		}

		this.addWindowListener(this);

	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (validateFields()) {
			saveProperties();
			OKClicked = true;
			this.setVisible(false);
			synchronized (this) {
				notify();
			}
		}
	}


	private boolean validateFields() {
		if (this.getServer().length() == 0) {
			JOptionPane.showMessageDialog(this, "Please enter a server name.");
			return false;
		}
		if (this.getPlayersName().length() == 0) {
			JOptionPane.showMessageDialog(this, "Please enter a players name.");
			return false;
		}
		if (this.getGameCode().length() == 0) {
			JOptionPane.showMessageDialog(this, "Please enter a game code.");
			return false;
		}
		try {
			if (this.getPort() <= 1024) {
				JOptionPane.showMessageDialog(this, "Please enter a valid port > 1024.");
				return false;
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Please enter a valid port.");
			return false;
		}
		try {
			if (this.getMinPlayers() < 1) {
				JOptionPane.showMessageDialog(this, "Please enter a minimum of 1 players.");
				return false;
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Please enter a valid minimum players.");
			return false;
		}
		try {
			this.getMaxPlayers();
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Please enter a valid maximum players.");
			return false;
		}
		return true;
	}


	private void saveProperties() {
		try {
			Properties props = new Properties();

			props.setProperty(SERVER, (String)this.txt_server.getSelectedItem());
			props.setProperty(PORT, this.txt_port.getText());
			props.setProperty(NAME, this.txt_player_name.getText());
			props.setProperty(GAME_CODE, this.txt_game_code.getText());
			props.setProperty(MIN_PLAYERS, this.txt_min_players.getText());
			props.setProperty(MAX_PLAYERS, this.txt_max_players.getText());

			String other_ips = props.getProperty(OTHER_IPS);
			String server = (String)this.txt_server.getSelectedItem();
			if (other_ips != null) {
			if (other_ips.indexOf(server) < 0 && !server.equalsIgnoreCase(PUBLIC_IP)) {
				props.setProperty(OTHER_IPS, other_ips + "," + server);
			}
			} else {
				if (!server.equalsIgnoreCase(PUBLIC_IP)) {
					props.setProperty(OTHER_IPS, server);
				}
			}

			File f = new File(FILENAME);
			OutputStream out = new FileOutputStream( f );
			props.store(out, "User properties");
			out.close();
		}
		catch (Exception e ) {
			e.printStackTrace();
		}
	}


	public String getServer() {
		return (String)this.txt_server.getSelectedItem();
	}


	public String getPlayersName() {
		return this.txt_player_name.getText().trim();
	}


	public String getGameCode() {
		return this.txt_game_code.getText().trim();
	}


	public int getPort() {
		return Integer.parseInt(this.txt_port.getText().trim());
	}


	public int getMinPlayers() {
		return Integer.parseInt(this.txt_min_players.getText().trim());
	}


	public int getMaxPlayers() {
		return Integer.parseInt(this.txt_max_players.getText().trim());
	}


	@Override
	public void windowActivated(WindowEvent arg0) {

	}


	@Override
	public void windowClosed(WindowEvent arg0) {
	}


	@Override
	public void windowClosing(WindowEvent arg0) {
		synchronized (this) {
			notify();
		}		
	}


	@Override
	public void windowDeactivated(WindowEvent arg0) {

	}


	@Override
	public void windowDeiconified(WindowEvent arg0) {

	}


	@Override
	public void windowIconified(WindowEvent arg0) {

	}


	@Override
	public void windowOpened(WindowEvent arg0) {

	}


}
