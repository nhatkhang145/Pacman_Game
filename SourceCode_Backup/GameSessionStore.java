package com.example.pacmangame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class GameSessionStore {
    private static GameSessionStore instance;

    private final Path storageFile;

    private GameSessionStore() {
        storageFile = Paths.get(System.getProperty("user.home"), ".pacman-game", "save.properties");
    }

    public static synchronized GameSessionStore getInstance() {
        if (instance == null) {
            instance = new GameSessionStore();
        }
        return instance;
    }

    public boolean hasSave() {
        return Files.exists(storageFile);
    }

    public void clear() {
        try {
            Files.deleteIfExists(storageFile);
        } catch (IOException ex) {
            // Ignore cleanup errors.
        }
    }

    public boolean save(Snapshot snapshot) {
        Properties properties = new Properties();
        properties.setProperty("score", Integer.toString(snapshot.score()));
        properties.setProperty("lives", Integer.toString(snapshot.lives()));
        properties.setProperty("comboMultiplier", Integer.toString(snapshot.comboMultiplier()));
        properties.setProperty("extraLifeGiven", Boolean.toString(snapshot.extraLifeGiven()));
        properties.setProperty("level", Integer.toString(snapshot.level()));
        properties.setProperty("totalDots", Integer.toString(snapshot.totalDots()));
        properties.setProperty("isGateOpen", Boolean.toString(snapshot.isGateOpen()));
        properties.setProperty("lastPacmanGridX", Integer.toString(snapshot.lastPacmanGridX()));
        properties.setProperty("lastPacmanGridY", Integer.toString(snapshot.lastPacmanGridY()));
        properties.setProperty("pacmanX", Integer.toString(snapshot.pacmanX()));
        properties.setProperty("pacmanY", Integer.toString(snapshot.pacmanY()));
        properties.setProperty("pacmanDirection", snapshot.pacmanDirection().name());
        properties.setProperty("pacmanNextDirection", snapshot.pacmanNextDirection().name());
        properties.setProperty("map", encodeMap(snapshot.map()));
        properties.setProperty("ghostCount", Integer.toString(snapshot.ghostSnapshots().size()));

        for (int index = 0; index < snapshot.ghostSnapshots().size(); index++) {
            GhostSnapshot ghost = snapshot.ghostSnapshots().get(index);
            String prefix = "ghost." + index + ".";
            properties.setProperty(prefix + "type", ghost.type().name());
            properties.setProperty(prefix + "x", Integer.toString(ghost.x()));
            properties.setProperty(prefix + "y", Integer.toString(ghost.y()));
            properties.setProperty(prefix + "speed", Integer.toString(ghost.speed()));
            properties.setProperty(prefix + "state", ghost.state().name());
            properties.setProperty(prefix + "direction", ghost.direction().name());
        }

        try {
            Files.createDirectories(storageFile.getParent());
            try (OutputStream outputStream = Files.newOutputStream(storageFile)) {
                properties.store(outputStream, "Pacman save data");
            }
            return true;
        } catch (IOException ex) {
            // Saving should not interrupt gameplay.
            return false;
        }
    }

    public Snapshot load() {
        if (!hasSave()) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(storageFile)) {
            properties.load(inputStream);

            int score = Integer.parseInt(properties.getProperty("score", "0"));
            int lives = Integer.parseInt(properties.getProperty("lives", "3"));
            int comboMultiplier = Integer.parseInt(properties.getProperty("comboMultiplier", "1"));
            boolean extraLifeGiven = Boolean.parseBoolean(properties.getProperty("extraLifeGiven", "false"));
            int level = Integer.parseInt(properties.getProperty("level", "1"));
            int totalDots = Integer.parseInt(properties.getProperty("totalDots", "0"));
            boolean isGateOpen = Boolean.parseBoolean(properties.getProperty("isGateOpen", "false"));
            int lastPacmanGridX = Integer.parseInt(properties.getProperty("lastPacmanGridX", "-1"));
            int lastPacmanGridY = Integer.parseInt(properties.getProperty("lastPacmanGridY", "-1"));
            int pacmanX = Integer.parseInt(properties.getProperty("pacmanX", "0"));
            int pacmanY = Integer.parseInt(properties.getProperty("pacmanY", "0"));
            Direction pacmanDirection = Direction
                    .valueOf(properties.getProperty("pacmanDirection", Direction.NONE.name()));
            Direction pacmanNextDirection = Direction
                    .valueOf(properties.getProperty("pacmanNextDirection", Direction.NONE.name()));
            int[][] map = decodeMap(properties.getProperty("map", ""));
            int ghostCount = Integer.parseInt(properties.getProperty("ghostCount", "0"));

            List<GhostSnapshot> ghostSnapshots = new ArrayList<>();
            for (int index = 0; index < ghostCount; index++) {
                String prefix = "ghost." + index + ".";
                GhostType type = GhostType.valueOf(properties.getProperty(prefix + "type", GhostType.BLINKY.name()));
                int x = Integer.parseInt(properties.getProperty(prefix + "x", "0"));
                int y = Integer.parseInt(properties.getProperty(prefix + "y", "0"));
                int speed = Integer.parseInt(properties.getProperty(prefix + "speed", "2"));
                GhostState state = GhostState
                        .valueOf(properties.getProperty(prefix + "state", GhostState.CHASE.name()));
                Direction direction = Direction
                        .valueOf(properties.getProperty(prefix + "direction", Direction.RIGHT.name()));
                ghostSnapshots.add(new GhostSnapshot(type, x, y, speed, state, direction));
            }

            return new Snapshot(score, lives, comboMultiplier, extraLifeGiven, level, totalDots, isGateOpen,
                    lastPacmanGridX, lastPacmanGridY, pacmanX, pacmanY, pacmanDirection, pacmanNextDirection, map,
                    ghostSnapshots);
        } catch (IOException | IllegalArgumentException ex) {
            clear();
            return null;
        }
    }

    private String encodeMap(int[][] map) {
        // UC-09 - Lưu và tiếp tục ván chơi
        // ID: UC-09
        // Tên UC: Lưu và tiếp tục ván chơi
        // Chức năng: Mã hóa ma trận lưới 2D của bản đồ thành chuỗi tuyến tính để lưu
        // vào tệp Properties
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < map.length; row++) {
            if (row > 0) {
                builder.append(';');
            }
            for (int col = 0; col < map[row].length; col++) {
                if (col > 0) {
                    builder.append(',');
                }
                builder.append(map[row][col]);
            }
        }
        return builder.toString();
    }

    private int[][] decodeMap(String encodedMap) {
        // UC-09 - Lưu và tiếp tục ván chơi
        // ID: UC-09
        // Tên UC: Lưu và tiếp tục ván chơi
        // Chức năng: Giải mã chuỗi đã lưu từ Properties thành ma trận lưới 2D của bản
        // đồ
        String[] rows = encodedMap.split(";");
        int[][] map = new int[rows.length][];

        for (int row = 0; row < rows.length; row++) {
            String[] cells = rows[row].split(",");
            map[row] = new int[cells.length];
            for (int col = 0; col < cells.length; col++) {
                map[row][col] = Integer.parseInt(cells[col]);
            }
        }

        return map;
    }

    public record Snapshot(int score, int lives, int comboMultiplier, boolean extraLifeGiven, int level,
            int totalDots, boolean isGateOpen, int lastPacmanGridX, int lastPacmanGridY, int pacmanX, int pacmanY,
            Direction pacmanDirection, Direction pacmanNextDirection, int[][] map, List<GhostSnapshot> ghostSnapshots) {
    }

    public record GhostSnapshot(GhostType type, int x, int y, int speed, GhostState state, Direction direction) {
    }
}