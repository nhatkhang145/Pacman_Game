package com.example.pacmangame;

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

public class GameManager {
    public static final int TILE_SIZE = 32;

    private int[][] map;
    private int cols = 20;
    private int rows = 19;

    public static boolean isGateOpen = false;
    private int lastPacmanGridX = -1;
    private int lastPacmanGridY = -1;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer gameLoop;

    private Pacman pacman;
    private List<Ghost> ghosts; // Quản lý danh sách các con ma
    private UIManager uiManager;

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

    private GameState gameState = GameState.MENU;

    public GameManager() {
        canvas = new Canvas(cols * TILE_SIZE, rows * TILE_SIZE);
        gc = canvas.getGraphicsContext2D();

        ghosts = new ArrayList<>();

        initLevel();
        setupGameLoop();
    }

    private void initLevel() {
        int[][] newMap = MapManager.loadMap(level);
        map = new int[rows][cols];
        totalDots = 0;
        isGateOpen = false; // Đóng cửa sập khi bắt đầu level mới

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
        pacman = new Pacman(9 * TILE_SIZE, 12 * TILE_SIZE);

        ghosts.clear();
        // Khởi tạo Blinky (Đỏ) - Xuất phát ngoài chuồng (Hàng 8, cột 9)
        ghosts.add(new Ghost(9 * TILE_SIZE, 8 * TILE_SIZE, GhostType.BLINKY));
        // Khởi tạo Pinky (Hồng) - Xuất phát trong chuồng
        ghosts.add(new Ghost(9 * TILE_SIZE, 9 * TILE_SIZE, GhostType.PINKY));
        // Khởi tạo Inky (Xanh) - Xuất phát trong chuồng
        ghosts.add(new Ghost(10 * TILE_SIZE, 9 * TILE_SIZE, GhostType.INKY));
        // Khởi tạo Clyde (Cam) - Xuất phát trong chuồng (x=9, y=10 để không dính tường)
        ghosts.add(new Ghost(9 * TILE_SIZE, 10 * TILE_SIZE, GhostType.CLYDE));

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
                uiManager.showPauseMenu();
            } else if (gameState == GameState.PAUSED) {
                uiManager.showGame();
            }
        }
    }

    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
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
        return GameSessionStore.getInstance().hasSave();
    }

    public boolean canContinueGame() {
        return isGameActive() || hasSavedGame();
    }

    public boolean saveCurrentGame() {
        if (isGameOver) {
            return false;
        }

        return GameSessionStore.getInstance().save(captureSnapshot());
    }

    public boolean continueSavedGame() {
        GameSessionStore.Snapshot snapshot = GameSessionStore.getInstance().load();
        if (snapshot == null) {
            return false;
        }

        applySnapshot(snapshot);
        return true;
    }

    public void clearSavedGame() {
        GameSessionStore.getInstance().clear();
    }

    public void resetGame() {
        score = 0;
        lives = 3;
        level = 1;
        comboMultiplier = 1;
        extraLifeGiven = false;
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
            p.setX((int) (canvas.getWidth() - TILE_SIZE));
        if (p.getX() >= canvas.getWidth())
            p.setX(0);
    }

    private void teleportEntity(Ghost g) {
        if (g.getX() < 0)
            g.setX((int) (canvas.getWidth() - TILE_SIZE));
        if (g.getX() >= canvas.getWidth())
            g.setX(0);
    }

    private void checkEat() {
        if (pacman.getX() % TILE_SIZE == 0 && pacman.getY() % TILE_SIZE == 0) {
            int gridX = pacman.getGridX();
            int gridY = pacman.getGridY();

            // Xử lý giẫm lên Công tắc (Switch = 8)
            if ((gridX != lastPacmanGridX || gridY != lastPacmanGridY) && map[gridY][gridX] == 8) {
                isGateOpen = !isGateOpen;
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
        if (score >= 10000 && !extraLifeGiven) {
            lives++;
            extraLifeGiven = true;
        }
    }

    private void checkCollisions() {
        for (Ghost g : ghosts) {
            double dx = pacman.getX() - g.getX();
            double dy = pacman.getY() - g.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < TILE_SIZE - 4) {
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
                        if (uiManager != null) {
                            uiManager.showGameOver(score);
                        }
                    } else {
                        resetPositions();
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
                int tileX = col * TILE_SIZE;
                int tileY = row * TILE_SIZE;

                if (map[row][col] == 1) {
                    if (level == 1) {
                        // Tường neon retro xanh lơ
                        gc.setFill(Color.rgb(0, 0, 80));
                        gc.fillRect(tileX, tileY, TILE_SIZE, TILE_SIZE);
                        gc.setStroke(Color.CYAN);
                        gc.setLineWidth(2);
                        gc.strokeRoundRect(tileX + 2, tileY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    } else {
                        // Phân Xưởng Cơ Học: Tường màu Đồng/Cam
                        gc.setFill(Color.rgb(80, 40, 0));
                        gc.fillRect(tileX, tileY, TILE_SIZE, TILE_SIZE);
                        gc.setStroke(Color.ORANGE);
                        gc.setLineWidth(2);
                        gc.strokeRoundRect(tileX + 2, tileY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    }
                } else if (map[row][col] == 2) {
                    gc.setFill(Color.WHITE);
                    double dotSize = 6;
                    double offset = (TILE_SIZE - dotSize) / 2.0;
                    gc.fillOval(tileX + offset, tileY + offset, dotSize, dotSize);
                } else if (map[row][col] == 3) {
                    gc.setFill(Color.WHITE);
                    // Hiệu ứng hạt to nhấp nháy/phóng to thu nhỏ
                    long time = System.currentTimeMillis();
                    double pulse = Math.sin(time / 150.0);
                    double dotSize = 12 + pulse * 3; // Kích thước dao động từ 9 đến 15
                    double offset = (TILE_SIZE - dotSize) / 2.0;
                    gc.fillOval(tileX + offset, tileY + offset, dotSize, dotSize);
                } else if (map[row][col] >= 4 && map[row][col] <= 7) {
                    // Vẽ băng chuyền
                    gc.setFill(Color.rgb(100, 100, 100)); // Màu xám cho băng chuyền
                    gc.fillRect(tileX + 2, tileY + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    gc.setFill(Color.YELLOW);
                    double cx = tileX + TILE_SIZE / 2.0;
                    double cy = tileY + TILE_SIZE / 2.0;
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
                    gc.fillRect(tileX + 4, tileY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                    gc.setFill(isGateOpen ? Color.GREEN : Color.RED);
                    gc.fillOval(tileX + 8, tileY + 8, TILE_SIZE - 16, TILE_SIZE - 16);
                } else if (map[row][col] == 9) {
                    // Cửa sập (Gate)
                    if (!isGateOpen) {
                        gc.setFill(Color.rgb(200, 0, 0)); // Đóng (màu đỏ)
                        gc.fillRect(tileX, tileY + TILE_SIZE / 4.0, TILE_SIZE, TILE_SIZE / 2.0);
                        gc.setStroke(Color.WHITE);
                        gc.strokeLine(tileX, tileY + TILE_SIZE / 2.0, tileX + TILE_SIZE, tileY + TILE_SIZE / 2.0);
                    } else {
                        gc.setFill(Color.rgb(0, 100, 0, 0.5)); // Mở (màu xanh lá bán trong suốt)
                        gc.fillRect(tileX, tileY, TILE_SIZE, 4);
                        gc.fillRect(tileX, tileY + TILE_SIZE - 4, TILE_SIZE, 4);
                    }
                }
            }
        }

        if (!isGameOver) {
            pacman.render(gc);
            for (Ghost g : ghosts) {
                g.render(gc);
            }
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("SCORE: " + score, 10, 20);
        gc.fillText("LIVES: " + lives, canvas.getWidth() - 90, 20);
        gc.fillText("LEVEL: " + level, canvas.getWidth() / 2 - 30, 20);
    }

    private GameSessionStore.Snapshot captureSnapshot() {
        List<GameSessionStore.GhostSnapshot> ghostSnapshots = new ArrayList<>();
        for (Ghost ghost : ghosts) {
            ghostSnapshots.add(new GameSessionStore.GhostSnapshot(ghost.getType(), ghost.getX(), ghost.getY(),
                    ghost.getSpeed(), ghost.getState(), ghost.getCurrentDirection()));
        }

        int[][] mapCopy = new int[map.length][map[0].length];
        for (int row = 0; row < map.length; row++) {
            System.arraycopy(map[row], 0, mapCopy[row], 0, map[row].length);
        }

        return new GameSessionStore.Snapshot(score, lives, comboMultiplier, extraLifeGiven, level, totalDots,
                isGateOpen, lastPacmanGridX, lastPacmanGridY, pacman.getX(), pacman.getY(),
                pacman.getCurrentDirection(), pacman.getNextDirection(), mapCopy, ghostSnapshots);
    }

    private void applySnapshot(GameSessionStore.Snapshot snapshot) {
        System.out.println("[DEBUG] Applying snapshot: lives=" + snapshot.lives() + ", score=" + snapshot.score()
                + ", level=" + snapshot.level());
        score = snapshot.score();
        lives = snapshot.lives();
        comboMultiplier = snapshot.comboMultiplier();
        extraLifeGiven = snapshot.extraLifeGiven();
        level = snapshot.level();
        totalDots = snapshot.totalDots();
        isGateOpen = snapshot.isGateOpen();
        lastPacmanGridX = snapshot.lastPacmanGridX();
        lastPacmanGridY = snapshot.lastPacmanGridY();
        map = snapshot.map();

        pacman = new Pacman(snapshot.pacmanX(), snapshot.pacmanY());
        pacman.setSpeed(2);
        pacman.setCurrentDirection(snapshot.pacmanDirection());
        pacman.setNextDirection(snapshot.pacmanNextDirection());

        ghosts = new ArrayList<>();
        for (GameSessionStore.GhostSnapshot ghostSnapshot : snapshot.ghostSnapshots()) {
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
    }
}
