package com.example.pacmangame;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class UIManager {
    private StackPane root;
    private VBox menuScreen;
    private VBox settingsScreen;
    private VBox pauseScreen;
    private VBox gameOverScreen;
    private GameManager gameManager;
    private GameState previousState = GameState.MENU;

    // UI Elements
    private Button btnPlay;
    private Button btnContinue;
    private Button btnSettings;
    private Button btnLeaderboard;
    private Button btnHowToPlay;
    private Button btnBack;
    private Label lblVolume;
    private Label lblLanguage;
    private Text titleMenu;
    private Text titleSettings;

    // Pause UI
    private Text titlePause;
    private Button btnResume;
    private Button btnPauseSave;
    private Button btnPauseSettings;
    private Button btnQuit;

    // Game Over UI
    private Text titleGameOver;
    private Text finalScoreGameOver;
    private Text hintGameOver;
    private Button btnPlayAgain;
    private Button btnGameOverSave;
    private Button btnGameOverLeaderboard;
    private Button btnGameOverQuit;

    public UIManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.gameManager.setUIManager(this);

        this.root = new StackPane();
        this.root.setStyle("-fx-background-color: black;");

        createMenuScreen();
        createSettingsScreen();
        createPauseScreen();
        createGameOverScreen();

        // Ensure canvas can receive focus for key events
        gameManager.getCanvas().setFocusTraversable(true);

        root.getChildren().addAll(gameManager.getCanvas(), settingsScreen, pauseScreen, gameOverScreen, menuScreen);

        showMenu();
        updateLanguage();
    }

    public StackPane getRoot() {
        return root;
    }

    private void createMenuScreen() {
        menuScreen = new VBox(20);
        menuScreen.setAlignment(Pos.CENTER);
        menuScreen.setStyle("-fx-background-color: black;");

        titleMenu = new Text("PAC-MAN");
        titleMenu.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        titleMenu.setStyle("-fx-fill: yellow;");

        btnPlay = createRetroButton("");
        btnPlay.setOnAction(e -> {
            gameManager.clearSavedGame();
            gameManager.resetGame();
            showGame();
        });

        btnContinue = createRetroButton("");
        btnContinue.setDisable(true);
        btnContinue.setOnAction(e -> {
            if (gameManager.continueSavedGame()) {
                showGame();
            } else {
                showAlert("Continue", "No saved game found.");
            }
        });

        btnLeaderboard = createRetroButton("");
        btnLeaderboard.setOnAction(e -> showLeaderboard());

        btnSettings = createRetroButton("");
        btnSettings.setOnAction(e -> showSettings());

        btnHowToPlay = createRetroButton("");
        btnHowToPlay.setOnAction(e -> showAlert("How to Play", "Coming Soon!"));

        menuScreen.getChildren().addAll(titleMenu, btnPlay, btnContinue, btnLeaderboard, btnSettings, btnHowToPlay);
    }

    private void createSettingsScreen() {
        settingsScreen = new VBox(20);
        settingsScreen.setAlignment(Pos.CENTER);
        settingsScreen.setStyle("-fx-background-color: black;");

        titleSettings = new Text("SETTINGS");
        titleSettings.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        titleSettings.setStyle("-fx-fill: yellow;");

        lblVolume = new Label();
        lblVolume.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblVolume.setStyle("-fx-text-fill: white;");

        Slider volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setMaxWidth(300);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SettingsManager.getInstance().setVolume(newVal.doubleValue() / 100.0);
        });

        lblLanguage = new Label();
        lblLanguage.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblLanguage.setStyle("-fx-text-fill: white;");

        ComboBox<SettingsManager.Language> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(SettingsManager.Language.values());
        langCombo.setValue(SettingsManager.Language.VI);
        langCombo.setOnAction(e -> {
            SettingsManager.getInstance().setLanguage(langCombo.getValue());
            updateLanguage();
        });

        btnBack = createRetroButton("");
        btnBack.setOnAction(e -> backFromSettings());

        settingsScreen.getChildren().addAll(titleSettings, lblVolume, volumeSlider, lblLanguage, langCombo, btnBack);
    }

    private void createPauseScreen() {
        pauseScreen = new VBox(20);
        pauseScreen.setAlignment(Pos.CENTER);
        // Semi-transparent background
        pauseScreen.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        titlePause = new Text("PAUSED");
        titlePause.setFont(Font.font("Arial", FontWeight.BOLD, 50));
        titlePause.setStyle("-fx-fill: yellow;");

        btnResume = createRetroButton("");
        btnResume.setOnAction(e -> showGame());

        btnPauseSave = createRetroButton("");
        btnPauseSave.setOnAction(e -> notifySave(gameManager.saveCurrentGame()));

        btnPauseSettings = createRetroButton("");
        btnPauseSettings.setOnAction(e -> showSettings());

        btnQuit = createRetroButton("");
        btnQuit.setOnAction(e -> {
            notifySave(gameManager.saveCurrentGame());
            showMenu();
        });

        pauseScreen.getChildren().addAll(titlePause, btnResume, btnPauseSave, btnPauseSettings, btnQuit);
    }

    private void createGameOverScreen() {
        gameOverScreen = new VBox(20);
        gameOverScreen.setAlignment(Pos.CENTER);
        gameOverScreen.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");

        titleGameOver = new Text("GAME OVER");
        titleGameOver.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        titleGameOver.setStyle("-fx-fill: red;");

        finalScoreGameOver = new Text("SCORE: 0");
        finalScoreGameOver.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        finalScoreGameOver.setStyle("-fx-fill: white;");

        hintGameOver = new Text("SAVE, RESTART OR RETURN TO MENU");
        hintGameOver.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        hintGameOver.setStyle("-fx-fill: lightgray;");

        btnPlayAgain = createRetroButton("");
        btnPlayAgain.setOnAction(e -> {
            gameManager.clearSavedGame();
            gameManager.resetGame();
            showGame();
        });

        btnGameOverSave = createRetroButton("");
        btnGameOverSave.setOnAction(e -> notifySave(gameManager.saveCurrentGame()));

        btnGameOverLeaderboard = createRetroButton("");
        btnGameOverLeaderboard.setOnAction(e -> showLeaderboard());

        btnGameOverQuit = createRetroButton("");
        btnGameOverQuit.setOnAction(e -> showMenu());

        gameOverScreen.getChildren().addAll(titleGameOver, finalScoreGameOver, hintGameOver, btnPlayAgain,
                btnGameOverSave, btnGameOverLeaderboard, btnGameOverQuit);
    }

    private Button createRetroButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        btn.setMinWidth(200);
        btn.setStyle(
                "-fx-background-color: black; -fx-text-fill: yellow; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: blue; -fx-text-fill: white; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: black; -fx-text-fill: yellow; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;"));

        return btn;
    }

    public void showMenu() {
        gameManager.getCanvas().setVisible(false);
        settingsScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        menuScreen.setVisible(true);
        gameManager.setGameState(GameState.MENU);
        // Stop the game loop while in the menu to save CPU and avoid unexpected input
        gameManager.stop();

        if (gameManager.canContinueGame()) {
            btnContinue.setDisable(false);
        } else {
            btnContinue.setDisable(true);
        }
    }

    public void showSettings() {
        previousState = gameManager.getGameState();

        // Hide UI elements to show settings
        menuScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);

        // Do not hide the canvas if we are coming from PAUSED, so settings overlay on
        // top of game
        if (previousState != GameState.PAUSED) {
            gameManager.getCanvas().setVisible(false);
        }

        settingsScreen.setVisible(true);
        gameManager.setGameState(GameState.SETTINGS);
    }

    private void backFromSettings() {
        if (previousState == GameState.PAUSED) {
            showPauseMenu();
        } else {
            showMenu();
        }
    }

    public void showGame() {
        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        gameManager.getCanvas().setVisible(true);
        gameManager.getCanvas().requestFocus();
        gameManager.setGameState(GameState.PLAYING);
        // Ensure the game loop is running when entering the game
        gameManager.start();
    }

    public void showPauseMenu() {
        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        gameManager.getCanvas().setVisible(true);
        pauseScreen.setVisible(true);
        gameManager.setGameState(GameState.PAUSED);
    }

    public void showGameOver(int score) {
        // Stop the game loop to freeze the canvas and avoid animation interference
        gameManager.stop();

        // Show the Game Over overlay first so the user sees it immediately.
        // Schedule the optional score recording to run after the overlay is rendered
        // to avoid blocking the UI thread before the overlay becomes visible.
        javafx.application.Platform.runLater(() -> recordScore(score));

        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameManager.getCanvas().setVisible(true);

        finalScoreGameOver.setText((SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI
                ? "ĐIỂM CUỐI: "
                : "FINAL SCORE: ") + score);

        // Make sure the overlay fills the window and is in front
        gameOverScreen.setPrefSize(root.getWidth(), root.getHeight());
        gameOverScreen.setVisible(true);
        gameOverScreen.toFront();

        gameManager.setGameState(GameState.GAME_OVER);
    }

    private void recordScore(int score) {
        TextInputDialog dialog = new TextInputDialog("PLAYER");
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;
        dialog.setTitle(isVi ? "LƯU BẢNG XẾP HẠNG" : "SAVE LEADERBOARD");
        dialog.setHeaderText(isVi ? "Nhập tên của bạn để lưu điểm" : "Enter your name to save the score");
        dialog.setContentText(isVi ? "Tên:" : "Name:");

        String playerName = dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElse("PLAYER");

        LeaderboardManager.getInstance().addScore(playerName, score);
    }

    public void showLeaderboard() {
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;
        String title = isVi ? "BẢNG XẾP HẠNG OFFLINE" : "OFFLINE LEADERBOARD";
        String content = LeaderboardManager.getInstance().buildLeaderboardText(10, isVi);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea area = new TextArea(content);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefColumnCount(30);
        area.setPrefRowCount(12);
        area.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setPrefWidth(420);
        alert.getDialogPane().setPrefHeight(360);
        alert.showAndWait();
    }

    private void updateLanguage() {
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;

        // Menu
        btnPlay.setText(isVi ? "CHƠI MỚI" : "PLAY");
        btnContinue.setText(isVi ? "TIẾP TỤC" : "CONTINUE");
        btnSettings.setText(isVi ? "CÀI ĐẶT" : "SETTINGS");
        btnLeaderboard.setText(isVi ? "BẢNG XẾP HẠNG" : "LEADERBOARD");
        btnHowToPlay.setText(isVi ? "HƯỚNG DẪN" : "HOW TO PLAY");

        // Settings
        btnBack.setText(isVi ? "QUAY LẠI" : "BACK");
        lblVolume.setText(isVi ? "ÂM LƯỢNG:" : "VOLUME:");
        lblLanguage.setText(isVi ? "NGÔN NGỮ:" : "LANGUAGE:");
        titleSettings.setText(isVi ? "CÀI ĐẶT" : "SETTINGS");

        // Pause Menu
        titlePause.setText(isVi ? "TẠM DỪNG" : "PAUSED");
        btnResume.setText(isVi ? "TIẾP TỤC" : "RESUME");
        btnPauseSave.setText(isVi ? "LƯU TRÒ CHƠI" : "SAVE GAME");
        btnPauseSettings.setText(isVi ? "CÀI ĐẶT" : "SETTINGS");
        btnQuit.setText(isVi ? "THOÁT" : "QUIT TO MENU");

        // Game Over
        titleGameOver.setText(isVi ? "THUA RỒI!" : "GAME OVER");
        btnPlayAgain.setText(isVi ? "CHƠI LẠI" : "PLAY AGAIN");
        btnGameOverSave.setText(isVi ? "LƯU VỊ TRÍ" : "SAVE GAME");
        btnGameOverLeaderboard.setText(isVi ? "BẢNG XẾP HẠNG" : "LEADERBOARD");
        btnGameOverQuit.setText(isVi ? "THOÁT" : "QUIT TO MENU");
        hintGameOver.setText(isVi ? "LƯU, CHƠI LẠI HOẶC VỀ MENU" : "SAVE, RESTART OR RETURN TO MENU");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void notifySave(boolean success) {
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(isVi ? (success ? "ĐÃ LƯU" : "LƯU THẤT BẠI")
                : (success ? "SAVED" : "SAVE FAILED"));
        alert.setHeaderText(null);
        alert.setContentText(isVi ? (success ? "Đã lưu game offline thành công." : "Không thể lưu game.")
                : (success ? "Game saved offline successfully." : "Unable to save the game."));
        alert.showAndWait();
    }
}
