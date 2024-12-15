package org.example.knotsandcrosses;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadServer {
  private static char[][] board = new char[3][3];
  private static int turn = 0; // 0 for X, 1 for O
  private static AtomicInteger clientCount = new AtomicInteger(0);  // Atomic counter for connected clients
  private static final int MAX_CLIENTS = 2;  // Maximum allowed clients

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(12345)) {
      System.out.println("Server started. Waiting for clients...");

      // Wait for first client (Player X)
      Socket playerX = acceptClient(serverSocket);
      if (playerX == null) return;  // If no client was accepted, exit the server
      PrintWriter outX = new PrintWriter(playerX.getOutputStream(), true);
      BufferedReader inX = new BufferedReader(new InputStreamReader(playerX.getInputStream()));
      outX.println("ROLE:X");
      outX.println("READY");

      // Wait for second client (Player O)
      Socket playerO = acceptClient(serverSocket);
      if (playerO == null) return;  // If no second client, exit the server
      PrintWriter outO = new PrintWriter(playerO.getOutputStream(), true);
      BufferedReader inO = new BufferedReader(new InputStreamReader(playerO.getInputStream()));
      outO.println("ROLE:O");

      resetBoard();
      broadcastBoard(outX, outO);

      while (true) {
        PrintWriter currentOut = (turn == 0) ? outX : outO;
        BufferedReader currentIn = (turn == 0) ? inX : inO;
        if (turn == 0) {
          outX.println("YOUR_TURN");
          outO.println("OPPONENT_TURN");
        } else {
          outX.println("OPPONENT_TURN");
          outO.println("YOUR_TURN");
        }

        String move = currentIn.readLine();
        if (move != null && move.startsWith("MOVE:")) {
          String[] parts = move.split(":")[1].split(",");
          int row = Integer.parseInt(parts[0]);
          int col = Integer.parseInt(parts[1]);

          if (board[row][col] == '\0') {
            board[row][col] = (turn == 0) ? 'X' : 'O';
            if (checkWin()) {
              broadcastBoard(outX, outO);
              outX.println("WIN:" + ((turn == 0) ? "X" : "O"));
              outO.println("WIN:" + ((turn == 0) ? "X" : "O"));
              break;
            } else if (isBoardFull()) {
              broadcastBoard(outX, outO);
              outX.println("DRAW");
              outO.println("DRAW");
              break;
            }
            turn = 1 - turn; // Switch turns
            broadcastBoard(outX, outO);
          } else {
            currentOut.println("INVALID_MOVE");
          }
        }
      }
    }
  }

  // Accept a client if under MAX_CLIENTS, otherwise reject it
  private static Socket acceptClient(ServerSocket serverSocket) throws IOException {
    synchronized (clientCount) {
      if (clientCount.get() >= MAX_CLIENTS) {
        System.out.println("Maximum clients reached. Rejecting new client...");
        Socket rejectedClient = serverSocket.accept();
        rejectClient(rejectedClient);
        return null;  // Return null but keep server alive
      } else {
        Socket clientSocket = serverSocket.accept();
        clientCount.incrementAndGet();  // Atomically increment the client count
        System.out.println("Client connected. Total clients: " + clientCount.get());
        return clientSocket;
      }
    }
  }


  // Method to reject a client connection
  private static void rejectClient(Socket clientSocket) {
    try {
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      out.println("SERVER: Maximum number of clients reached. You cannot join the game.");
      clientSocket.close();  // Close the socket of the rejected client
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void resetBoard() {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        board[i][j] = '\0';
      }
    }
  }

  private static void broadcastBoard(PrintWriter outX, PrintWriter outO) {
    StringBuilder sb = new StringBuilder("BOARD:");
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        sb.append(board[i][j] == '\0' ? '-' : board[i][j]);
      }
    }
    outX.println(sb);
    outO.println(sb);
  }

  private static boolean checkWin() {
    for (int i = 0; i < 3; i++) {
      if (board[i][0] != '\0' && board[i][0] == board[i][1] && board[i][1] == board[i][2]) return true;
      if (board[0][i] != '\0' && board[0][i] == board[1][i] && board[1][i] == board[2][i]) return true;
    }
    return board[0][0] != '\0' && board[0][0] == board[1][1] && board[1][1] == board[2][2] ||
            board[0][2] != '\0' && board[0][2] == board[1][1] && board[1][1] == board[2][0];
  }

  private static boolean isBoardFull() {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (board[i][j] == '\0') return false;
      }
    }
    return true;
  }
}