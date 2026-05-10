package com.example.pacmangame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ghost {
    private int x;
    private int y;
    private int speed = 2;

    private Direction currentDirection = Direction.RIGHT;
    private GhostState state = GhostState.CHASE;
    private GhostType type;

    private Random random = new Random();

    public Ghost(int startX, int startY, GhostType type) {
        this.x = startX;
        this.y = startY;
        this.type = type;
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

    public GhostState getState() {
        return state;
    }

    public GhostType getType() {
        return type;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(Direction direction) {
        this.currentDirection = direction;
    }

    public void setState(GhostState newState) {
        if ((this.state == GhostState.CHASE && newState == GhostState.SCATTER) ||
                (this.state == GhostState.SCATTER && newState == GhostState.CHASE)) {
            currentDirection = getOpposite(currentDirection);
        }
        this.state = newState;
    }

    // Inky cần vị trí của Blinky để tính toán mục tiêu
    public void update(int[][] map, Pacman pacman, Ghost blinky) {
        if (x % GameManager.TILE_SIZE == 0 && y % GameManager.TILE_SIZE == 0) {

            int currentGridX = x / GameManager.TILE_SIZE;
            int currentGridY = y / GameManager.TILE_SIZE;

            int targetX = 0;
            int targetY = 0;

            if (state == GhostState.CHASE) {
                if (type == GhostType.BLINKY) {
                    targetX = pacman.getGridX();
                    targetY = pacman.getGridY();
                } else if (type == GhostType.PINKY) {
                    // Nhắm 4 ô phía trước Pacman
                    targetX = pacman.getGridX();
                    targetY = pacman.getGridY();
                    switch (pacman.getCurrentDirection()) {
                        case UP:
                            targetY -= 4;
                            targetX -= 4;
                            break; // Lỗi gốc của game: đi lên sẽ qua trái 4 ô nữa
                        case DOWN:
                            targetY += 4;
                            break;
                        case LEFT:
                            targetX -= 4;
                            break;
                        case RIGHT:
                            targetX += 4;
                            break;
                        default:
                            break;
                    }
                } else if (type == GhostType.INKY && blinky != null) {
                    // Nhắm 2 ô phía trước Pacman
                    int twoAheadX = pacman.getGridX();
                    int twoAheadY = pacman.getGridY();
                    switch (pacman.getCurrentDirection()) {
                        case UP:
                            twoAheadY -= 2;
                            twoAheadX -= 2;
                            break;
                        case DOWN:
                            twoAheadY += 2;
                            break;
                        case LEFT:
                            twoAheadX -= 2;
                            break;
                        case RIGHT:
                            twoAheadX += 2;
                            break;
                        default:
                            break;
                    }
                    // Tính vector từ Blinky đến 2 ô đó, rồi nhân đôi
                    int vectorX = twoAheadX - (blinky.getX() / GameManager.TILE_SIZE);
                    int vectorY = twoAheadY - (blinky.getY() / GameManager.TILE_SIZE);
                    targetX = twoAheadX + vectorX;
                    targetY = twoAheadY + vectorY;
                } else if (type == GhostType.CLYDE) {
                    double dist = Math.sqrt(Math.pow(pacman.getGridX() - currentGridX, 2)
                            + Math.pow(pacman.getGridY() - currentGridY, 2));
                    if (dist >= 8) {
                        targetX = pacman.getGridX();
                        targetY = pacman.getGridY();
                    } else {
                        targetX = 1;
                        targetY = map.length - 2;
                    }
                }
            } else if (state == GhostState.SCATTER) {
                if (type == GhostType.BLINKY) {
                    targetX = map[0].length - 2;
                    targetY = 1;
                } else if (type == GhostType.PINKY) {
                    targetX = 1;
                    targetY = 1;
                } else if (type == GhostType.INKY) {
                    targetX = map[0].length - 2;
                    targetY = map.length - 2;
                } else if (type == GhostType.CLYDE) {
                    targetX = 1;
                    targetY = map.length - 2;
                }
            } else if (state == GhostState.EYES) {
                targetX = 9;
                targetY = 8;
                if (currentGridX == targetX && currentGridY == targetY) {
                    setState(GhostState.CHASE);
                }
            }

            Direction[] possibleDirs = { Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT };
            List<Direction> validDirs = new ArrayList<>();

            for (Direction dir : possibleDirs) {
                if (isOpposite(currentDirection, dir) && currentDirection != Direction.NONE) {
                    continue;
                }

                int nextX = currentGridX;
                int nextY = currentGridY;
                switch (dir) {
                    case UP:
                        nextY--;
                        break;
                    case DOWN:
                        nextY++;
                        break;
                    case LEFT:
                        nextX--;
                        break;
                    case RIGHT:
                        nextX++;
                        break;
                    default:
                        break;
                }

                if (nextY >= 0 && nextY < map.length && nextX >= 0 && nextX < map[0].length) {
                    if (map[nextY][nextX] == 1)
                        continue;
                    if (map[nextY][nextX] == 9 && !GameManager.isGateOpen)
                        continue;
                    validDirs.add(dir);
                }
            }

            if (validDirs.isEmpty()) {
                currentDirection = getOpposite(currentDirection);
            } else {
                if (state == GhostState.FRIGHTENED) {
                    currentDirection = validDirs.get(random.nextInt(validDirs.size()));
                } else {
                    Direction bestDir = validDirs.get(0);
                    double minDistance = Double.MAX_VALUE;

                    for (Direction dir : validDirs) {
                        int nextX = currentGridX;
                        int nextY = currentGridY;
                        switch (dir) {
                            case UP:
                                nextY--;
                                break;
                            case DOWN:
                                nextY++;
                                break;
                            case LEFT:
                                nextX--;
                                break;
                            case RIGHT:
                                nextX++;
                                break;
                            default:
                                break;
                        }

                        double distanceSq = Math.pow(nextX - targetX, 2) + Math.pow(nextY - targetY, 2);

                        // Trọng số cho băng chuyền (Dynamic Pathfinding)
                        int tile = map[nextY][nextX];
                        if (tile >= 4 && tile <= 7) {
                            if ((tile == 4 && dir == Direction.UP) ||
                                    (tile == 5 && dir == Direction.DOWN) ||
                                    (tile == 6 && dir == Direction.LEFT) ||
                                    (tile == 7 && dir == Direction.RIGHT)) {
                                distanceSq -= 5; // Ưu tiên đi xuôi chiều băng chuyền
                            }
                        }

                        if (distanceSq < minDistance) {
                            minDistance = distanceSq;
                            bestDir = dir;
                        }
                    }
                    currentDirection = bestDir;
                }
            }
        } else {
            // Phản xạ thời gian thực: Cửa sập đóng đột ngột
            int nextX = x / GameManager.TILE_SIZE;
            int nextY = y / GameManager.TILE_SIZE;
            switch (currentDirection) {
                case UP:
                    nextY--;
                    break;
                case DOWN:
                    nextY++;
                    break;
                case LEFT:
                    nextX--;
                    break;
                case RIGHT:
                    nextX++;
                    break;
                default:
                    break;
            }
            if (nextY >= 0 && nextY < map.length && nextX >= 0 && nextX < map[0].length) {
                if (map[nextY][nextX] == 9 && !GameManager.isGateOpen) {
                    currentDirection = getOpposite(currentDirection);
                }
            }
        }

        int currentSpeed = speed;
        int gridX = x / GameManager.TILE_SIZE;
        int gridY = y / GameManager.TILE_SIZE;
        if (gridY >= 0 && gridY < map.length && gridX >= 0 && gridX < map[0].length) {
            int tile = map[gridY][gridX];
            if (tile >= 4 && tile <= 7) {
                if ((tile == 4 && currentDirection == Direction.UP) ||
                        (tile == 5 && currentDirection == Direction.DOWN) ||
                        (tile == 6 && currentDirection == Direction.LEFT) ||
                        (tile == 7 && currentDirection == Direction.RIGHT)) {
                    currentSpeed = speed * 2;
                } else if ((tile == 4 && currentDirection == Direction.DOWN) ||
                        (tile == 5 && currentDirection == Direction.UP) ||
                        (tile == 6 && currentDirection == Direction.RIGHT) ||
                        (tile == 7 && currentDirection == Direction.LEFT)) {
                    currentSpeed = speed / 2;
                    if (currentSpeed < 1)
                        currentSpeed = 1;
                }
            }
        }

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
            default:
                break;
        }
    }

    private boolean isOpposite(Direction dir1, Direction dir2) {
        return (dir1 == Direction.UP && dir2 == Direction.DOWN) ||
                (dir1 == Direction.DOWN && dir2 == Direction.UP) ||
                (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) ||
                (dir1 == Direction.RIGHT && dir2 == Direction.LEFT);
    }

    private Direction getOpposite(Direction dir) {
        switch (dir) {
            case UP:
                return Direction.DOWN;
            case DOWN:
                return Direction.UP;
            case LEFT:
                return Direction.RIGHT;
            case RIGHT:
                return Direction.LEFT;
            default:
                return Direction.NONE;
        }
    }

    public void render(GraphicsContext gc) {
        double size = GameManager.TILE_SIZE - 4;
        double offset = 2;

        if (state != GhostState.EYES) {
            if (state == GhostState.FRIGHTENED) {
                gc.setFill(Color.BLUE);
            } else {
                if (type == GhostType.BLINKY) {
                    gc.setFill(Color.RED);
                } else if (type == GhostType.PINKY) {
                    gc.setFill(Color.PINK);
                } else if (type == GhostType.INKY) {
                    gc.setFill(Color.CYAN);
                } else if (type == GhostType.CLYDE) {
                    gc.setFill(Color.ORANGE);
                }
            }

            // Vẽ đầu tròn
            gc.fillArc(x + offset, y + offset, size, size, 0, 180, javafx.scene.shape.ArcType.ROUND);
            // Vẽ thân vuông
            gc.fillRect(x + offset, y + offset + size / 2, size, size / 2);

            // Vẽ rãnh cắt làm chân vẫy (2 tam giác màu đen đè lên)
            gc.setFill(Color.BLACK);
            gc.fillPolygon(new double[] { x + offset, x + offset + size / 4, x + offset + size / 2 },
                    new double[] { y + offset + size, y + offset + size - 4, y + offset + size }, 3);
            gc.fillPolygon(new double[] { x + offset + size / 2, x + offset + 3 * size / 4, x + offset + size },
                    new double[] { y + offset + size, y + offset + size - 4, y + offset + size }, 3);
        }

        // Vẽ Mắt
        if (state == GhostState.FRIGHTENED) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(x + offset + 6, y + offset + 8, 4, 4);
            gc.fillRect(x + offset + 18, y + offset + 8, 4, 4);

            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1.5);
            gc.strokePolyline(
                    new double[] { x + offset + 4, x + offset + 8, x + offset + 14, x + offset + 20, x + offset + 24 },
                    new double[] { y + offset + 20, y + offset + 16, y + offset + 20, y + offset + 16,
                            y + offset + 20 },
                    5);
        } else {
            double eyeWidth = 8;
            double eyeHeight = 10;
            double eyeOffsetX = 4;
            double eyeOffsetY = 6;

            gc.setFill(Color.WHITE);
            gc.fillOval(x + offset + eyeOffsetX, y + offset + eyeOffsetY, eyeWidth, eyeHeight);
            gc.fillOval(x + offset + size - eyeOffsetX - eyeWidth, y + offset + eyeOffsetY, eyeWidth, eyeHeight);

            gc.setFill(Color.BLUE);
            double pupilSize = 4;
            double pX1 = x + offset + eyeOffsetX + 2;
            double pY1 = y + offset + eyeOffsetY + 4;
            double pX2 = x + offset + size - eyeOffsetX - eyeWidth + 2;
            double pY2 = y + offset + eyeOffsetY + 4;

            switch (currentDirection) {
                case UP:
                    pY1 -= 3;
                    pY2 -= 3;
                    break;
                case DOWN:
                    pY1 += 3;
                    pY2 += 3;
                    break;
                case LEFT:
                    pX1 -= 3;
                    pX2 -= 3;
                    break;
                case RIGHT:
                    pX1 += 3;
                    pX2 += 3;
                    break;
                default:
                    break;
            }

            gc.fillOval(pX1, pY1, pupilSize, pupilSize);
            gc.fillOval(pX2, pY2, pupilSize, pupilSize);
        }
    }
}
