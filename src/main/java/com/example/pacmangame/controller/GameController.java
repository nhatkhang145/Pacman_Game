package com.example.pacmangame.controller;

import com.example.pacmangame.dao.GameSessionDAO;
import com.example.pacmangame.model.Direction;
import com.example.pacmangame.model.GameConfig;
import com.example.pacmangame.model.GameState;
import com.example.pacmangame.model.Ghost;
import com.example.pacmangame.model.GhostState;
import com.example.pacmangame.model.GhostType;
import com.example.pacmangame.model.MapManager;
import com.example.pacmangame.model.Pacman;
import com.example.pacmangame.model.SoundManager;
import com.example.pacmangame.view.GameView;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

public class GameController {
    private int[][] map;
    private int cols = 20;
    private int rows = 19;

    private int lastPacmanGridX = -1;
    private int lastPacmanGridY = -1;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer gameLoop;

    private Pacman pacman;
    private List<Ghost> ghosts; // Quản lý danh sách các con ma
    private GameView gameView;

    private int score = 0;
    private int lives = 3;
    private int comboMultiplier = 1;
    private boolean extraLifeGiven = false;

    private int level = 1;
    private int totalDots = 0;

    private boolean isGameOver = false;
    private boolean gameStarted = false;
    private boolean isPaused = false;
    private long pauseEndTime = 0;
    private long frightenedEndTime = 0;

    private boolean isInvincible = false;
    private long invincibleEndTime = 0;
    private int nextExtraLifeIndex = 0;
    private static final int[] EXTRA_LIFE_SCORE_THRESHOLDS = {10000, 25000, 50000, 100000};

    private GameState gameState = GameState.MENU;

    public GameController() {
        canvas = new Canvas(cols * GameConfig.TILE_SIZE, rows * GameConfig.TILE_SIZE);
        gc = canvas.getGraphicsContext2D();

        ghosts = new ArrayList<>();

        initLevel();
        setupGameLoop();
    }

