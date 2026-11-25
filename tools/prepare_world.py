"""
Packs Natural Earth coastlines and country borders into a small binary file for the route map.

Usage (from the project root):
    python tools/prepare_world.py

Input (public domain, from https://www.naturalearthdata.com or the
nvkelso/natural-earth-vector repository on GitHub, geojson folder):
    tools/raw/ne_50m_land.geojson
    tools/raw/ne_50m_admin_0_boundary_lines_land.geojson
Output:
    app/src/main/assets/world.bin

Format (big-endian, read by WorldMap.kt):
    4 bytes  "FDVW"
    1 byte   version (1)
    int32    land ring count, then for each ring: int32 point count, then
             point count x (int16 lon, int16 lat) in hundredths of a degree
    int32    border line count, then lines in the same layout
"""

import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAND_IN = ROOT / "tools" / "raw" / "ne_50m_land.geojson"
BORDERS_IN = ROOT / "tools" / "raw" / "ne_50m_admin_0_boundary_lines_land.geojson"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "world.bin"

# Detail kept: 0.02 degrees is about 2 km, finer than the map ever shows
TOLERANCE = 0.02
# Islands smaller than this (square degrees) are left out; they'd be a speck at any zoom
MIN_LAND_AREA = 0.01


def simplify(points, tolerance):
    """Ramer-Douglas-Peucker: drops points that sit within `tolerance` of the line between their neighbours."""
    if len(points) < 3:
        return points
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        start, end = stack.pop()
        (x1, y1), (x2, y2) = points[start], points[end]
        dx, dy = x2 - x1, y2 - y1
        length = (dx * dx + dy * dy) ** 0.5
        farthest, index = 0.0, -1
        for i in range(start + 1, end):
            px, py = points[i]
            if length == 0:
                distance = ((px - x1) ** 2 + (py - y1) ** 2) ** 0.5
            else:
                distance = abs(dy * px - dx * py + x2 * y1 - y2 * x1) / length
            if distance > farthest:
                farthest, index = distance, i
        if farthest > tolerance:
            keep[index] = True
            stack.append((start, index))
            stack.append((index, end))
    return [p for p, k in zip(points, keep) if k]


def area(ring):
    """Shoelace formula, in square degrees."""
    total = 0.0
    for (x1, y1), (x2, y2) in zip(ring, ring[1:] + ring[:1]):
        total += x1 * y2 - x2 * y1
    return abs(total) / 2


def rings_of(geometry):
    kind, coordinates = geometry["type"], geometry["coordinates"]
    if kind == "Polygon":
        return coordinates
    if kind == "MultiPolygon":
        return [ring for polygon in coordinates for ring in polygon]
    if kind == "LineString":
        return [coordinates]
    if kind == "MultiLineString":
        return coordinates
    return []


def load(path, closed):
    shapes = []
    for feature in json.loads(path.read_text(encoding="utf-8"))["features"]:
        for ring in rings_of(feature["geometry"]):
            points = [(float(x), float(y)) for x, y, *_ in ring]
            if closed and area(points) < MIN_LAND_AREA:
                continue
            points = simplify(points, TOLERANCE)
            if len(points) >= (3 if closed else 2):
                shapes.append(points)
    return shapes


def pack(shapes):
    out = bytearray(struct.pack(">i", len(shapes)))
    for points in shapes:
        out += struct.pack(">i", len(points))
        for x, y in points:
            out += struct.pack(">hh", round(x * 100), round(y * 100))
    return out


def main():
    land = load(LAND_IN, closed=True)
    borders = load(BORDERS_IN, closed=False)
    data = b"FDVW" + bytes([1]) + pack(land) + pack(borders)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(data)
    points = sum(map(len, land)) + sum(map(len, borders))
    print(f"{len(land)} land rings, {len(borders)} border lines, {points:,} points, {len(data):,} bytes -> {OUTPUT}")


if __name__ == "__main__":
    main()
