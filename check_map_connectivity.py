from pathlib import Path
import re

text = Path('src/main/java/com/example/pacmangame/model/MapManager.java').read_text()
pat = re.compile(r'private static int\[\]\[\] getMapLevel(\d+)\(\) \{\s*return new int\[\]\[\] \{(.*?)\};', re.S)

levels = {}
for m in pat.finditer(text):
    level = int(m.group(1))
    rows = []
    for line in m.group(2).splitlines():
        line = line.strip()
        if line.startswith('{') and '}' in line:
            rows.append(list(map(int, re.findall(r'\d+', line))))
    levels[level] = rows

start = (12, 9)

def bfs(rows, start, open_gates=False):
    h = len(rows)
    w = len(rows[0])
    seen = [[False] * w for _ in range(h)]
    q = [start]
    seen[start[0]][start[1]] = True
    for r, c in q:
        for dr, dc in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
            nr, nc = r + dr, c + dc
            if 0 <= nr < h and 0 <= nc < w and not seen[nr][nc]:
                v = rows[nr][nc]
                if v != 1 and (open_gates or v != 9):
                    seen[nr][nc] = True
                    q.append((nr, nc))
    return seen

for lvl in [1, 2, 3, 6, 10]:
    rows = levels[lvl]
    closed = bfs(rows, start, open_gates=False)
    sw_reached = [(r, c) for r in range(len(rows)) for c in range(len(rows[0])) if closed[r][c] and rows[r][c] == 8]
    opened = bfs(rows, start, open_gates=True) if sw_reached else closed
    total = sum(1 for r in rows for v in r if v in [2,3])
    reached = sum(1 for r in range(len(rows)) for c in range(len(rows[0])) if opened[r][c] and rows[r][c] in [2,3])
    unreachable_open = [(r,c,rows[r][c]) for r in range(len(rows)) for c in range(len(rows[0])) if rows[r][c] in [0,2,3,4,5,6,7,8] and not opened[r][c]]
    print(f'level {lvl}: switch reached={bool(sw_reached)}, switch coords={sw_reached}')
    print(f'  total dots={total}, reached={reached}, unreachable_open={len(unreachable_open)}')
    print('  sample unreachable', unreachable_open[:20])
    print()