    private void initLevel() {
        int[][] newMap = MapManager.loadMap(level);
        map = new int[rows][cols];
        totalDots = 0;
        GameConfig.gateOpen = false; // Đóng cửa sập khi bắt đầu level mới

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                map[r][c] = newMap[r][c];
                if (map[r][c] == 2 || map[r][c] == 3) {
                    totalDots++;
                }
            }
        }
        resetPositions();
    }

    private void resetPositions() {
        // Khởi tạo Pac-Man (Ở hàng 12, cột 9 là ô trống ngay dưới chuồng ma)
        pacman = new Pacman(9 * GameConfig.TILE_SIZE, 12 * GameConfig.TILE_SIZE);

        ghosts.clear();
        // Khởi tạo Blinky (Đỏ) - Xuất phát ngoài chuồng (Hàng 8, cột 9)
        ghosts.add(new Ghost(9 * GameConfig.TILE_SIZE, 8 * GameConfig.TILE_SIZE, GhostType.BLINKY));
        // Khởi tạo Pinky (Hồng) - Xuất phát trong chuồng
        ghosts.add(new Ghost(9 * GameConfig.TILE_SIZE, 9 * GameConfig.TILE_SIZE, GhostType.PINKY));
        // Khởi tạo Inky (Xanh) - Xuất phát trong chuồng
        ghosts.add(new Ghost(10 * GameConfig.TILE_SIZE, 9 * GameConfig.TILE_SIZE, GhostType.INKY));
        // Khởi tạo Clyde (Cam) - Xuất phát trong chuồng (x=9, y=10 để không dính tường)
        ghosts.add(new Ghost(9 * GameConfig.TILE_SIZE, 10 * GameConfig.TILE_SIZE, GhostType.CLYDE));

        pacman.setSpeed(2);
        for (Ghost g : ghosts) {
            g.setSpeed(1);
        }
    }

    private long getUpdateInterval() {
        // Màn 1 bắt đầu ở 60 FPS, mỗi màn tăng thêm 4 FPS để tạo cảm giác nhanh dần nhẹ nhàng
        int targetFPS = 60 + (level - 1) * 4;
        return 1_000_000_000L / targetFPS;
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    update();
                    render();
                    return;
                }

                long elapsed = now - lastUpdate;
                long updateInterval = getUpdateInterval();
                if (elapsed >= updateInterval) {
                    update();
                    lastUpdate = now - (elapsed % updateInterval);
                }
                render();
            }
        };
    }

    public void start() {
        gameLoop.start();
    }

    public void stop() {
        gameLoop.stop();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.W || code == KeyCode.UP) {
            pacman.setNextDirection(Direction.UP);
        } else if (code == KeyCode.S || code == KeyCode.DOWN) {
            pacman.setNextDirection(Direction.DOWN);
        } else if (code == KeyCode.A || code == KeyCode.LEFT) {
            pacman.setNextDirection(Direction.LEFT);
        } else if (code == KeyCode.D || code == KeyCode.RIGHT) {
            pacman.setNextDirection(Direction.RIGHT);
        } else if (code == KeyCode.ESCAPE) {
            if (gameState == GameState.PLAYING) {
                gameView.showPauseMenu();
            } else if (gameState == GameState.PAUSED) {
                gameView.showGame();
            }
        }
    }

    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public int getScore() {
        return score;
    }

    public boolean hasSavedGame() {
        return GameSessionDAO.getInstance().hasSave();
    }

    public boolean canContinueGame() {
        return isGameActive() || hasSavedGame();
    }

    public boolean saveCurrentGame() {
        if (isGameOver) {
            return false;
        }

        return GameSessionDAO.getInstance().save(captureSnapshot());
    }

    public boolean continueSavedGame() {
        GameSessionDAO.Snapshot snapshot = GameSessionDAO.getInstance().load();
        if (snapshot == null) {
            return false;
        }

        applySnapshot(snapshot);
        return true;
    }

    public void clearSavedGame() {
        GameSessionDAO.getInstance().clear();
    }

    public void resetGame() {
        score = 0;
        lives = 3;
        level = 1;
        comboMultiplier = 1;
        extraLifeGiven = false;
        nextExtraLifeIndex = 0;
        isInvincible = false;
        invincibleEndTime = 0;
        isGameOver = false;
        gameStarted = true;
        isPaused = false;
        initLevel();
    }

    public boolean isGameActive() {
        return gameStarted && !isGameOver;
    }

    private void update() {
        if (gameState != GameState.PLAYING)
            return;
        if (isGameOver)
            return;

        long now = System.currentTimeMillis();

        if (isPaused) {
            if (now > pauseEndTime) {
                isPaused = false;
            } else {
                return;
            }
        }

        if (isInvincible && now > invincibleEndTime) {
            isInvincible = false;
        }

        for (Ghost g : ghosts) {
            if (g.getState() == GhostState.FRIGHTENED) {
                if (now > frightenedEndTime) {
                    g.setState(GhostState.CHASE);
                }
            }
        }

        // Dịch chuyển Pac-Man nếu đi qua đường hầm (xuyên qua cạnh màn hình)
        teleportEntity(pacman);

        pacman.update(map);

        // Tìm Blinky để truyền vào update cho Inky
        Ghost blinky = ghosts.stream().filter(g -> g.getType() == GhostType.BLINKY).findFirst().orElse(null);

        for (Ghost g : ghosts) {
            teleportEntity(g);
            g.update(map, pacman, blinky);
        }

        checkEat();
        checkCollisions();
    }

    // Xử lý điệu kiện đường hầm (đi ra cạnh trái vòng sang phải)
    private void teleportEntity(Pacman p) {
        if (p.getX() < 0)
            p.setX((int) (canvas.getWidth() - GameConfig.TILE_SIZE));
        if (p.getX() >= canvas.getWidth())
            p.setX(0);
    }

    private void teleportEntity(Ghost g) {
        if (g.getX() < 0)
            g.setX((int) (canvas.getWidth() - GameConfig.TILE_SIZE));
        if (g.getX() >= canvas.getWidth())
            g.setX(0);
    }

    private void checkEat() {
        if (pacman.getX() % GameConfig.TILE_SIZE == 0 && pacman.getY() % GameConfig.TILE_SIZE == 0) {
            int gridX = pacman.getGridX();
            int gridY = pacman.getGridY();

            // Xử lý giẫm lên Công tắc (Switch = 8)
            if ((gridX != lastPacmanGridX || gridY != lastPacmanGridY) && map[gridY][gridX] == 8) {
                GameConfig.gateOpen = !GameConfig.gateOpen;
                SoundManager.getInstance().playSound("powerup"); // Chơi âm thanh khi bật công tắc
                lastPacmanGridX = gridX;
                lastPacmanGridY = gridY;
            } else if (gridX != lastPacmanGridX || gridY != lastPacmanGridY) {
                lastPacmanGridX = gridX;
                lastPacmanGridY = gridY;
            }

            if (gridY >= 0 && gridY < rows && gridX >= 0 && gridX < cols) {
                if (map[gridY][gridX] == 2) { // Hạt nhỏ
                    map[gridY][gridX] = 0;
                    addScore(10);
                    totalDots--;
                    SoundManager.getInstance().playSound("chomp");
                } else if (map[gridY][gridX] == 3) { // Hạt to (Power Pellet)
                    map[gridY][gridX] = 0;
                    addScore(50);
                    totalDots--;
                    SoundManager.getInstance().playSound("powerup");

                    comboMultiplier = 1;
                    for (Ghost g : ghosts) {
                        g.setState(GhostState.FRIGHTENED);
                    }
                    frightenedEndTime = System.currentTimeMillis() + 7000;
                }
            }

            if (totalDots == 0) {
                level++;
                isPaused = true;
                pauseEndTime = System.currentTimeMillis() + 2000;
                initLevel();
            }
        }
    }

    private void addScore(int points) {
        score += points;
        checkExtraLife();
    }

    private void checkExtraLife() {
        while (nextExtraLifeIndex < EXTRA_LIFE_SCORE_THRESHOLDS.length &&
                score >= EXTRA_LIFE_SCORE_THRESHOLDS[nextExtraLifeIndex]) {
            lives++;
            nextExtraLifeIndex++;
            extraLifeGiven = true;
        }
    }

    private void checkCollisions() {
        if (isInvincible) {
            return;
        }

        for (Ghost g : ghosts) {
            double dx = pacman.getX() - g.getX();
            double dy = pacman.getY() - g.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < GameConfig.TILE_SIZE - 4) {
                if (g.getState() == GhostState.CHASE || g.getState() == GhostState.SCATTER) {
                    SoundManager.getInstance().playSound("death");
                    System.out.println("[DEBUG] Collision with ghost " + g.getType() + " at (pacman=" + pacman.getX()
                            + "," + pacman.getY() + ") (ghost=" + g.getX() + "," + g.getY() + ") - lives before="
                            + lives);
                    lives--;
                    System.out.println("[DEBUG] lives after decrement=" + lives);
                    isPaused = true;
                    pauseEndTime = System.currentTimeMillis() + 2000;

                    if (lives == 0) {
                        isGameOver = true;
                        gameStarted = false;
                        clearSavedGame();
                        if (gameView != null) {
                            gameView.showGameOver(score);
                        }
                    } else {
                        resetPositions();
                        setInvincible(2000);
                    }
                    break; // Ngừng kiểm tra các ma khác
                } else if (g.getState() == GhostState.FRIGHTENED) {
                    SoundManager.getInstance().playSound("eatghost");
                    addScore(200 * comboMultiplier);
                    comboMultiplier *= 2;
                    g.setState(GhostState.EYES);
                }
            }
        }
    }

    private void render() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
