package com.example.pacmangame.view;

import com.example.pacmangame.controller.GameController;
import com.example.pacmangame.dao.LeaderboardDAO;
import com.example.pacmangame.model.GameState;
import com.example.pacmangame.model.SettingsManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.Arrays;
import java.util.List;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class GameView {
    private StackPane root;
    private VBox menuScreen;
    private VBox settingsScreen;
    private VBox howToPlayScreen;
    private VBox pauseScreen;
    private VBox gameOverScreen;
    private GameController gameController;
    private GameState previousState = GameState.MENU;

    private int currentMenuIndex = 0;
    private List<Button> menuButtons;

    // UI Elements
    /** UC-15: Tham chieu Stage de dieu khien fullscreen */
    private Stage primaryStage;

    // --- Settings labels (can null-check in updateLanguage) ---
    private Label lblVolume;
    private Label lblLanguage;
    private Label lblFullscreen;   // UC-15

    // --- UC-14: Nut remap phim (hien thi phim hien tai, click de doi) ---
    private Button btnRemapUp;
    private Button btnRemapDown;
    private Button btnRemapLeft;
    private Button btnRemapRight;
    private Button btnRemapPause;

    // Menu buttons
    private Button btnPlay;
    private Button btnContinue;
    private Button btnSettings;
    private Button btnLeaderboard;
    private Button btnHowToPlay;
    private Button btnBack;
    private Text titleMenu;
    private Text titleSettings;
    private Text titleHowToPlay;
    private Text instructionsText;
    private Button btnBackFromHowToPlay;

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

    private enum LeaderboardMode {
        TOP_SCORES,
        RECENT_RUNS
    }


    /**
     *   controller chinh cua game
     *   cua so chinh, dung de bat/tat fullscreen 
     */
    public GameView(GameController gameController, Stage primaryStage) {
        this.gameController = gameController;
        this.gameController.setGameView(this);
        this.primaryStage = primaryStage;  // UC-15: luu Stage de goi setFullScreen()

        this.root = new StackPane();
        this.root.setStyle("-fx-background-color: black;");

        createMenuScreen();
        createSettingsScreen();
        createHowToPlayScreen();
        createPauseScreen();
        createGameOverScreen();

        // Ensure canvas can receive focus for key events
        gameController.getCanvas().setFocusTraversable(true);

        root.getChildren().addAll(gameController.getCanvas(), settingsScreen, howToPlayScreen, pauseScreen, gameOverScreen, menuScreen);

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
            gameController.clearSavedGame();
            gameController.resetGame();
            fadeToGame(menuScreen);
        });

        btnContinue = createRetroButton("");
        btnContinue.setDisable(true);
        btnContinue.setOnAction(e -> {
            if (gameController.continueSavedGame()) {
                fadeToGame(menuScreen);
            } else {
                showAlert("Continue", "No saved game found.");
            }
        });

        btnLeaderboard = createRetroButton("");
        btnLeaderboard.setOnAction(e -> showLeaderboard());

        btnSettings = createRetroButton("");
        btnSettings.setOnAction(e -> showSettings());

        btnHowToPlay = createRetroButton("");
        btnHowToPlay.setOnAction(e -> showHowToPlay());

        menuScreen.getChildren().addAll(titleMenu, btnPlay, btnContinue, btnLeaderboard, btnSettings, btnHowToPlay);

        menuButtons = Arrays.asList(btnPlay, btnContinue, btnLeaderboard, btnSettings, btnHowToPlay);
        updateMenuSelection();
    }

    public void navigateMenu(int direction) {
        if (menuButtons == null || menuButtons.isEmpty()) return;
        
        int newIndex = currentMenuIndex;
        do {
            newIndex = (newIndex + direction + menuButtons.size()) % menuButtons.size();
        } while (newIndex != currentMenuIndex && menuButtons.get(newIndex).isDisabled());
        
        currentMenuIndex = newIndex;
        updateMenuSelection();
    }

    public void selectCurrentMenu() {
        if (menuButtons != null && !menuButtons.isEmpty() && !menuButtons.get(currentMenuIndex).isDisabled()) {
            menuButtons.get(currentMenuIndex).fire();
        }
    }

    public void updateMenuSelection() {
        if (menuButtons == null) return;
        for (int i = 0; i < menuButtons.size(); i++) {
            Button btn = menuButtons.get(i);
            if (i == currentMenuIndex) {
                btn.setStyle("-fx-background-color: blue; -fx-text-fill: white; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: black; -fx-text-fill: yellow; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            }
        }
    }

    /**
     * UC-08 / UC-13 / UC-14 / UC-15
     * Xay dung man hinh Settings gom 3 khu vuc:
     *   1. Am thanh + Ngon ngu  (UC-08/UC-13)
     *   2. Dieu khien           (UC-14) - chon thiet bi, remap 5 phim
     *   3. Hien thi             (UC-15) - theme + fullscreen
     */
    private void createSettingsScreen() {
        settingsScreen = new VBox(14);
        settingsScreen.setAlignment(Pos.CENTER);
        settingsScreen.setStyle("-fx-background-color: black;");
        settingsScreen.setPadding(new Insets(20));

        titleSettings = new Text("SETTINGS");
        titleSettings.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        titleSettings.setStyle("-fx-fill: yellow;");

        // ---- UC-08/UC-13: Am luong ----
        // Lang nghe Slider de cap nhat volume theo thoi gian thuc.
        lblVolume = new Label("AM LUONG:");
        lblVolume.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblVolume.setStyle("-fx-text-fill: white;");
        Slider volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setMaxWidth(280);
        volumeSlider.valueProperty().addListener((obs, o, n) ->
                SettingsManager.getInstance().setVolume(n.doubleValue() / 100.0));

        // ---- UC-08: Ngon ngu ----
        // Khi doi ngon ngu, goi updateLanguage() de lam moi toan bo text.
        lblLanguage = new Label("NGON NGU:");
        lblLanguage.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblLanguage.setStyle("-fx-text-fill: white;");
        ComboBox<SettingsManager.Language> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(SettingsManager.Language.values());
        langCombo.setValue(SettingsManager.getInstance().getLanguage());
        langCombo.setOnAction(e -> {
            SettingsManager.getInstance().setLanguage(langCombo.getValue());
            updateLanguage();
        });

        // UC-14: Tuy chinh dieu khien
        Separator sep1 = new Separator();
        sep1.setMaxWidth(320);


        // --- Remap phim ---
        // Moi hang gom: nhan hien thi + nut remap.
        // Nhan nut → goi startKeyCapture() de cho nguoi choi nhan phim moi.
        SettingsManager sm = SettingsManager.getInstance();
        btnRemapUp    = buildRemapRow(sm.getKeyUp());
        btnRemapDown  = buildRemapRow(sm.getKeyDown());
        btnRemapLeft  = buildRemapRow(sm.getKeyLeft());
        btnRemapRight = buildRemapRow(sm.getKeyRight());
        btnRemapPause = buildRemapRow(sm.getKeyPause());

        // Khi click, bat dau che do "doi phim": hien thi "...", cho nhan phim moi
        btnRemapUp.setOnAction(e    -> startKeyCapture(btnRemapUp,    KeyAction.UP));
        btnRemapDown.setOnAction(e  -> startKeyCapture(btnRemapDown,  KeyAction.DOWN));
        btnRemapLeft.setOnAction(e  -> startKeyCapture(btnRemapLeft,  KeyAction.LEFT));
        btnRemapRight.setOnAction(e -> startKeyCapture(btnRemapRight, KeyAction.RIGHT));
        btnRemapPause.setOnAction(e -> startKeyCapture(btnRemapPause, KeyAction.PAUSE));

        // GridPane: col 0 = nhan mo ta, col 1 = nut phim
        GridPane remapGrid = new GridPane();
        remapGrid.setHgap(10);
        remapGrid.setVgap(6);
        remapGrid.setAlignment(Pos.CENTER);
        String[] labels = {"[Len]", "[Xuong]", "[Trai]", "[Phai]", "[Pause]"};
        Button[] btns   = {btnRemapUp, btnRemapDown, btnRemapLeft, btnRemapRight, btnRemapPause};
        for (int i = 0; i < labels.length; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px;");
            remapGrid.add(lbl,    0, i);
            remapGrid.add(btns[i], 1, i);
        }


        // --- Fullscreen ---
        // UC-15: CheckBox gui lenh stage.setFullScreen() de bat/tat toan man hinh.
        lblFullscreen = new Label("TOAN MAN HINH:");
        lblFullscreen.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblFullscreen.setStyle("-fx-text-fill: white;");
        CheckBox cbFullscreen = new CheckBox();
        cbFullscreen.setSelected(SettingsManager.getInstance().isFullscreen());
        cbFullscreen.setStyle("-fx-text-fill: white;");
        cbFullscreen.setOnAction(e -> {
            boolean on = cbFullscreen.isSelected();
            SettingsManager.getInstance().setFullscreen(on);
            if (primaryStage != null) primaryStage.setFullScreen(on);
        });
        HBox fsRow = new HBox(10, lblFullscreen, cbFullscreen);
        fsRow.setAlignment(Pos.CENTER);

        // --- Nut Quay lai ---
        btnBack = createRetroButton("");
        btnBack.setOnAction(e -> backFromSettings());

        settingsScreen.getChildren().addAll(
                titleSettings,
                lblVolume, volumeSlider,
                lblLanguage, langCombo,
                sep1,
                remapGrid,
                fsRow,
                btnBack
        );
    }


     // UC-14 – Tao mot nut hien thi phim hien tai.
    private Button buildRemapRow(KeyCode key) {
        Button btn = new Button(key.getName());
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        btn.setMinWidth(110);
        btn.setStyle("-fx-background-color: #111; -fx-text-fill: yellow;"
                + " -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 4;"
                + " -fx-background-radius: 4; -fx-cursor: hand;");
        return btn;
    }

    private enum KeyAction { UP, DOWN, LEFT, RIGHT, PAUSE }


     // UC-14 – Bat dau che do "cho phim moi".
    private void startKeyCapture(Button targetBtn, KeyAction action) {
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;
        // Bao nguoi choi biet dang cho nhan phim
        targetBtn.setText(isVi ? "... Nhan phim ..." : "... Press key ...");
        targetBtn.setStyle("-fx-background-color: #1a1a5a; -fx-text-fill: cyan;"
                + " -fx-border-color: cyan; -fx-border-width: 2; -fx-border-radius: 4;"
                + " -fx-background-radius: 4;");
        // Lang nghe phim ke tiep tren settingsScreen
        settingsScreen.setOnKeyPressed(event -> {
            KeyCode newKey = event.getCode();
            SettingsManager sm = SettingsManager.getInstance();
            // Ghi nhan phim moi vao SettingsManager theo hanh dong tuong ung
            switch (action) {
                case UP    -> sm.setKeyUp(newKey);
                case DOWN  -> sm.setKeyDown(newKey);
                case LEFT  -> sm.setKeyLeft(newKey);
                case RIGHT -> sm.setKeyRight(newKey);
                case PAUSE -> sm.setKeyPause(newKey);
            }
            // Cap nhat text nut ve phim moi va khoi phuc style binh thuong
            targetBtn.setText(newKey.getName());
            targetBtn.setStyle("-fx-background-color: #111; -fx-text-fill: yellow;"
                    + " -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 4;"
                    + " -fx-background-radius: 4; -fx-cursor: hand;");
            // Huy lang nghe sau khi da nhan duoc phim
            settingsScreen.setOnKeyPressed(null);
        });
        // Cho settingsScreen nhan focus de bat duoc phim
        settingsScreen.requestFocus();
        settingsScreen.setFocusTraversable(true);
    }

    private void createHowToPlayScreen() {
        howToPlayScreen = new VBox(20);
        howToPlayScreen.setAlignment(Pos.CENTER);
        howToPlayScreen.setStyle("-fx-background-color: black;");

        titleHowToPlay = new Text("HOW TO PLAY");
        titleHowToPlay.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        titleHowToPlay.setStyle("-fx-fill: yellow;");

        instructionsText = new Text("Move: W, A, S, D or ARROW KEYS");
        instructionsText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        instructionsText.setStyle("-fx-fill: white;");

        btnBackFromHowToPlay = createRetroButton("");
        btnBackFromHowToPlay.setOnAction(e -> showMenu());

        howToPlayScreen.getChildren().addAll(titleHowToPlay, instructionsText, btnBackFromHowToPlay);
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
        btnResume.setOnAction(e -> fadeToGame(pauseScreen));

        btnPauseSave = createRetroButton("");
        btnPauseSave.setOnAction(e -> notifySave(gameController.saveCurrentGame()));

        btnPauseSettings = createRetroButton("");
        btnPauseSettings.setOnAction(e -> showSettings());

        btnQuit = createRetroButton("");
        btnQuit.setOnAction(e -> {
            notifySave(gameController.saveCurrentGame());
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
            gameController.clearSavedGame();
            gameController.resetGame();
            fadeToGame(gameOverScreen);
        });

        btnGameOverSave = createRetroButton("");
        btnGameOverSave.setOnAction(e -> notifySave(gameController.saveCurrentGame()));

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
        btn.setStyle("-fx-background-color: black; -fx-text-fill: yellow; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> {
            if (menuButtons != null && menuButtons.contains(btn)) {
                currentMenuIndex = menuButtons.indexOf(btn);
                updateMenuSelection();
            } else {
                btn.setStyle("-fx-background-color: blue; -fx-text-fill: white; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (menuButtons != null && menuButtons.contains(btn)) {
                updateMenuSelection();
            } else {
                btn.setStyle("-fx-background-color: black; -fx-text-fill: yellow; -fx-border-color: blue; -fx-border-width: 3px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            }
        });

        return btn;
    }

    public void showMenu() {
        gameController.getCanvas().setVisible(false);
        settingsScreen.setVisible(false);
        howToPlayScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        menuScreen.setVisible(true);
        gameController.setGameState(GameState.MENU);
        // Stop the game loop while in the menu to save CPU and avoid unexpected input
        gameController.stop();

        if (gameController.canContinueGame()) {
            btnContinue.setDisable(false);
        } else {
            btnContinue.setDisable(true);
            if (currentMenuIndex == 1) currentMenuIndex = 0;
        }
        updateMenuSelection();
    }

    public void showSettings() {
        previousState = gameController.getGameState();

        // Hide UI elements to show settings
        menuScreen.setVisible(false);
        howToPlayScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);

        // Do not hide the canvas if we are coming from PAUSED, so settings overlay on
        // top of game
        if (previousState != GameState.PAUSED) {
            gameController.getCanvas().setVisible(false);
        }

        settingsScreen.setVisible(true);
        gameController.setGameState(GameState.SETTINGS);
    }

    private void backFromSettings() {
        if (previousState == GameState.PAUSED) {
            showPauseMenu();
        } else {
            showMenu();
        }
    }

    public void showHowToPlay() {
        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        gameController.getCanvas().setVisible(false);
        howToPlayScreen.setVisible(true);
        gameController.setGameState(GameState.HOW_TO_PLAY);
    }

    public void showGame() {
        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        howToPlayScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        gameController.getCanvas().setVisible(true);
        gameController.getCanvas().requestFocus();
        gameController.setGameState(GameState.PLAYING);
        // Ensure the game loop is running when entering the game
        gameController.start();
    }

    public void showPauseMenu() {
        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        howToPlayScreen.setVisible(false);
        gameOverScreen.setVisible(false);
        gameController.getCanvas().setVisible(true);
        pauseScreen.setVisible(true);
        gameController.setGameState(GameState.PAUSED);
    }

    public void showGameOver(int score) {
        // Stop the game loop to freeze the canvas and avoid animation interference
        gameController.stop();

        // Show the Game Over overlay first so the user sees it immediately.
        // Schedule the optional score recording to run after the overlay is rendered
        // to avoid blocking the UI thread before the overlay becomes visible.
        javafx.application.Platform.runLater(() -> recordScore(score));

        menuScreen.setVisible(false);
        settingsScreen.setVisible(false);
        howToPlayScreen.setVisible(false);
        pauseScreen.setVisible(false);
        gameController.getCanvas().setVisible(true);

        finalScoreGameOver.setText((SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI
                ? "ĐIỂM CUỐI: "
                : "FINAL SCORE: ") + score);

        // Make sure the overlay fills the window and is in front
        gameOverScreen.setPrefSize(root.getWidth(), root.getHeight());
        gameOverScreen.setVisible(true);
        gameOverScreen.toFront();

        gameController.setGameState(GameState.GAME_OVER);
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

        LeaderboardDAO.getInstance().addScore(playerName, score);
    }

    public void showLeaderboard() {
        boolean isVi = SettingsManager.getInstance().getLanguage() == SettingsManager.Language.VI;
        String title = isVi ? "BẢNG XẾP HẠNG OFFLINE" : "OFFLINE LEADERBOARD";
        showLeaderboardWindow(title, isVi);
    }

    private void showLeaderboardWindow(String title, boolean isVi) {
        Stage stage = new Stage();
        stage.initOwner(root.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);

        VBox container = new VBox(16);
        container.setPadding(new Insets(18, 22, 20, 22));
        container.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0f1a, #0d1b2a);"
            + "-fx-border-color: #1b263b; -fx-border-width: 2; -fx-border-radius: 12;"
            + "-fx-background-radius: 12;");

        Text header = new Text(title);
        header.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 26));
        header.setFill(Color.web("#ffd166"));
        header.setEffect(new DropShadow(12, Color.web("#000000")));

        Text subtitle = new Text(isVi ? "Tổng hợp thành tích và lịch sử gần đây" : "Top performance and recent runs");
        subtitle.setFont(Font.font("Verdana", FontWeight.NORMAL, 12));
        subtitle.setFill(Color.web("#b7c5d6"));

        Label modeLabel = new Label(isVi ? "Chế độ" : "View");
        modeLabel.setTextFill(Color.web("#b7c5d6"));
        ChoiceBox<LeaderboardMode> modeChoice = new ChoiceBox<>();
        modeChoice.getItems().addAll(LeaderboardMode.TOP_SCORES, LeaderboardMode.RECENT_RUNS);
        modeChoice.setValue(LeaderboardMode.TOP_SCORES);
        modeChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(LeaderboardMode mode) {
                if (mode == null) {
                    return "";
                }
                return switch (mode) {
                    case TOP_SCORES -> isVi ? "Top điểm" : "Top scores";
                    case RECENT_RUNS -> isVi ? "Lịch sử gần đây" : "Recent runs";
                };
            }

            @Override
            public LeaderboardMode fromString(String string) {
                return LeaderboardMode.TOP_SCORES;
            }
        });

        Label periodLabel = new Label(isVi ? "Thời gian" : "Period");
        periodLabel.setTextFill(Color.web("#b7c5d6"));
        ChoiceBox<LeaderboardDAO.Period> periodChoice = new ChoiceBox<>();
        periodChoice.getItems().addAll(LeaderboardDAO.Period.ALL, LeaderboardDAO.Period.DAY,
                LeaderboardDAO.Period.WEEK, LeaderboardDAO.Period.MONTH);
        periodChoice.setValue(LeaderboardDAO.Period.ALL);
        periodChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(LeaderboardDAO.Period period) {
                if (period == null) {
                    return "";
                }
                return switch (period) {
                    case ALL -> isVi ? "Tất cả" : "All";
                    case DAY -> isVi ? "24 giờ" : "24 hours";
                    case WEEK -> isVi ? "7 ngày" : "7 days";
                    case MONTH -> isVi ? "30 ngày" : "30 days";
                };
            }

            @Override
            public LeaderboardDAO.Period fromString(String string) {
                return LeaderboardDAO.Period.ALL;
            }
        });

        Label playerLabel = new Label(isVi ? "Người chơi" : "Player");
        playerLabel.setTextFill(Color.web("#b7c5d6"));
        TextField playerField = new TextField();
        playerField.setPromptText(isVi ? "Bỏ trống để tất cả" : "Leave empty for all");
        playerField.setStyle("-fx-background-color: #0b1320; -fx-text-fill: #e9ecef;"
                + "-fx-border-color: #1f2f46; -fx-border-radius: 6; -fx-background-radius: 6;");

        Button btnApply = new Button(isVi ? "ÁP DỤNG" : "APPLY");
        btnApply.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        btnApply.setStyle("-fx-background-color: #4cc9f0; -fx-text-fill: #0b1320;"
                + "-fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");

        Button btnClear = new Button(isVi ? "XÓA LỌC" : "CLEAR");
        btnClear.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        btnClear.setStyle("-fx-background-color: #1f2f46; -fx-text-fill: #e9ecef;"
                + "-fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefColumnCount(36);
        area.setPrefRowCount(16);
        area.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px; -fx-control-inner-background: #0b1320;"
            + "-fx-text-fill: #e9ecef; -fx-highlight-fill: #4361ee; -fx-border-color: #1f2f46;"
            + "-fx-border-radius: 8; -fx-background-radius: 8;");

        Runnable refresh = () -> {
            LeaderboardMode mode = modeChoice.getValue();
            LeaderboardDAO.Period period = periodChoice.getValue();
            String playerFilter = playerField.getText();
            area.setText(buildLeaderboardContent(mode, period, playerFilter, isVi));
        };

        btnApply.setOnAction(e -> refresh.run());
        btnClear.setOnAction(e -> {
            modeChoice.setValue(LeaderboardMode.TOP_SCORES);
            periodChoice.setValue(LeaderboardDAO.Period.ALL);
            playerField.clear();
            refresh.run();
        });

        modeChoice.setOnAction(e -> refresh.run());
        periodChoice.setOnAction(e -> refresh.run());
        playerField.setOnAction(e -> refresh.run());

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnClose = new Button(isVi ? "ĐÓNG" : "CLOSE");
        btnClose.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        btnClose.setStyle("-fx-background-color: #fcbf49; -fx-text-fill: #1b1b1b;"
            + "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        footer.getChildren().add(btnClose);

        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(12);
        filterGrid.setVgap(8);
        filterGrid.add(modeLabel, 0, 0);
        filterGrid.add(modeChoice, 1, 0);
        filterGrid.add(periodLabel, 2, 0);
        filterGrid.add(periodChoice, 3, 0);
        filterGrid.add(playerLabel, 0, 1);
        filterGrid.add(playerField, 1, 1, 3, 1);

        HBox filterActions = new HBox(10, btnClear, btnApply);
        filterActions.setAlignment(Pos.CENTER_RIGHT);

        VBox filtersBox = new VBox(10, filterGrid, filterActions);
        filtersBox.setPadding(new Insets(6, 0, 6, 0));

        container.getChildren().addAll(header, subtitle, filtersBox, area, footer);

        refresh.run();

        Scene scene = new Scene(container, 520, 440);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private String buildLeaderboardContent(LeaderboardMode mode, LeaderboardDAO.Period period, String playerFilter,
            boolean isVi) {
        if (mode == LeaderboardMode.RECENT_RUNS) {
            return LeaderboardDAO.getInstance().buildRecentRunsText(20, isVi, period, playerFilter);
        }

        return LeaderboardDAO.getInstance().buildLeaderboardTextFiltered(10, isVi, period, playerFilter);
    }

    // UC-08 - Hệ thống cài đặt
    // ID: UC-08
    // Tên UC: Hệ thống cài đặt
    // Chức năng: Cập nhật nội dung text cho toàn bộ giao diện khi thay đổi ngôn ngữ
    // (sử dụng trong ComboBox handler)
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

        // How To Play
        if (btnBackFromHowToPlay != null) btnBackFromHowToPlay.setText(isVi ? "QUAY LẠI" : "BACK");
        if (titleHowToPlay != null) titleHowToPlay.setText(isVi ? "HƯỚNG DẪN" : "HOW TO PLAY");
        if (instructionsText != null) instructionsText.setText(isVi ? "Di chuyển: W, A, S, D hoặc CÁC PHÍM MŨI TÊN" : "Move: W, A, S, D or ARROW KEYS");

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

    private void fadeToGame(javafx.scene.Node screenToFade) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), screenToFade);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            screenToFade.setVisible(false);
            screenToFade.setOpacity(1.0);
            showGame();
        });
        fadeOut.play();
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
