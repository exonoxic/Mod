"""
The Undertext script: twelve 5x5 letterforms used everywhere the lower writing shows through
(the veil, particles, stone, the sky). Consistency of this one motif ties the art together.
"""
GLYPHS = [
    ["..#..", ".#.#.", "#...#", ".#.#.", "..#.."],
    ["####.", "#....", "###..", "#....", "#...."],
    ["#...#", "##..#", "#.#.#", "#..##", "#...#"],
    [".###.", "#....", "#.##.", "#...#", ".###."],
    ["#.#.#", "#.#.#", "#####", "..#..", "..#.."],
    ["###..", "..#..", "..###", "....#", "...##"],
    [".#...", "###..", ".#.#.", "...##", "....#"],
    ["#....", "#.##.", "##..#", "#...#", "#...#"],
    ["..##.", ".#...", "#####", "...#.", ".##.."],
    ["#...#", ".#.#.", "..#..", ".#...", "#...."],
    [".##..", "#..#.", ".##..", "#..#.", ".##.#"],
    ["#####", "....#", "..##.", ".#...", "#####"],
]


def draw(canvas, glyph, x, y, color, alpha=255, scale=1):
    g = GLYPHS[glyph % len(GLYPHS)]
    for gy, row in enumerate(g):
        for gx, ch in enumerate(row):
            if ch == "#":
                for sy in range(scale):
                    for sx in range(scale):
                        canvas.set(x + gx * scale + sx, y + gy * scale + sy, color, alpha)


def draw_small(canvas, glyph, x, y, color, alpha=255):
    """A 3x3 reduction for 16px textures."""
    g = GLYPHS[glyph % len(GLYPHS)]
    for gy in range(3):
        for gx in range(3):
            if g[gy * 2][gx * 2] == "#":
                canvas.set(x + gx, y + gy, color, alpha)