int tileX = col * GameConfig.TILE_SIZE;
            int tileY = row * GameConfig.TILE_SIZE;

                if (map[row][col] == 1) {
                    if (level == 1) {
                        // Tường neon retro xanh lơ
                        gc.setFill(Color.rgb(0, 0, 80));
                        gc.fillRect(tileX, tileY, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
                        gc.setStroke(Color.CYAN);
                        gc.setLineWidth(2);
                        gc.strokeRoundRect(tileX + 2, tileY + 2, GameConfig.TILE_SIZE - 4, GameConfig.TILE_SIZE - 4, 8, 8);
                    } else {
                        // Phân Xưởng Cơ Học: Tường màu Đồng/Cam
                        gc.setFill(Color.rgb(80, 40, 0));
                        gc.fillRect(tileX, tileY, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
                        gc.setStroke(Color.ORANGE);
                        gc.setLineWidth(2);
                        gc.strokeRoundRect(tileX + 2, tileY + 2, GameConfig.TILE_SIZE - 4, GameConfig.TILE_SIZE - 4, 8, 8);
                    }
                } else if (map[row][col] == 2) {
                    gc.setFill(Color.WHITE);
                    double dotSize = 6;
                    double offset = (GameConfig.TILE_SIZE - dotSize) / 2.0;
                    gc.fillOval(tileX + offset, tileY + offset, dotSize, dotSize);
                } else if (map[row][col] == 3) {
                    gc.setFill(Color.WHITE);
                    // Hiệu ứng hạt to nhấp nháy/phóng to thu nhỏ
                    long time = System.currentTimeMillis();
                    double pulse = Math.sin(time / 150.0);
                    double dotSize = 12 + pulse * 3; // Kích thước dao động từ 9 đến 15
                    double offset = (GameConfig.TILE_SIZE - dotSize) / 2.0;
                    gc.fillOval(tileX + offset, tileY + offset, dotSize, dotSize);
                } else if (map[row][col] >= 4 && map[row][col] <= 7) {
                    // Vẽ băng chuyền
                    gc.setFill(Color.rgb(100, 100, 100)); // Màu xám cho băng chuyền
                    gc.fillRect(tileX + 2, tileY + 2, GameConfig.TILE_SIZE - 4, GameConfig.TILE_SIZE - 4);
                    gc.setFill(Color.YELLOW);
                    double cx = tileX + GameConfig.TILE_SIZE / 2.0;
                    double cy = tileY + GameConfig.TILE_SIZE / 2.0;
                    // Vẽ mũi tên đơn giản
                    if (map[row][col] == 4)
                        gc.fillPolygon(new double[] { cx - 5, cx + 5, cx }, new double[] { cy + 5, cy + 5, cy - 5 }, 3); // UP
                    if (map[row][col] == 5)
                        gc.fillPolygon(new double[] { cx - 5, cx + 5, cx }, new double[] { cy - 5, cy - 5, cy + 5 }, 3); // DOWN
                    if (map[row][col] == 6)
                        gc.fillPolygon(new double[] { cx + 5, cx + 5, cx - 5 }, new double[] { cy - 5, cy + 5, cy }, 3); // LEFT
                    if (map[row][col] == 7)
                        gc.fillPolygon(new double[] { cx - 5, cx - 5, cx + 5 }, new double[] { cy - 5, cy + 5, cy }, 3); // RIGHT
                } else if (map[row][col] == 8) {
                    // Công tắc (Switch)
                    gc.setFill(Color.rgb(50, 50, 50));
                    gc.fillRect(tileX + 4, tileY + 4, GameConfig.TILE_SIZE - 8, GameConfig.TILE_SIZE - 8);
                    gc.setFill(GameConfig.gateOpen ? Color.GREEN : Color.RED);
                    gc.fillOval(tileX + 8, tileY + 8, GameConfig.TILE_SIZE - 16, GameConfig.TILE_SIZE - 16);
                } else if (map[row][col] == 9) {
                    // Cửa sập (Gate)
                    if (!GameConfig.gateOpen) {
                        gc.setFill(Color.rgb(200, 0, 0)); // Đóng (màu đỏ)
                        gc.fillRect(tileX, tileY + GameConfig.TILE_SIZE / 4.0, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE / 2.0);
                        gc.setStroke(Color.WHITE);
                        gc.strokeLine(tileX, tileY + GameConfig.TILE_SIZE / 2.0, tileX + GameConfig.TILE_SIZE, tileY + GameConfig.TILE_SIZE / 2.0);
                    } else {
                        gc.setFill(Color.rgb(0, 100, 0, 0.5)); // Mở (màu xanh lá bán trong suốt)
                        gc.fillRect(tileX, tileY, GameConfig.TILE_SIZE, 4);
                        gc.fillRect(tileX, tileY + GameConfig.TILE_SIZE - 4, GameConfig.TILE_SIZE, 4);
                    }
                }
            }
        }

        if (!isGameOver) {
            if (isInvincible) {
                long blink = System.currentTimeMillis() % 400;
                gc.setGlobalAlpha(blink < 200 ? 0.35 : 1.0);
            }
            pacman.render(gc);
            gc.setGlobalAlpha(1.0);
            for (Ghost g : ghosts) {
                g.render(gc);
            }
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("SCORE: " + score, 10, 20);
        gc.fillText("LEVEL: " + level, canvas.getWidth() / 2 - 30, 20);

        if (isInvincible) {
            gc.setFill(Color.LIME);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText("INVINCIBLE", 10, 38);
        }

        drawLives(gc);
    }

    private void drawLives(GraphicsContext gc) {
        double heartSize = 12;
        double startX = canvas.getWidth() - 90;
        double y = 8;

        for (int i = 0; i < lives; i++) {
            double x = startX + i * (heartSize + 6);
            gc.setFill(Color.RED);
            gc.fillOval(x, y, heartSize / 2, heartSize / 2);
            gc.fillOval(x + heartSize / 2, y, heartSize / 2, heartSize / 2);
            gc.fillPolygon(new double[] { x, x + heartSize, x + heartSize / 2 },
                    new double[] { y + heartSize / 3, y + heartSize / 3, y + heartSize }, 3);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(x, y, heartSize / 2, heartSize / 2);
            gc.strokeOval(x + heartSize / 2, y, heartSize / 2, heartSize / 2);
            gc.strokePolygon(new double[] { x, x + heartSize, x + heartSize / 2 },
                    new double[] { y + heartSize / 3, y + heartSize / 3, y + heartSize }, 3);
        }

        if (lives > 6) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText("+" + (lives - 6), startX + 6 * (heartSize + 6), y + heartSize - 2);
        }
    }

    private GameSessionDAO.Snapshot captureSnapshot() {
        List<GameSessionDAO.GhostSnapshot> ghostSnapshots = new ArrayList<>();
        for (Ghost ghost : ghosts) {
            ghostSnapshots.add(new GameSessionDAO.GhostSnapshot(ghost.getType(), ghost.getX(), ghost.getY(),
                    ghost.getSpeed(), ghost.getState(), ghost.getCurrentDirection()));
        }

        int[][] mapCopy = new int[map.length][map[0].length];
        for (int row = 0; row < map.length; row++) {
            System.arraycopy(map[row], 0, mapCopy[row], 0, map[row].length);
        }

        long invincibleRemaining = 0;
        if (isInvincible) {
            invincibleRemaining = Math.max(0, invincibleEndTime - System.currentTimeMillis());
        }

        return new GameSessionDAO.Snapshot(score, lives, comboMultiplier, extraLifeGiven, level, totalDots,
                GameConfig.gateOpen, lastPacmanGridX, lastPacmanGridY, pacman.getX(), pacman.getY(),
                pacman.getCurrentDirection(), pacman.getNextDirection(), mapCopy, ghostSnapshots,
                nextExtraLifeIndex, invincibleRemaining);
    }

    private void applySnapshot(GameSessionDAO.Snapshot snapshot) {
        System.out.println("[DEBUG] Applying snapshot: lives=" + snapshot.lives() + ", score=" + snapshot.score()
                + ", level=" + snapshot.level());
        score = snapshot.score();
        lives = snapshot.lives();
        comboMultiplier = snapshot.comboMultiplier();
        extraLifeGiven = snapshot.extraLifeGiven();
        level = snapshot.level();
        totalDots = snapshot.totalDots();
        GameConfig.gateOpen = snapshot.isGateOpen();
        lastPacmanGridX = snapshot.lastPacmanGridX();
        lastPacmanGridY = snapshot.lastPacmanGridY();
        map = snapshot.map();

        pacman = new Pacman(snapshot.pacmanX(), snapshot.pacmanY());
        pacman.setSpeed(2);
        pacman.setCurrentDirection(snapshot.pacmanDirection());
        pacman.setNextDirection(snapshot.pacmanNextDirection());

        ghosts = new ArrayList<>();
        for (GameSessionDAO.GhostSnapshot ghostSnapshot : snapshot.ghostSnapshots()) {
            Ghost ghost = new Ghost(ghostSnapshot.x(), ghostSnapshot.y(), ghostSnapshot.type());
            ghost.setSpeed(1);
            ghost.setState(ghostSnapshot.state());
            ghost.setCurrentDirection(ghostSnapshot.direction());
            ghosts.add(ghost);
        }

        gameStarted = true;
        isGameOver = false;
        isPaused = false;
        pauseEndTime = 0;
        frightenedEndTime = 0;
        nextExtraLifeIndex = snapshot.nextExtraLifeIndex();
        invincibleEndTime = System.currentTimeMillis() + snapshot.invincibleRemainingMillis();
        isInvincible = snapshot.invincibleRemainingMillis() > 0;
    }

    private void setInvincible(long millis) {
        isInvincible = true;
        invincibleEndTime = System.currentTimeMillis() + millis;
    }
}
