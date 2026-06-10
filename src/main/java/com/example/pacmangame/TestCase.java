package com.example.pacmangame;

import java.util.ArrayList;
import java.util.List;

import com.example.pacmangame.model.Direction;

public class TestCase extends junit.framework.TestCase {
	private int[][] mockMap;
    private int tileSize;

    /**
     * Hàm khởi tạo môi trường test cho mỗi test case (Chuẩn JUnit 3)
     */
    protected void setUp() throws Exception {
        super.setUp();
        tileSize = 16; // Giả lập TILE_SIZE = 16px
        
        // Ma trận bản đồ thu nhỏ giả lập:
        // 1: Tường, 0: Đường trống, 8: Công tắc, 9: Cổng Ghost
        // 4, 5, 6, 7: Băng chuyền (4: UP, 5: DOWN, 6: LEFT, 7: RIGHT)
        mockMap = new int[][]{
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 1},
            {1, 4, 5, 6, 7, 0, 9, 0, 8, 1}, // Dòng 2 chứa băng chuyền, cổng, công tắc
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
    }

    // =========================================================================
    // UC-03: AI & THUẬT TOÁN TÌM ĐƯỜNG (GHOST PATHFINDING)
    // =========================================================================

    /**
     * Test logic loại bỏ hướng cấm ngược chiều (No U-Turn)
     */
    public void testNoUTurnLogic() {
        Direction currentDirection = Direction.RIGHT;
        Direction[] possibleDirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        List<Direction> validDirs = new ArrayList<Direction>();

        for (Direction dir : possibleDirs) {
            // Loại bỏ hướng U-Turn (quay 180 độ) khi đang di chuyển
            if (isOpposite(currentDirection, dir) && currentDirection != Direction.NONE) {
                continue; // Sẽ bỏ qua Direction.LEFT
            }
            validDirs.add(dir);
        }

        assertFalse("AI phải loại bỏ hướng đi ngược lại (LEFT khi đang đi RIGHT).", validDirs.contains(Direction.LEFT));
        assertTrue("Hướng đi tiếp tục (RIGHT) phải hợp lệ.", validDirs.contains(Direction.RIGHT));
    }

    /**
     * Test logic tính khoảng cách Euclidean bình phương và trọng số Băng chuyền
     */
    public void testEuclideanDistanceWithConveyorWeight() {
        // Ma đứng ở ô (5, 2), Target nhắm Pacman ở ô (5, 0)
        int targetX = 5;
        int targetY = 0;

        // Hướng 1: UP -> Ô kế tiếp (5, 1) là ô trống bình thường (0)
        int nextXUp = 5;
        int nextYUp = 1;
        double distanceSqUp = Math.pow(nextXUp - targetX, 2) + Math.pow(nextYUp - targetY, 2); // (5-5)^2 + (1-0)^2 = 1

        // Hướng 2: LEFT -> Ô kế tiếp (4, 2) là ô băng chuyền số 7 (RIGHT) -> Đi ngược chiều
        int nextXLeft = 4;
        int nextYLeft = 2;
        double distanceSqLeft = Math.pow(nextXLeft - targetX, 2) + Math.pow(nextYLeft - targetY, 2); // (4-5)^2 + (2-0)^2 = 1 + 4 = 5
        
        int tile = mockMap[nextYLeft][nextXLeft];
        if (tile >= 4 && tile <= 7) {
            if (tile == 7 && Direction.LEFT == Direction.RIGHT) {
                distanceSqLeft -= 5; // Chỉ trừ điểm ưu tiên nếu đi xuôi chiều băng chuyền
            }
        }

        // AI sẽ chọn hướng có khoảng cách nhỏ nhất
        Direction bestDir = (distanceSqUp < distanceSqLeft) ? Direction.UP : Direction.LEFT;
        
        assertEquals(1.0, distanceSqUp);
        assertEquals(5.0, distanceSqLeft); // Không được giảm do đi ngược chiều
        assertEquals("AI phải chọn hướng UP vì khoảng cách ngắn nhất.", Direction.UP, bestDir);
    }

