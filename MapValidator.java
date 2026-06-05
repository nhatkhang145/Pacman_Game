import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapValidator {
    public static void main(String[] args) throws IOException {
        Path path = Path.of("src/main/java/com/example/pacmangame/model/MapManager.java");
        String text = Files.readString(path);
        Pattern pat = Pattern.compile("private static int\\[\\]\\[\\] getMapLevel(\\d+)\\s*\\(\\) \\{\\s*return new int\\[\\]\\[\\] \\{(.*?)\\};", Pattern.DOTALL);
        Matcher matcher = pat.matcher(text);
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(1));
            String body = matcher.group(2);
            List<int[]> rows = new ArrayList<>();
            for (String line : body.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("{") && trimmed.contains("}")) {
                    List<Integer> values = new ArrayList<>();
                    Matcher numMatcher = Pattern.compile("\\d+").matcher(trimmed);
                    while (numMatcher.find()) {
                        values.add(Integer.parseInt(numMatcher.group()));
                    }
                    if (!values.isEmpty()) {
                        int[] row = new int[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            row[i] = values.get(i);
                        }
                        rows.add(row);
                    }
                }
            }
            analyze(level, rows);
        }
    }

    private static void analyze(int level, List<int[]> rows) {
        int h = rows.size();
        int w = rows.isEmpty() ? 0 : rows.get(0).length;
        int[] start = {12, 9};
        boolean[][] seenClosedGate = bfs(rows, start, false);
        boolean switchReachable = false;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (seenClosedGate[r][c] && rows.get(r)[c] == 8) {
                    switchReachable = true;
                }
            }
        }
        boolean[][] seen = switchReachable ? bfs(rows, start, true) : seenClosedGate;
        System.out.printf("  switchReachable=%b\n", switchReachable);
        int totalDots = 0;
        int reachableDots = 0;
        int unreachableOpen = 0;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int v = rows.get(r)[c];
                if (v == 2 || v == 3) totalDots++;
                if ((v == 0 || v == 2 || v == 3 || v == 4 || v == 5 || v == 6 || v == 7 || v == 8) && !seen[r][c]) unreachableOpen++;
                if ((v == 2 || v == 3) && seen[r][c]) reachableDots++;
            }
        }
        if (unreachableOpen > 0) {
            System.out.println("  unreachable open tiles:");
            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    int v = rows.get(r)[c];
                    if ((v == 0 || v == 2 || v == 3 || v == 4 || v == 5 || v == 6 || v == 7 || v == 8) && !seen[r][c]) {
                        System.out.printf("    (%d,%d)=%d\n", r, c, v);
                    }
                }
            }
        }
        if (!switchReachable) {
            printReachability(rows, seen, start);
        }
        System.out.printf("level %d %dx%d start=%d reachableDots=%d/%d unreachableOpen=%d\n", level, h, w, rows.get(start[0])[start[1]], reachableDots, totalDots, unreachableOpen);
    }

    private static void printReachability(List<int[]> rows, boolean[][] seen, int[] start) {
        System.out.println("  reachability map (S=start, .=reachable, ?=unreachable open, #=wall, X=switch):");
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < rows.get(r).length; c++) {
                if (r == start[0] && c == start[1]) {
                    sb.append('S');
                } else if (rows.get(r)[c] == 1) {
                    sb.append('#');
                } else if (rows.get(r)[c] == 8) {
                    sb.append('X');
                } else if (seen[r][c]) {
                    sb.append('.');
                } else {
                    sb.append('?');
                }
            }
            System.out.println("  " + sb);
        }
    }

    private static boolean[][] bfs(List<int[]> rows, int[] start, boolean openGates) {
        int h = rows.size();
        int w = rows.isEmpty() ? 0 : rows.get(0).length;
        boolean[][] seen = new boolean[h][w];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(start);
        seen[start[0]][start[1]] = true;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int r = p[0];
            int c = p[1];
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 0 && nr < h && nc >= 0 && nc < w && !seen[nr][nc]) {
                    int v = rows.get(nr)[nc];
                    if (v != 1 && (openGates || v != 9)) {
                        seen[nr][nc] = true;
                        q.add(new int[]{nr,nc});
                    }
                }
            }
        }
        return seen;
    }
}
