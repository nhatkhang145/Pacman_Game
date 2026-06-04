package com.example.pacmangame;

import com.example.pacmangame.controller.GameController;
import com.example.pacmangame.view.GameView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameController gameController = new GameController();
        GameView gameView = new GameView(gameController);

        // Kích thước cửa sổ bằng kích thước Canvas (20 cột x 19 hàng * 32)
        Scene scene = new Scene(gameView.getRoot(), 20 * 32, 19 * 32);

        // Bắt sự kiện bàn phím và chuyển cho GameController
        scene.setOnKeyPressed(event -> gameController.handleKeyPress(event));

        primaryStage.setTitle("Pac-Man Game 2D");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Bắt đầu vòng lặp game nhưng ban đầu nó ở chế độ MENU
        gameController.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
