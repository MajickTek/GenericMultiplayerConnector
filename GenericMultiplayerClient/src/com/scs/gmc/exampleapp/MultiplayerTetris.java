package com.scs.gmc.exampleapp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.scs.gmc.client.ConnectorMain;
import com.scs.gmc.client.ConnectorMain.GameStage;
import com.scs.gmc.client.IGameClient;
import com.scs.gmc.client.StartGameOptions;

/**
 * A simple implementation of Tetris designed to show real-world usage of GMC.
 *
 */
public class MultiplayerTetris extends JFrame {

	private static final int ROWS = 16;
	private static final int COLS = 8;


	// Comms codes
	public static final int CODE_LINE_CLEARED = 1;

	private static final long serialVersionUID = 1L;

	private ConnectorMain connector; 
	private MyPanel canvas = new MyPanel(20, ROWS, COLS);
	private JTextArea textarea;

	public static void main(String[] args) {
		new MultiplayerTetris();
	}

	public MultiplayerTetris() {
		this.setSize((2 * canvas.cell_size * canvas.col_cells) + 80, (canvas.cell_size * canvas.row_cells) + 80);
		this.setTitle("Multiplayer Tetris");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setLayout(null);
		this.setResizable(false);
		this.add(canvas);

		textarea = new JTextArea();
		textarea.setWrapStyleWord(true);
		textarea.setEditable(false);
		textarea.setLineWrap(true);
		JScrollPane sp = new JScrollPane(textarea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBounds(this.getWidth() / 2, 10, this.getWidth() - canvas.getWidth() - 80, this.getHeight()-80);
		this.add(sp);

		textarea.append("Use arrow keys to move the shapes.\n");

		connector = StartGameOptions.ShowOptionsAndConnect(canvas, "Multiplayer Tetris", null);
		if (connector == null) {
			// todo - check min players
			// User pressed cancel or connection failed.
			System.exit(0);
		}
		try {
			connector.joinGame();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		textarea.append("Connected to server " + connector.getServer() + ":" + connector.getPort() + "\n");
		textarea.append("Hello " + connector.getPlayerName() + ".\n");
		textarea.append("Joined game " + connector.getGameID() + ".\n");

		//canvas.setPosition(109, 20);
		canvas.setFocusable(true);
		canvas.addKeyListener(canvas);
		canvas.requestFocusInWindow();

		while (true) {
			canvas.restart();

			Thread canvasUpdateThread = new Thread(canvas, this.getClass().getSimpleName() + "_Thread");
			canvasUpdateThread.start();

			try {
				canvasUpdateThread.join();
			} catch (InterruptedException e) {
				// Do nothing
			}

			int dialogResult = JOptionPane.showConfirmDialog (null, "Play again?", "Game Over", JOptionPane.YES_NO_OPTION);
			if(dialogResult != JOptionPane.YES_OPTION){
				break;
			}
			this.textarea.setText("");
		}
		if (connector != null) {
			connector.disconnect();
		}
		System.exit(0);
	}


	class MyPanel extends JPanel implements KeyListener, Runnable, IGameClient {

		private static final long serialVersionUID = 1L;

		private final int DROP_TIME = 100;
		private final int NORMAL_TIME = 500;
		private final int SPEEDUP_INC = 40;

		private static final int EMPTY = 0;
		private static final int FILLED = 1;

		private int cell_size;
		private int row_cells;
		private int col_cells;
		private int[][] data;
		private int x_offset = 0;
		private int y_offset = 0;
		private Shape current_shape;

		private boolean game_over = false;

		private int timeInterval = NORMAL_TIME;
		private AtomicInteger current_speedup = new AtomicInteger(); // Gets subtracted from delay.

		public MyPanel(int _cell_size, int _row_cells, int _col_cells) {
			super();

			cell_size = _cell_size;
			row_cells = _row_cells;
			col_cells = _col_cells;

			restart();
			this.setBounds(10,10, _col_cells*_cell_size+1, _row_cells*_cell_size+1);
		}


		public void restart() {
			x_offset = 0;
			y_offset = 0;
			data = new int[row_cells][col_cells];
			this.current_speedup.set(0);
			this.appendNewShape();
		}


		@Override
		public void run() {
			textarea.append("Connected to " + connector.getServer() + ":" + connector.getPort() + ".\n");
			textarea.append("Joined game '" + connector.getGameID() + "'.\n");
			textarea.append("Waiting for other players...\n");
			connector.waitForStage(GameStage.IN_PROGRESS);
			textarea.append("Game started!\n");
			game_over = false;
			while(!game_over && connector.getGameStage() == GameStage.IN_PROGRESS) {
				int len = timeInterval - current_speedup.get();
				if (len < DROP_TIME) {
					len = DROP_TIME;
				}
				try {
					Thread.sleep(len);
				} catch (InterruptedException e) {}

				tryMoveDown();
				repaint();
			}

			textarea.append("Game Over - waiting for game to finish...\n");
			connector.waitForStage(GameStage.FINISHED);
			if (connector.areWeTheWinner()) {
				textarea.append("You have won!\n");
				JOptionPane.showMessageDialog(this, "You won!");
			} else {
				textarea.append("The winner was " + connector.getWinnersName() + "\n");
				JOptionPane.showMessageDialog(this, "The winner was " + connector.getWinnersName());
			}
		}


		@Override
		protected void paintComponent(Graphics g) {
			drawBackground(g, Color.white);
			drawData(g);
			if(!game_over) {// && connector.getGameStage() != GameStage.WAITING_FOR_PLAYERS) {
				drawCurrentShape(g);
			}
			drawGrid(g);
		}

		public void appendNewShape() {
			x_offset = 0;
			y_offset = 0;
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
			if (current_shape != null) {
				for (int i = 0; i < current_shape.getCurrentDataBlock().length; i++) {
					for (int j = 0; j < current_shape.getCurrentDataBlock()[i].length; j++) {
						if(current_shape.getCurrentDataBlock()[i][j] == FILLED) {
							g.fillRect((x_offset + j) * cell_size, (y_offset + i) * cell_size, cell_size, cell_size);
						}
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
			if (!game_over) {
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
					timeInterval = DROP_TIME;
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
					if (fig_data[i][j] == FILLED) {
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
			if (canBeHere(x_offset+1, y_offset, current_shape.getCurrentDataBlock())) {
				x_offset++;
			}
			repaint();
		}

		private void tryMoveLeft() {
			if (canBeHere(x_offset-1, y_offset, current_shape.getCurrentDataBlock())) {
				x_offset--;
			}
			repaint();
		}

		private void tryMoveDown() {
			if (current_shape != null) {
				if (canBeHere(x_offset, y_offset+1, current_shape.getCurrentDataBlock())) {
					y_offset++;
				} else {
					addCurrentDataBlockToMainData();
					tryClearLines();
					timeInterval = NORMAL_TIME;

					x_offset = 0;
					y_offset = 0;
					current_shape = new Shape();
					if(!canBeHere(x_offset, y_offset, current_shape.getCurrentDataBlock())) {
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
					// We have cleared a row
					try {
						connector.sendKeyValueDataByTCP(CODE_LINE_CLEARED, 1);
					} catch (IOException e) {
						textarea.append(e.getMessage());
						e.printStackTrace();
					}
					// Slow us down
					synchronized (current_speedup) {
						current_speedup.addAndGet(-SPEEDUP_INC);
						if (current_speedup.get() < 0) {
							current_speedup.set(0);
						}
					}
				}
			}
			data = buf_data;
		}


		private void gameOver() {
			System.out.println("GAME OVER");
			try {
				connector.sendOutOfGame();
			} catch (IOException e) {
				textarea.append(e.getMessage());
				e.printStackTrace();
			}
			game_over = true;
			addCurrentDataBlockToMainData();
			current_shape = null;
		}


		private void addCurrentDataBlockToMainData() {
			for (int i = 0; i < current_shape.getCurrentDataBlock().length; i++) {
				for (int j = 0; j < current_shape.getCurrentDataBlock()[i].length; j++) {
					if(current_shape.getCurrentDataBlock()[i][j] == FILLED) {
						data[i+y_offset][j+x_offset] = FILLED;
					}
				}
			}
		}


		private void tryRotateRight() {
			if (canBeHere(x_offset, y_offset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
			} else if (canBeHere(x_offset-1, y_offset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
				x_offset -= 1;
			} else if (canBeHere(x_offset+1, y_offset, current_shape.peekNextRight())) {
				current_shape.rotateRight();
				x_offset += 1;
			}
			repaint();
		}

		private void tryRotateLeft() {
			if (canBeHere(x_offset, y_offset, current_shape.peekNextLeft())) {
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
		public void keyValueReceivedByTCP(int fromplayerid, int code, int value) {
			if (code == CODE_LINE_CLEARED) {
				// An opponent has clear a row, so speed us up!
				textarea.append("Opponent has cleared a line\n");
				synchronized (current_speedup) {
					current_speedup.addAndGet(SPEEDUP_INC);
					if (current_speedup.get() > NORMAL_TIME-DROP_TIME) {
						current_speedup.set(NORMAL_TIME-DROP_TIME);
					}
				}
			}
		}

		@Override
		public void keyValueReceivedByUDP(long time, int fromplayerid, int code, int value) {

		}

		@Override
		public void stringReceivedByTCP(int fromplayerid, String data) {

		}

		@Override
		public void stringReceivedByUDP(long time, int fromplayerid, String data) {

		}

		@Override
		public void error(Throwable ex) {
			textarea.append("Error:" + ex.getMessage() + "\n");
		}

		@Override
		public void serverDown(long ms_since_response) {
			textarea.append("Server seems to be down\n");
		}


		@Override
		public void byteArrayReceivedByTCP(int fromplayerid, byte[] data) {

		}


		@Override
		public void byteArrayReceivedByUDP(long time, int fromplayerid, byte[] data) {

		}


		@Override
		public void objectReceivedByTCP(int fromplayerid, Object obj) {
			
		}


		@Override
		public void objectReceivedByUDP(long time, int fromplayerid, Object obj) {
			
		}

	}

	class Shape {

		private final Random rand = new Random();

		private int curr_block_num = 0;
		private int curr_shape_num = 0;

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
			curr_shape_num = randInt(0, data.length-1);
		}

		public int[][] peekNextRight() {
			if (curr_block_num+1 < data[curr_shape_num].length) {
				return data[curr_shape_num][curr_block_num+1];
			} else {
				return data[curr_shape_num][0];
			}
		}

		public int[][] peekNextLeft() {
			if (curr_block_num-1 >= 0) {
				return data[curr_shape_num][curr_block_num-1];
			} else {
				return data[curr_shape_num][data.length-1];
			}
		}

		public int[][] getCurrentDataBlock() {
			return data[curr_shape_num][curr_block_num];
		}

		public void rotateRight() {
			if (curr_block_num+1 < data[curr_shape_num].length) {
				curr_block_num++;
			} else {
				curr_block_num = 0;
			}
		}

		public void rotateLeft() {
			if (curr_block_num-1 >= 0) {
				curr_block_num--;
			} else {
				curr_block_num = data[curr_shape_num].length-1;
			}
		}
	}

}