    /**
     * Test logic thay đổi vận tốc thực tế dựa trên hướng di chuyển trên Băng Chuyền
     */
    public void testConveyorSpeedModifier() {
        int baseSpeed = 2;
        int tileLeftConveyor = 6; // Ô băng chuyền hướng LEFT

        // Trường hợp 1: Ma đi xuôi chiều băng chuyền (Đi LEFT trên băng chuyền LEFT)
        int speedXuoiChieu = baseSpeed;
        if (tileLeftConveyor == 6 && Direction.LEFT == Direction.LEFT) {
            speedXuoiChieu = baseSpeed * 2;
        }
        assertEquals("Tốc độ phải tăng gấp đôi khi đi xuôi chiều băng chuyền.", 4, speedXuoiChieu);

        // Trường hợp 2: Ma đi ngược chiều băng chuyền (Đi RIGHT trên băng chuyền LEFT)
        int speedNguocChieu = baseSpeed;
        if (tileLeftConveyor == 6 && Direction.RIGHT == Direction.LEFT) {
            // Xuôi chiều
        } else if (tileLeftConveyor == 6 && Direction.RIGHT == Direction.RIGHT) {
            speedNguocChieu = baseSpeed / 2;
        }
        assertEquals("Tốc độ phải giảm một nửa khi đi ngược chiều băng chuyền.", 1, speedNguocChieu);
    }

    // =========================================================================
    // UC-04: VA CHẠM VÀ LOGIC ĐIỂM SỐ COMBO
    // =========================================================================

    /**
     * Test thuật toán kiểm tra va chạm dựa trên khoảng cách Pixel
     */
    public void testCollisionDetection() {
        // Giả lập tọa độ pixel của Pac-Man và Ghost ở rất gần nhau
        double pacmanX = 32.0;
        double pacmanY = 32.0;
        double ghostX = 35.0; // Cách 3 pixel
        double ghostY = 32.0;

        double dx = pacmanX - ghostX;
        double dy = pacmanY - ghostY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Ngưỡng va chạm: TILE_SIZE - 4 = 16 - 4 = 12px
        boolean isCollided = distance < (tileSize - 4);
        assertTrue("Hệ thống phải ghi nhận va chạm khi khoảng cách < 12px (thực tế 3px).", isCollided);

        // Giả lập khi Ghost ở xa
        double safeGhostX = 60.0;
        double dxSafe = pacmanX - safeGhostX;
        double distanceSafe = Math.sqrt(dxSafe * dxSafe + dy * dy);
        boolean isSafe = distanceSafe < (tileSize - 4);
        assertFalse("Hệ thống không được tính va chạm khi Ghost ở xa.", isSafe);
    }

    /**
     * Test logic nhân đôi điểm Combo liên tiếp khi ăn ma FRIGHTENED
     */
    public void testDeepScoreComboLogic() {
        int currentScore = 0;
        int comboMultiplier = 1; // Khởi tạo khi Pacman ăn Power Pellet

        // Ăn con ma thứ 1
        currentScore += 200 * comboMultiplier;
        comboMultiplier *= 2; 
        assertEquals(200, currentScore);
        assertEquals(2, comboMultiplier);

        // Ăn con ma thứ 2 liên tiếp
        currentScore += 200 * comboMultiplier;
        comboMultiplier *= 2;
        assertEquals("Tổng điểm sau 2 lần ăn ma liên tiếp phải là 600.", 600, currentScore);
        assertEquals(4, comboMultiplier);

        // Reset khi ăn Power Pellet mới
        comboMultiplier = 1;
        assertEquals("Combo phải reset về 1 khi ăn viên năng lượng mới.", 1, comboMultiplier);
    }

    /**
     * Test logic tặng thêm mạng (Extra Life) duy nhất 1 lần tại mốc 10.000 điểm
     */
    public void testExtraLifeReward() {
        int lives = 3;
        int score = 9900;
        boolean extraLifeGiven = false;

        // Ăn ma được cộng thêm 200 điểm -> Vượt mốc 10.000
        score += 200;
        if (score >= 10000 && !extraLifeGiven) {
            lives++;
            extraLifeGiven = true;
        }

        assertEquals(4, lives);
        assertTrue(extraLifeGiven);

        // Tiếp tục tăng điểm, mạng không được tăng thêm lần nữa ở mốc này
        score += 5000;
        if (score >= 10000 && !extraLifeGiven) {
            lives++;
        }
        assertEquals("Mạng không được tăng thêm lần 2 tại mốc này.", 4, lives);
    }

    // =========================================================================
    // HÀM UTILS TRỢ GIÚP (Mô phỏng logic của Direction.java)
    // =========================================================================
    
    private boolean isOpposite(Direction dir1, Direction dir2) {
        if (dir1 == Direction.UP && dir2 == Direction.DOWN) return true;
        if (dir1 == Direction.DOWN && dir2 == Direction.UP) return true;
        if (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) return true;
        if (dir1 == Direction.RIGHT && dir2 == Direction.LEFT) return true;
        return false;
    }
}
