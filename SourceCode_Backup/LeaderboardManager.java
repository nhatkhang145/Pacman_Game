package com.example.pacmangame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LeaderboardManager {
    private static final int DEFAULT_LIMIT = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private static LeaderboardManager instance;

    private final Path storageFile;
    private final List<Entry> entries;

    private LeaderboardManager() {
        storageFile = Paths.get(System.getProperty("user.home"), ".pacman-game", "leaderboard.tsv");
        entries = loadEntries();
    }

    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    public synchronized void addScore(String playerName, int score) {
        // UC-07 - Hệ thống bảng xếp hạng
        // ID: UC-07
        // Tên UC: Hệ thống bảng xếp hạng
        // Chức năng: Chuẩn hóa tên người chơi, lưu điểm mới và sắp xếp danh sách Top 10
        // theo nguyên tắc ưu tiên kép: Điểm giảm dần -> Thời gian tăng dần (nếu điểm
        // bằng nhau ưu tiên thời gian sớm hơn)
        entries.add(new Entry(sanitizeName(playerName), score, System.currentTimeMillis()));
        entries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::timestamp));

        if (entries.size() > DEFAULT_LIMIT) {
            entries.subList(DEFAULT_LIMIT, entries.size()).clear();
        }

        saveEntries();
    }

    public synchronized boolean isHighScore(int score) {
        if (entries.size() < DEFAULT_LIMIT) {
            return true;
        }

        Entry lowest = entries.get(entries.size() - 1);
        return score >= lowest.score();
    }

    public synchronized String buildLeaderboardText(int limit, boolean isVietnamese) {
        StringBuilder builder = new StringBuilder();
        builder.append(isVietnamese ? "TOP ".concat(String.valueOf(limit)).concat(" ĐIỂM")
                : "TOP ".concat(String.valueOf(limit)).concat(" SCORES"));
        builder.append(System.lineSeparator()).append(System.lineSeparator());

        if (entries.isEmpty()) {
            builder.append(isVietnamese ? "Chưa có dữ liệu." : "No scores yet.");
            return builder.toString();
        }

        int rank = 1;
        for (Entry entry : entries.subList(0, Math.min(limit, entries.size()))) {
            builder.append(String.format("%2d. %-12s %8d  %s", rank++, entry.name(), entry.score(),
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))));
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    private List<Entry> loadEntries() {
        List<Entry> loadedEntries = new ArrayList<>();

        try {
            if (!Files.exists(storageFile)) {
                return loadedEntries;
            }

            for (String line : Files.readAllLines(storageFile, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length < 3) {
                    continue;
                }

                String name = sanitizeName(parts[0]);
                int score = Integer.parseInt(parts[1]);
                long timestamp = Long.parseLong(parts[2]);
                loadedEntries.add(new Entry(name, score, timestamp));
            }

            loadedEntries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::timestamp));
            if (loadedEntries.size() > DEFAULT_LIMIT) {
                loadedEntries.subList(DEFAULT_LIMIT, loadedEntries.size()).clear();
            }
        } catch (IOException | NumberFormatException ex) {
            loadedEntries.clear();
        }

        return loadedEntries;
    }

    private void saveEntries() {
        try {
            Files.createDirectories(storageFile.getParent());

            List<String> lines = new ArrayList<>();
            for (Entry entry : entries) {
                lines.add(entry.name() + "\t" + entry.score() + "\t" + entry.timestamp());
            }

            Files.write(storageFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // Offline leaderboard should never block gameplay if saving fails.
        }
    }

    private static String sanitizeName(String playerName) {
        // UC-07 - Hệ thống bảng xếp hạng (tách hàm chuẩn hóa tên)
        // ID: UC-07
        // Tên UC: Hệ thống bảng xếp hạng
        // Chức năng: Chuẩn hóa và giới hạn độ dài tên người chơi trước khi lưu (loại bỏ
        // ký tự điều khiển, trim,
        // mặc định "PLAYER" khi rỗng, cắt tối đa 12 ký tự)
        if (playerName == null) {
            return "PLAYER";
        }

        String cleaned = playerName.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (cleaned.isEmpty()) {
            return "PLAYER";
        }

        return cleaned.length() > 12 ? cleaned.substring(0, 12) : cleaned;
    }

    private static final class Entry {
        private final String name;
        private final int score;
        private final long timestamp;

        private Entry(String name, int score, long timestamp) {
            this.name = name;
            this.score = score;
            this.timestamp = timestamp;
        }

        private String name() {
            return name;
        }

        private int score() {
            return score;
        }

        private long timestamp() {
            return timestamp;
        }
    }
}