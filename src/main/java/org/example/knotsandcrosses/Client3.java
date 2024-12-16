package org.example.knotsandcrosses;
// Client.java

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class Client3 extends Application {
  private char role;
  private boolean myTurn;
  private char[][] board = new char[3][3];
  private Button[][] buttons = new Button[3][3];
  private Label statusLabel = new Label("Cannot connect more than two players");
  private Label titleLabel = new Label("Knots and Crosses");
  private Label roleLabel = new Label("Quit this page");


  private PrintWriter out;
  private BufferedReader in;


  @Override
  public void start(Stage primaryStage) throws Exception {
    VBox root = new VBox(10);
    root.setAlignment(Pos.CENTER);

    titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

    // Role label styling
    roleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: normal;");

    statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: normal;");

    GridPane grid = new GridPane();
    grid.setVgap(10); // Vertical gap between rows
    grid.setHgap(10); // Horizontal gap between columns
    grid.setAlignment(Pos.CENTER);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        Button button = new Button(" ");
        button.setMinSize(100, 100);
        int row = i, col = j;
        button.setOnAction(e -> {
          if (myTurn && board[row][col] == '\0') {
            out.println("MOVE:" + row + "," + col);
          }
        });
        buttons[i][j] = button;
        grid.add(button, j, i);
      }
    }


    root.getChildren().addAll(grid, statusLabel, titleLabel, roleLabel);
    Scene scene = new Scene(root, 400, 450);
    primaryStage.setScene(scene);
    primaryStage.setTitle("Tic-Tac-Toe");
    primaryStage.show();


    new Thread(this::connectToServer).start();
  }


  private void connectToServer() {
    try (Socket socket = new Socket("localhost", 12345)) {
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


      String message;
      while ((message = in.readLine()) != null) {
        String currentMessage = message; // Create a final local copy of the message
        if (currentMessage.startsWith("ROLE:")) {
          role = currentMessage.charAt(5);
          roleLabel.setText("You are " + role);
          Platform.runLater(() -> statusLabel.setText("You are " + role));
        } else if(currentMessage.startsWith("READY")){
          Platform.runLater(() -> statusLabel.setText("Connected, waiting on opponent..."));
        } else if (currentMessage.startsWith("YOUR_TURN")) {
          myTurn = true;
          enableButtons();
          Platform.runLater(() -> statusLabel.setText("Your turn!"));
        }else if (message.startsWith("OPPONENT_TURN")) {
          myTurn = false;
          Platform.runLater(() -> statusLabel.setText("Opponent's turn..."));
          disableButtons();
        } else if (currentMessage.startsWith("BOARD:")) {
          Platform.runLater(() -> updateBoard(currentMessage.substring(6)));
        } else if (currentMessage.startsWith("WIN:")) {
          char winner = currentMessage.charAt(4);
          Platform.runLater(() -> {
            statusLabel.setText(winner + " wins!");
            disableButtons();
          });
        } else if (currentMessage.startsWith("DRAW")) {
          Platform.runLater(() -> {
            statusLabel.setText("It's a draw!");
            disableButtons();
          });
        } else if (currentMessage.startsWith("INVALID_MOVE")) {
          Platform.runLater(() -> statusLabel.setText("Invalid move! Try again."));
        }
      }


    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private void updateBoard(String boardString) {
    for (int i = 0; i < 9; i++) {
      int row = i / 3;
      int col = i % 3;
      board[row][col] = boardString.charAt(i) == '-' ? '\0' : boardString.charAt(i);
      buttons[row][col].setText(board[row][col] == '\0' ? " " : String.valueOf(board[row][col]));
    }
  }


  private void enableButtons() {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        buttons[i][j].setDisable(false); // Enable all buttons
      }
    }
  }

  private void disableButtons() {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        buttons[i][j].setDisable(true); // Disable all buttons
      }
    }
  }


  public static void main(String[] args) {
    launch(args);
  }
}

