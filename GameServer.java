import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class GameServer {
    private static final int PORT = 5999;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static List<Player> players = new ArrayList<>();
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void main(String[] args) {
        System.out.println("Game server started...");
        System.out.println(lock.isFair());
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
            }
        }


        private void handleMove(String playerName, int delta_x, int delta_y, String direction) {
            System.out.println("Attempting to lock for player move: " + playerName);
            lock.lock();
            try {
                System.out.println("Lock acquired for player move: " + playerName);

                Player movingPlayer = null;
                for (Player p : players) {
                    if (p.getName().equals(playerName)) {
                        movingPlayer = p;
                        break;
                    }
                }

                if (movingPlayer != null) {
                    int newX = movingPlayer.getXpos() + delta_x;
                    int newY = movingPlayer.getYpos() + delta_y;

                    if (isWall(newX, newY)) {
                        movingPlayer.addPoints(-1);
                    } else {
                        movingPlayer.move(delta_x, delta_y, direction);
                        movingPlayer.addPoints(1);
                    }

                    broadcast("UPDATE " + movingPlayer.getName() + " " + movingPlayer.getXpos() + " " + movingPlayer.getYpos() + " " + movingPlayer.getDirection() + " " + movingPlayer.getScore());

                    for (Player otherPlayer : players) {
                        if (!otherPlayer.equals(movingPlayer) && otherPlayer.getXpos() == movingPlayer.getXpos() && otherPlayer.getYpos() == movingPlayer.getYpos()) {
                            movingPlayer.addPoints(10);
                            otherPlayer.addPoints(-10);
                            broadcast("UPDATE " + movingPlayer.getName() + " " + movingPlayer.getXpos() + " " + movingPlayer.getYpos() + " " + movingPlayer.getDirection() + " " + movingPlayer.getScore());
                            broadcast("UPDATE " + otherPlayer.getName() + " " + otherPlayer.getXpos() + " " + otherPlayer.getYpos() + " " + otherPlayer.getDirection() + " " + otherPlayer.getScore());
                            break;
                        }
                    }
                }

                System.out.println("Player move processed for: " + playerName);
            } finally {
                System.out.println("Releasing lock for player move: " + playerName);
                lock.unlock();
            }
        }


        private void handleJoin(String playerName) {
            lock.lock();
            try {
                Player newPlayer = new Player(playerName, 9, 4, "up");
                players.add(newPlayer);

                broadcast("UPDATE " + newPlayer.getName() + " " + newPlayer.getXpos() + " " + newPlayer.getYpos() + " " + newPlayer.getDirection() + " " + newPlayer.getScore());

                for (Player p : players) {
                    if (!p.getName().equals(playerName)) {
                        out.println("UPDATE " + p.getName() + " " + p.getXpos() + " " + p.getYpos() + " " + p.getDirection() + " " + newPlayer.getScore());
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }

        private boolean isWall(int x, int y) {
            String[] board = {
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
            return board[y].charAt(x) == 'w';
        }
    }
}