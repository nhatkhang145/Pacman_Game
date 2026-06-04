

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

        
    }

   
  //  Phương thức này xử lý việc cộng điểm cho Pac-Man.
    private void addScore(int points) {
        score += points;
        if (score >= 10000 && !extraLifeGiven) {
            lives++;
            extraLifeGiven = true;
        }
    }

    
//Phương thức này chịu trách nhiệm vẽ/hiển thị toàn bộ giao diện trò chơi (UI/HUD) lên màn hình bằng bộ công cụ đồ họa GraphicsContext.
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

   
    
}
