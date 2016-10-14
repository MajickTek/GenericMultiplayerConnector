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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.scs.gmc.ConnectorMain;
import com.scs.gmc.ConnectorMain.GameStage;
import com.scs.gmc.IGameClient;
import com.scs.gmc.StartGameOptions;

public class MultiplayerTetris extends JFrame {

	// Comms codes
	public static final int CODE_LINE_CLEARED = 1;

	private static final long serialVersionUID = 1L;

	private ConnectorMain connector; 
	private MyPanel canvas = new MyPanel(20, 12, 8);
	private JTextArea textarea;

	public static void main(String[] args) {
		new MultiplayerTetris();
	}

	public MultiplayerTetris() {
		this.setSize(400,350);
		this.setTitle("Multiplayer Tetris");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setLayout(null);
		this.add(canvas);

		textarea = new JTextArea();
		textarea.setWrapStyleWord(true);
		textarea.setEditable(false);
		textarea.setLineWrap(true);
		JScrollPane sp = new JScrollPane(textarea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBounds(canvas.getLocation().x + canvas.getWidth()+10, 10, this.getWidth() - canvas.getHeight(), this.getHeight()-40);
		this.add(sp);

		connector = StartGameOptions.ShowOptionsAndConnect(canvas);
		if (connector == null) {
			// User pressed cancel or connection failed.
			System.exit(0);
		}
		textarea.append("Connected to server\n");
		textarea.append("Hello " + connector.getPlayerName() + "\n");

		//canvas.setPosition(109, 20);
		canvas.setFocusable(true);
		canvas.addKeyListener(canvas);
		canvas.requestFocusInWindow();

		Thread canvasUpdateThread = new Thread(canvas, this.getClass().getSimpleName() + "_Thread");
		canvasUpdateThread.start();
	}


	class MyPanel extends JPanel implements KeyListener, Runnable, IGameClient {

		private static final long serialVersionUID = 1L;

		private static final int EMPTY = 0;
		private static final int FILLED = 1;

		private int cell_size;
		private int row_cells;
		private int col_cells;
		private int[][] data;
		private int xFigureOffset = 0;
		private int yFigureOffset = 0;
		private Shape current_shape;

		private boolean game_over = false;

		private final int LOWER_TIME = 100;
		private final int NORMAL_TIME = 500;
		private int timeInterval = NORMAL_TIME;
		private int current_speedup = 0;
		private final int SPEEDUP_INC = 20;

		public MyPanel(int _cell_size, int _row_cells, int _col_cells) {
			super();

			cell_size = _cell_size;
			row_cells = _row_cells;
			col_cells = _col_cells;

			data = new int[row_cells][col_cells];
			this.setBounds(10,10, _col_cells*_cell_size+1, _row_cells*_cell_size+1);
			this.appendNewTFigure();
		}


		@Override
		public void run() {
			connector.joinGame();
			textarea.append("Waiting for other players...\n");
			connector.waitForStage(GameStage.IN_PROGRESS);
			textarea.append("Game started!\n");

			while(!game_over && connector.getGameStage() == GameStage.IN_PROGRESS) {
				try {
					Thread.sleep(timeInterval - current_speedup);
				} catch (InterruptedException e) {}

				tryMoveDown();
				repaint();
			}

			textarea.append("Game ended - waiting for server\n");
			connector.waitForStage(GameStage.FINISHED);
			if (connector.areWeTheWinner()) {
				textarea.append("You won!\n");
				JOptionPane.showMessageDialog(this, "You won!");
			} else {
				textarea.append("The winner was " + connector.getWinnersName() + "\n");
				JOptionPane.showMessageDialog(this, "The winner was " + connector.getWinnersName());
			}
			textarea.append("Please restart to play again.\n");
		}


		@Override
		protected void paintComponent(Graphics g) {
			drawBackground(g, Color.white);
			drawData(g);
			if(!game_over) {
				drawCurrentShape(g);
			}
			drawGrid(g);
		}

		public void appendNewTFigure() {
			xFigureOffset = 0;
			yFigureOffset = 0;
			current_shape = new Shape();
		}

		private void drawBackground(Graphics g, Color color) {
			g.setColor(color);
			g.fillRect(0,0,getWidth(), getHeight());
		}

		private void drawGrid(Graphics g) {
			g.setColor(Color.BLACK);
			for (int i = 0; i < getWidth(); i+=cell_size) {
				g.drawLine(i, 0, i, getHeight());
			}
			for (int i = 0; i < getHeight(); i+=cell_size) {
				g.drawLine(0, i, getWidth(), i);
			}
		}

		private void drawData(Graphics g) {
			g.setColor(Color.RED);
			for (int i = 0; i < data.length; i++) {
				for (int j = 0; j < data[i].length; j++) {
					if(data[i][j] == FILLED) {
						g.fillRect(j * cell_size, i * cell_size, cell_size, cell_size);
					}
				}
			}
		}

		private void drawCurrentShape(Graphics g) {
			g.setColor(Color.CYAN);
			for (int i = 0; i < current_shape.getCurrentDataBlock().length; i++) {
				for (int j = 0; j < current_shape.getCurrentDataBlock()[i].length; j++) {
					if(current_shape.getCurrentDataBlock()[i][j] == FILLED) {
						g.fillRect((xFigureOffset + j) * cell_size, (yFigureOffset + i) * cell_size, cell_size, cell_size);
					}
				}
			}
		}

		public void setPosition(int x, int y) {
			this.setBounds(x, y, col_cells*cell_size+1, row_cells*cell_size+1);
		}

		public void keyTyped(KeyEvent e) {
			// Do nothing
		}

		public void keyPressed(KeyEvent e) {
			if(!game_over) {
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					tryRotateLeft();
				}
				if(e.getKeyCode() == KeyEvent.VK_E || e.getKeyCode() == KeyEvent.VK_UP) {
					tryRotateRight();
				}
				if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
					tryMoveLeft();
				}
				if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
					tryMoveRight();
				}
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					tryRotateRight();
				}
				if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) {
					timeInterval = LOWER_TIME;
				}
			}
		}

		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) {
				timeInterval = NORMAL_TIME;
			}
		}

		private boolean canBeHere(int x_fig_offs, int y_fig_offs, int[][] fig_data) {
			boolean can = true;
			for (int i = 0; i < fig_data.length; i++) {
				for (int j = 0; j < fig_data[i].length; j++) {
					if (fig_data[i][j]==FILLED) {
						if ((j+x_fig_offs >= col_cells)  || (j+x_fig_offs < 0) || (i+y_fig_offs >= row_cells)) {
							can = false;
						} else if (data[i+y_fig_offs][j+x_fig_offs] == FILLED) {
							can = false;
						}
					}
				}
			}
			return can;
		}


		private void tryMoveRight() {
			if (canBeHere(xFigureOffset+1, yFigureOffset, current_shape.getCurrentDataBlock())) {
				xFigureOffset++;
			}
			repaint();
		}

		private void tryMoveLeft() {
			if (canBeHere(xFigureOffset-1, yFigureOffset, current_shape.getCurrentDataBlock())) {
				xFigureOffset--;
			}
			repaint();
		}

		private void tryMoveDown() {
			if (current_shape != null) {
				if (canBeHere(xFigureOffset, yFigureOffset+1, current_shape.getCurrentDataBlock())) {
					yFigureOffset++;
				} else {
					addCurrentDataBlockToMainData();
					tryClearLines();
					timeInterval = NORMAL_TIME;

					xFigureOffset = 0;
					yFigureOffset = 0;
					current_shape = new Shape();
					if(!canBeHere(xFigureOffset, yFigureOffset, current_shape.getCurrentDataBlock())) {
						gameOver();
					}
				}
			}
		}

		private void tryClearLines() {
			int[][] buf_data = new int[row_cells][col_cells];
			boolean clear_this_line;

			int buf_index = row_cells-1;
			for (int i = row_cells-1; i >= 0; i--) {
				clear_this_line = true;
				for (int j = 0; j < col_cells; j++) {
					if(data[i][j] == FILLED) {
						buf_data[buf_index][j] = FILLED;
					} else {
						buf_data[buf_index][j] = EMPTY;
						clear_this_line = false;
					}
				}
				if (!clear_this_line) {
					buf_index--;
				} else {
					connector.sendKeyValueDataByTCP(CODE_LINE_CLEARED, 1);
				}
			}
			data = buf_data;
		}

		private void gameOver() {
			System.out.println("GAME IS OVER");
			connector.sendOutOfGame();
			game_over = true;
			addCurrentDataBlockToMainData();
			current_shape = null;
		}

		private void addCurrentDataBlockToMainData() {
			for (int i = 0; i < current_shape.getCurrentDataBlock().length; i++) {
				for (int j = 0; j < current_shape.getCurrentDataBlock()[i].length; j++) {
					if(current_shape.getCurrentDataBlock()[i][j] == FILLED) {
						data[i+yFigureOffset][j+xFigureOffset] = FILLED;
					}
				}
			}
		}

		private void tryRotateRight() {
			//            for (int i = -1; i < 2; i++) {
			//                if (canBeHere(xFigureOffset+i, yFigureOffset, currentTFigure.peekNextRight())) {
			//                    currentTFigure.rotateRight();
			//                    xFigureOffset += i;
			//                    break;
			//                }
			//            }
			if (canBeHere(xFigureOffset, yFigureOffset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
			} else if (canBeHere(xFigureOffset-1, yFigureOffset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
				xFigureOffset -= 1;
			} else if (canBeHere(xFigureOffset+1, yFigureOffset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
				xFigureOffset += 1;
			}
			repaint();
		}

		private void tryRotateLeft() {
			if (canBeHere(xFigureOffset, yFigureOffset, current_shape.peekNextLeft())) {
				current_shape.rotateLeft();
			}
			repaint();
		}


		// IGameClient methods

		@Override
		public void playerLeft(String name) {
			textarea.append(name + " has left the game.\n");

		}

		@Override
		public void playerJoined(String name) {
			textarea.append(name + " has joined the game.\n");
		}

		@Override
		public void gameStarted() {

		}

		@Override
		public void gameEnded(String winner) {
			game_over = true;
		}

		@Override
		public void dataReceivedByTCP(int fromplayerid, int code, int value) {
			if (code == CODE_LINE_CLEARED) {
				textarea.append("Opponent has cleared a line\n");
				current_speedup += SPEEDUP_INC;
				if (current_speedup > NORMAL_TIME-LOWER_TIME) {
					current_speedup = NORMAL_TIME-LOWER_TIME;
				}
			}
		}

		@Override
		public void dataReceivedByUDP(long time, int fromplayerid, int code, int value) {

		}

		@Override
		public void dataReceivedByTCP(int fromplayerid, String data) {

		}

		@Override
		public void dataReceivedByUDP(long time, int fromplayerid, String data) {

		}

		@Override
		public void error(int error_code, String msg) {
			textarea.append("Error:" + msg + "\n");
		}

		@Override
		public void serverDown(long ms_since_response) {
			textarea.append("Server seems to be down\n");
		}

	}

	class Shape {

		private final Random rand = new Random();

		private int currentDataBlockNumber = 0;
		private int currentFigureNumber = 0;

		private int[][][][] data = {
				{       // L FIGURE
					{
						{0,1,0},
						{0,1,0},
						{1,1,0}
					},
					{
						{1,0,0},
						{1,1,1},
						{0,0,0}
					},
					{
						{0,1,1},
						{0,1,0},
						{0,1,0}
					},
					{
						{0,0,0},
						{1,1,1},
						{0,0,1}
					}
				},
				{   // SQUARE FIGURE
					{
						{1,1},
						{1,1}
					}
				},
				{   // |- FIGURE
					{
						{0,1,0},
						{1,1,1},
						{0,0,0}
					},
					{
						{0,1,0},
						{0,1,1},
						{0,1,0}
					},
					{
						{0,0,0},
						{1,1,1},
						{0,1,0}
					},
					{
						{0,1,0},
						{1,1,0},
						{0,1,0}
					}
				},
				{   // LZ FIGURE
					{
						{1,0,0},
						{1,1,0},
						{0,1,0}
					},
					{
						{0,1,1},
						{1,1,0},
						{0,0,0}
					}
				},
				{   // RZ FIGURE
					{
						{0,0,1},
						{0,1,1},
						{0,1,0}
					},
					{
						{1,1,0},
						{0,1,1},
						{0,0,0}
					}
				},
				{   // | FIGURE
					{
						{0,1,0,0},
						{0,1,0,0},
						{0,1,0,0},
						{0,1,0,0}
					},
					{
						{0,0,0,0},
						{0,0,0,0},
						{1,1,1,1},
						{0,0,0,0}
					}
				}
		};

		public int randInt(int min, int max) {
			return rand.nextInt((max - min) + 1) + min;
		}

		public Shape() {
			currentFigureNumber = randInt(0, data.length-1);
		}

		public int[][] peekNextRight() {
			if(currentDataBlockNumber+1 < data[currentFigureNumber].length) {
				return data[currentFigureNumber][currentDataBlockNumber+1];
			} else {
				return data[currentFigureNumber][0];
			}
		}

		public int[][] peekNextLeft() {
			if(currentDataBlockNumber-1 >= 0) {
				return data[currentFigureNumber][currentDataBlockNumber-1];
			} else {
				return data[currentFigureNumber][data.length-1];
			}
		}

		public int[][] getCurrentDataBlock() {
			return data[currentFigureNumber][currentDataBlockNumber];
		}

		public void rotateRight() {
			if(currentDataBlockNumber+1 < data[currentFigureNumber].length) {
				currentDataBlockNumber++;
			} else {
				currentDataBlockNumber = 0;
			}
		}

		public void rotateLeft() {
			if(currentDataBlockNumber-1 >= 0) {
				currentDataBlockNumber--;
			} else {
				currentDataBlockNumber = data[currentFigureNumber].length-1;
			}
		}
	}

}