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
	public static List<Player> players = new ArrayList<Player>();

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
		socket = new Socket("localhost", 12345);
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
		String[] parts = message.split(" ");
		switch (parts[0]) {
			case "UPDATE":
				// Use Platform.runLater to ensure UI updates are on the JavaFX Application Thread
				Platform.runLater(() -> updatePlayer(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]));
				break;
			case "SCORES":
				Platform.runLater(() -> updateScores(parts[1]));
				break;
			// Handle other messages
		}
	}

	private void updatePlayer(String playerName, int x, int y, String direction) {
		Player player = getPlayerByName(playerName);
		if (player != null) {
			Platform.runLater(() -> {
				fields[player.getXpos()][player.getYpos()].setGraphic(new ImageView(image_floor));
				player.setXpos(x);
				player.setYpos(y);
				player.setDirection(direction);
				ImageView heroImage = getHeroImageForDirection(direction);
				fields[x][y].setGraphic(heroImage);
			});
		}
	}

	private void updateScores(String scores) {
		Platform.runLater(() -> scoreList.setText(scores));
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


	private Player getPlayerByName(String playerName) {
		for (Player p : players) {
			if (p.getName().equals(playerName)) {
				return p;
			}
		}
		return null;
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		// Send move command to server
		out.println("MOVE " + me.getName() + " " + delta_x + " " + delta_y + " " + direction);
	}

	public static void main(String[] args) {
		launch(args);
	}
}