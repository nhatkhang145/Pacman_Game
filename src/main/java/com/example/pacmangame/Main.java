package com.example.pacmangame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameManager gameManager = new GameManager();
        
        // Khởi tạo UIManager để quản lý Scene
        UIManager uiManager = new UIManager(gameManager);

        // Kích thước cửa sổ bằng kích thước Canvas (20 cột x 19 hàng * 32)
        Scene scene = new Scene(uiManager.getRoot(), 20 * 32, 19 * 32);
        
        // Bắt sự kiện bàn phím và chuyển cho GameManager
        scene.setOnKeyPressed(event -> gameManager.handleKeyPress(event));

        primaryStage.setTitle("Pac-Man Game 2D");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Bắt đầu vòng lặp game nhưng ban đầu nó ở chế độ MENU
        gameManager.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
