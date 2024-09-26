import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;

public class GUI extends Application {

	public static final int size = 20;
	public static final int scene_height = size * 20 + 100;
	public static final int scene_width = size * 20 + 200;

	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right, hero_left, hero_up, hero_down;

	public static Player me;
	public static List<Player> players = new ArrayList<>();

	private Label[][] fields;
	private TextArea scoreList;

	private String[] board = {
			"wwwwwwwwwwwwwwwwwwww",
			"w        ww        w",
			"w w  w  www w  w  ww",
			"w w  w   ww w  w  ww",
			"w  w               w",
			"w w w w w w w  w  ww",
			"w w     www w  w  ww",
			"w w     w w w  w  ww",
			"w   w w  w  w  w   w",
			"w     w  w  w  w   w",
			"w ww ww        w  ww",
			"w  w w    w    w  ww",
			"w        ww w  w  ww",
			"w         w w  w  ww",
			"w        w     w  ww",
			"w  w              ww",
			"w  w www  w w  ww ww",
			"w w      ww w     ww",
			"w   w   ww  w      w",
			"wwwwwwwwwwwwwwwwwwww"
	};

	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	@Override
	public void start(Stage primaryStage) {
		try {
			connectToServer();
			try {
				setupGUI(primaryStage);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupGUI(Stage primaryStage) throws Exception {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(0, 10, 0, 10));

		Text mazeLabel = new Text("Maze:");
		mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

		Text scoreLabel = new Text("Score:");
		scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

		scoreList = new TextArea();

		GridPane boardGrid = new GridPane();

		image_wall = new Image(getClass().getResourceAsStream("Image/wall4.png"), size, size, false, false);
		image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"), size, size, false, false);

		hero_right = new Image(getClass().getResourceAsStream("Image/heroRight.png"), size, size, false, false);
		hero_left = new Image(getClass().getResourceAsStream("Image/heroLeft.png"), size, size, false, false);
		hero_up = new Image(getClass().getResourceAsStream("Image/heroUp.png"), size, size, false, false);
		hero_down = new Image(getClass().getResourceAsStream("Image/heroDown.png"), size, size, false, false);

		fields = new Label[20][20];
		for (int j = 0; j < 20; j++) {
			for (int i = 0; i < 20; i++) {
				switch (board[j].charAt(i)) {
					case 'w':
						fields[i][j] = new Label("", new ImageView(image_wall));
						break;
					case ' ':
						fields[i][j] = new Label("", new ImageView(image_floor));
						break;
					default:
						throw new Exception("Illegal field value: " + board[j].charAt(i));
				}
				boardGrid.add(fields[i][j], i, j);
			}
		}
		scoreList.setEditable(false);

		grid.add(mazeLabel, 0, 0);
		grid.add(scoreLabel, 1, 0);
		grid.add(boardGrid, 0, 1);
		grid.add(scoreList, 1, 1);

		Scene scene = new Scene(grid, scene_width, scene_height);
		primaryStage.setScene(scene);
		primaryStage.show();

		scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			switch (event.getCode()) {
				case UP: playerMoved(0, -1, "up"); break;
				case DOWN: playerMoved(0, +1, "down"); break;
				case LEFT: playerMoved(-1, 0, "left"); break;
				case RIGHT: playerMoved(+1, 0, "right"); break;
				default: break;
			}
		});

		// Initial setup for players
		me = new Player("Orville", 9, 4, "up");
		players.add(me);
		fields[9][4].setGraphic(new ImageView(hero_up));

		// Send join message to server
		out.println("JOIN Orville");

		new Thread(new ServerListener()).start(); // Start listening for server messages
	}

	private void connectToServer() throws IOException {
		socket = new Socket("192.168.50.207", 5999);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private class ServerListener implements Runnable {
		@Override
		public void run() {
			try {
				String message;
				while ((message = in.readLine()) != null) {
					processServerMessage(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void processServerMessage(String message) {
		Platform.runLater(() -> {
			String[] parts = message.split(" ");
			if ("UPDATE".equals(parts[0])) {
				String playerName = parts[1];
				int x = Integer.parseInt(parts[2]);
				int y = Integer.parseInt(parts[3]);
				String direction = parts[4];

				// Update or add player to the list
				boolean playerExists = false;
				for (Player p : players) {
					if (p.getName().equals(playerName)) {
						playerExists = true;
						updatePlayer(playerName, x, y, direction);
						break;
					}
				}
				// If the player doesn't exist, add them
				if (!playerExists) {
					Player newPlayer = new Player(playerName, x, y, direction);
					players.add(newPlayer);
					fields[x][y].setGraphic(getHeroImageForDirection(direction)); // Add new player's graphic
				}
			}
		});
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		int new_x = me.getXpos() + delta_x;
		int new_y = me.getYpos() + delta_y;

		// Check if the new position is valid
		if (new_x >= 0 && new_x < 20 && new_y >= 0 && new_y < 20 && board[new_y].charAt(new_x) != 'w') {
			// Send move command to the server
			out.println("MOVE " + me.getName() + " " + delta_x + " " + delta_y + " " + direction);
		}
	}

	private void updatePlayer(String playerName, int x, int y, String direction) {
		for (Player p : players) {
			if (p.getName().equals(playerName)) {
				fields[p.getXpos()][p.getYpos()].setGraphic(new ImageView(image_floor)); // Clear old position
				p.setXpos(x);
				p.setYpos(y);
				p.setDirection(direction);

				ImageView heroImage = getHeroImageForDirection(direction);
				fields[x][y].setGraphic(heroImage);
			}
		}
	}

	private ImageView getHeroImageForDirection(String direction) {
		switch (direction) {
			case "right": return new ImageView(hero_right);
			case "left": return new ImageView(hero_left);
			case "up": return new ImageView(hero_up);
			case "down": return new ImageView(hero_down);
			default: return new ImageView(hero_up);
		}
	}
}
