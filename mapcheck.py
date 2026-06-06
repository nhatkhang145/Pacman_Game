from pathlib import Path
import collections

text = Path('src/main/java/com/example/pacmangame/model/MapManager.java').read_text()
maps = {}
current = None
for line in text.splitlines():
    if 'private static int[][] getMapLevel' in line:
        current = int(line.split('getMapLevel')[1].split('(')[0])
        maps[current] = []
        continue
    if current is not None:
        stripped = line.strip()
        if stripped.startswith('{') and (stripped.endswith('},') or stripped.endswith('}')):
            content = stripped.strip('{} ,')
            if content:
                row = [int(x.strip()) for x in content.split(',') if x.strip()]
                if row:
                    maps[current].append(row)
        if stripped == '    }' and maps[current] and len(maps[current]) == 19:
            current = None

print('parsed levels', sorted(maps.keys()))
for lvl in sorted(maps.keys()):
    rows = maps[lvl]
    h = len(rows)
    w = len(rows[0]) if rows else 0
    start = (12, 9)
    q = collections.deque([start])
    seen = {start}
    while q:
        r, c = q.popleft()
        for dr, dc in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
            nr, nc = r + dr, c + dc
            if 0 <= nr < h and 0 <= nc < w and (nr, nc) not in seen:
                v = rows[nr][nc]
                if v != 1 and v != 9:
                    seen.add((nr, nc))
                    q.append((nr, nc))
    total = sum(1 for r in rows for v in r if v in (2, 3))
    reach = sum(1 for r, c in seen if rows[r][c] in (2, 3))
    unreachable = sum(1 for r in range(h) for c in range(w) if rows[r][c] in (0, 2, 3, 4, 5, 6, 7, 8) and (r, c) not in seen)
    print(f'level {lvl} {h}x{w} totalDots={total} reachable={reach} unreachableOpen={unreachable} startTile={rows[start[0]][start[1]]}')
