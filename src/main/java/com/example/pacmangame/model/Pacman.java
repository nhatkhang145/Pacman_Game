package com.example.pacmangame.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Pacman {
    private int x;
    private int y;
    private int speed = 2;

    private Direction currentDirection = Direction.NONE;
    private Direction nextDirection = Direction.NONE;

    private int animationFrame = 0;
    private boolean isDying = false;
    private long deathStartTime = 0;

    public boolean isDying() {
        return isDying;
    }

    public void setDying(boolean dying) {
        this.isDying = dying;
        if (dying) {
            this.deathStartTime = System.currentTimeMillis();
        }
    }

    public Pacman(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(Direction dir) {
        this.currentDirection = dir;
    }

    public Direction getNextDirection() {
        return nextDirection;
    }

    public void setNextDirection(Direction dir) {
        this.nextDirection = dir;
    }

    public void update(int[][] map) {
        if (currentDirection != Direction.NONE) {
            animationFrame++;
        }
        // Chỉ cho phép rẽ/đổi hướng khi Pac-Man nằm khớp chính xác với ô lưới
        // (Grid-aligned)
        // Điều này đảm bảo Pac-Man không đi lệch khỏi đường ray.
        if (x % GameConfig.TILE_SIZE == 0 && y % GameConfig.TILE_SIZE == 0) {

            // 1. Kiểm tra xem hướng đệm (nextDirection) có hợp lệ không (có bị tường chặn
            // không)
            if (nextDirection != Direction.NONE && canMove(nextDirection, map)) {
                currentDirection = nextDirection;
                // Có thể reset nextDirection = NONE nếu muốn phải bấm lại,
                // nhưng giữ nguyên để Pac-Man rẽ liên tục nếu đè phím cũng được.
            }

            // 2. Kiểm tra xem hướng hiện tại có bị chặn không. Nếu bị chặn, dừng lại.
            if (!canMove(currentDirection, map)) {
                return;
            }
        } else {
            // Cho phép đảo ngược hướng ngay lập tức (180 độ) ngay cả khi chưa khớp lưới
            if (nextDirection != Direction.NONE && isOpposite(currentDirection, nextDirection)) {
                currentDirection = nextDirection;
            }
        }

        int currentSpeed = speed;

        // Kiểm tra Băng chuyền (Conveyor) để thay đổi tốc độ
        int gridX = x / GameConfig.TILE_SIZE;
        int gridY = y / GameConfig.TILE_SIZE;
        if (gridY >= 0 && gridY < map.length && gridX >= 0 && gridX < map[0].length) {
            int tile = map[gridY][gridX];
            if (tile >= 4 && tile <= 7) {
                if ((tile == 4 && currentDirection == Direction.UP) ||
                        (tile == 5 && currentDirection == Direction.DOWN) ||
                        (tile == 6 && currentDirection == Direction.LEFT) ||
                        (tile == 7 && currentDirection == Direction.RIGHT)) {
                    currentSpeed = speed * 2; // +100% tốc độ khi xuôi chiều
                } else if ((tile == 4 && currentDirection == Direction.DOWN) ||
                        (tile == 5 && currentDirection == Direction.UP) ||
                        (tile == 6 && currentDirection == Direction.RIGHT) ||
                        (tile == 7 && currentDirection == Direction.LEFT)) {
                    currentSpeed = speed / 2; // -50% tốc độ khi ngược chiều
                    if (currentSpeed < 1)
                        currentSpeed = 1;
                }
            }
        }

        // Cập nhật tọa độ theo hướng đi hiện tại
        switch (currentDirection) {
            case UP:
                y -= currentSpeed;
                break;
            case DOWN:
                y += currentSpeed;
                break;
            case LEFT:
                x -= currentSpeed;
                break;
            case RIGHT:
                x += currentSpeed;
                break;
            case NONE:
                break;
        }
    }

    // Kiểm tra xem 2 hướng có phải là ngược nhau không (đi quay lưng 180 độ)
    private boolean isOpposite(Direction dir1, Direction dir2) {
        return (dir1 == Direction.UP && dir2 == Direction.DOWN) ||
                (dir1 == Direction.DOWN && dir2 == Direction.UP) ||
                (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) ||
                (dir1 == Direction.RIGHT && dir2 == Direction.LEFT);
    }

    /*
     * Logic Toán học xử lý va chạm tường:
     * Dựa trên vị trí hiện tại của Pac-Man (đã khớp vào một ô lưới),
     * tính toán tọa độ (cột, hàng) của ô tiếp theo theo hướng dir trên mảng 2D.
     * Nếu ô tiếp theo là tường (giá trị 1) thì trả về false (bị chặn).
     */
    private boolean canMove(Direction dir, int[][] map) {
        if (dir == Direction.NONE)
            return false;

        // Tính ra chỉ số cột (col) và hàng (row) hiện tại của Pac-Man trên mảng 2D
int currentGridX = x / GameConfig.TILE_SIZE;
            int currentGridY = y / GameConfig.TILE_SIZE;

        int nextGridX = currentGridX;
        int nextGridY = currentGridY;

        // Dự tính tọa độ ô tiếp theo
        switch (dir) {
            case UP:
                nextGridY -= 1;
                break;
            case DOWN:
                nextGridY += 1;
                break;
            case LEFT:
                nextGridX -= 1;
                break;
            case RIGHT:
                nextGridX += 1;
                break;
            case NONE:
                break;
        }

        // Kiểm tra xem ô tiếp theo có bị out of bounds (vượt khỏi màn hình) không
        if (nextGridY < 0 || nextGridY >= map.length || nextGridX < 0 || nextGridX >= map[0].length) {
            return false;
        }

        // Kiểm tra xem ô tiếp theo có phải là Tường (1) hoặc Cửa sập đóng (9) không
        if (map[nextGridY][nextGridX] == 1)
            return false;
        if (map[nextGridY][nextGridX] == 9 && !GameConfig.gateOpen)
            return false;

        return true;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);

        double size = GameConfig.TILE_SIZE - 4;
        double offset = 2;

        if (isDying) {
            long now = System.currentTimeMillis();
            long elapsed = now - deathStartTime;
            double angle = 0;
            if (elapsed < 2000) {
                angle = (elapsed / 2000.0) * 180; // Há miệng to ra đến 180 độ (nửa hình tròn mỗi bên)
            } else {
                angle = 180;
            }
            
            double startAngle = 0;
            switch (currentDirection) {
                case RIGHT: startAngle = angle; break;
                case UP: startAngle = 90 + angle; break;
                case LEFT: startAngle = 180 + angle; break;
                case DOWN: startAngle = 270 + angle; break;
                case NONE: startAngle = angle; break;
            }
            double extent = 360 - 2 * angle;
            if (extent > 0) {
                gc.fillArc(x + offset, y + offset, size, size, startAngle, extent, javafx.scene.shape.ArcType.ROUND);
            }
            return;
        }

        // Tính toán góc há miệng
        double angle = 45 * Math.abs(Math.sin(animationFrame * 0.3));
        double startAngle = 0;

        switch (currentDirection) {
            case RIGHT:
                startAngle = angle;
                break;
            case UP:
                startAngle = 90 + angle;
                break;
            case LEFT:
                startAngle = 180 + angle;
                break;
            case DOWN:
                startAngle = 270 + angle;
                break;
            case NONE:
                startAngle = angle;
                break;
        }

        double extent = 360 - 2 * angle;
        gc.fillArc(x + offset, y + offset, size, size, startAngle, extent, javafx.scene.shape.ArcType.ROUND);
    }

    public int getGridX() {
        return x / GameConfig.TILE_SIZE;
    }

    public int getGridY() {
        return y / GameConfig.TILE_SIZE;
    }
}
