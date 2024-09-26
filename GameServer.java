import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 5999;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static List<Player> players = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Game server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Player player;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    handleClientMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }

        private void handleClientMessage(String message) {
            String[] parts = message.split(" ");
            switch (parts[0]) {
                case "MOVE":
                    handleMove(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]);
                    break;
                case "JOIN":
                    handleJoin(parts[1]);
                    break;
                // Handle other messages
            }
        }

        private void handleMove(String playerName, int delta_x, int delta_y, String direction) {
            // Update player position and notify all clients
            for (Player p : players) {
                if (p.getName().equals(playerName)) {
                    p.move(delta_x, delta_y, direction);
                    broadcast("UPDATE " + p.getName() + " " + p.getXpos() + " " + p.getYpos() + " " + p.getDirection());
                }
            }
        }

        private void handleJoin(String playerName) {
            // Set initial position (e.g., (9, 4)) when the player joins
            Player newPlayer = new Player(playerName, 9, 4, "up");
            players.add(newPlayer);
            broadcast("UPDATE " + newPlayer.getName() + " " + newPlayer.getXpos() + " " + newPlayer.getYpos() + " " + newPlayer.getDirection());
        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }
}
