package com.example.pacmangame.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LeaderboardDAO {
    private static final int DEFAULT_LIMIT = 10;
    private static final int HISTORY_LIMIT = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private static LeaderboardDAO instance;

    private final Path storageFile;
    private final Path historyFile;
    private final List<Entry> entries;
    private final List<Entry> historyEntries;

    private LeaderboardDAO() {
        storageFile = Paths.get(System.getProperty("user.home"), ".pacman-game", "leaderboard.tsv");
        historyFile = Paths.get(System.getProperty("user.home"), ".pacman-game", "leaderboard_history.tsv");
        entries = loadEntries();
        historyEntries = loadHistoryEntries();
    }

    public static synchronized LeaderboardDAO getInstance() {
        if (instance == null) {
            instance = new LeaderboardDAO();
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
        Entry entry = new Entry(sanitizeName(playerName), score, System.currentTimeMillis());
        entries.add(entry);
        entries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::timestamp));

        if (entries.size() > DEFAULT_LIMIT) {
            entries.subList(DEFAULT_LIMIT, entries.size()).clear();
        }

        historyEntries.add(entry);
        historyEntries.sort(Comparator.comparingLong(Entry::timestamp).reversed());
        if (historyEntries.size() > HISTORY_LIMIT) {
            historyEntries.subList(HISTORY_LIMIT, historyEntries.size()).clear();
        }

        saveEntries();
        saveHistoryEntries();
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

    public synchronized String buildLeaderboardTextFiltered(int limit, boolean isVietnamese, Period period,
            String playerFilter) {
        List<Entry> source = historyEntries.isEmpty() ? entries : historyEntries;
        List<Entry> filtered = filterEntries(source, period, playerFilter);
        filtered.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::timestamp));

        StringBuilder builder = new StringBuilder();
        builder.append(isVietnamese ? "TOP ".concat(String.valueOf(limit)).concat(" ĐIỂM")
                : "TOP ".concat(String.valueOf(limit)).concat(" SCORES"));
        builder.append(System.lineSeparator()).append(System.lineSeparator());

        if (filtered.isEmpty()) {
            builder.append(isVietnamese ? "Chưa có dữ liệu." : "No scores yet.");
            return builder.toString();
        }

        int rank = 1;
        for (Entry entry : filtered.subList(0, Math.min(limit, filtered.size()))) {
            builder.append(String.format("%2d. %-12s %8d  %s", rank++, entry.name(), entry.score(),
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))));
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public synchronized String buildRecentRunsText(int limit, boolean isVietnamese, Period period,
            String playerFilter) {
        StringBuilder builder = new StringBuilder();
        builder.append(isVietnamese ? "LỊCH SỬ GẦN ĐÂY" : "RECENT RUNS");
        builder.append(System.lineSeparator()).append(System.lineSeparator());

        if (historyEntries.isEmpty()) {
            builder.append(isVietnamese ? "Chưa có dữ liệu." : "No runs yet.");
            return builder.toString();
        }

        List<Entry> filtered = filterEntries(historyEntries, period, playerFilter);
        filtered.sort(Comparator.comparingLong(Entry::timestamp).reversed());

        if (filtered.isEmpty()) {
            builder.append(isVietnamese ? "Không có bản ghi phù hợp." : "No matching runs.");
            return builder.toString();
        }

        for (Entry entry : filtered.subList(0, Math.min(limit, filtered.size()))) {
            builder.append(String.format("%s  %-12s %8d", TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                    entry.name(), entry.score()));
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

    private List<Entry> loadHistoryEntries() {
        List<Entry> loadedEntries = new ArrayList<>();

        try {
            if (!Files.exists(historyFile)) {
                return loadedEntries;
            }

            for (String line : Files.readAllLines(historyFile, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length < 3) {
                    continue;
                }

                String name = sanitizeName(parts[0]);
                int score = Integer.parseInt(parts[1]);
                long timestamp = Long.parseLong(parts[2]);
                loadedEntries.add(new Entry(name, score, timestamp));
            }

            loadedEntries.sort(Comparator.comparingLong(Entry::timestamp).reversed());
            if (loadedEntries.size() > HISTORY_LIMIT) {
                loadedEntries.subList(HISTORY_LIMIT, loadedEntries.size()).clear();
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

    private void saveHistoryEntries() {
        try {
            Files.createDirectories(historyFile.getParent());

            List<String> lines = new ArrayList<>();
            for (Entry entry : historyEntries) {
                lines.add(entry.name() + "\t" + entry.score() + "\t" + entry.timestamp());
            }

            Files.write(historyFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // History saving failure should not interrupt gameplay.
        }
    }

    private List<Entry> filterEntries(List<Entry> source, Period period, String playerFilter) {
        List<Entry> filtered = new ArrayList<>();
        String normalizedPlayer = sanitizeName(playerFilter).toLowerCase(Locale.getDefault());
        boolean hasPlayerFilter = playerFilter != null && !playerFilter.trim().isEmpty();
        Instant cutoff = getCutoffInstant(period);

        for (Entry entry : source) {
            if (cutoff != null && Instant.ofEpochMilli(entry.timestamp()).isBefore(cutoff)) {
                continue;
            }

            if (hasPlayerFilter && !entry.name().toLowerCase(Locale.getDefault()).equals(normalizedPlayer)) {
                continue;
            }

            filtered.add(entry);
        }

        return filtered;
    }

    private Instant getCutoffInstant(Period period) {
        if (period == null || period == Period.ALL) {
            return null;
        }

        Instant now = Instant.now();
        return switch (period) {
            case DAY -> now.minus(Duration.ofDays(1));
            case WEEK -> now.minus(Duration.ofDays(7));
            case MONTH -> now.minus(Duration.ofDays(30));
            default -> null;
        };
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

    public enum Period {
        ALL,
        DAY,
        WEEK,
        MONTH
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