"""
Trims OurAirports airports.csv + runways.csv into one compact JSON file for the app.

Usage (from the project root):
    python tools/prepare_airports.py

Input:  tools/raw/airports.csv, tools/raw/runways.csv
Output: app/src/main/assets/airports.json
"""

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AIRPORTS_IN = ROOT / "tools" / "raw" / "airports.csv"
RUNWAYS_IN = ROOT / "tools" / "raw" / "runways.csv"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "airports.json"

# Large and medium airports are always kept; small ones only if they have airline service.
ALWAYS_KEEP = {"large_airport", "medium_airport"}


def pick_icao(row: dict) -> str:
    # Newer exports have a dedicated icao_code column; older ones only have gps_code/ident.
    for key in ("icao_code", "gps_code", "ident"):
        value = (row.get(key) or "").strip().upper()
        if len(value) == 4 and value.isalnum():
            return value
    return ""


def should_keep(row: dict) -> bool:
    airport_type = row.get("type", "")
    if airport_type in ALWAYS_KEEP:
        return True
    return airport_type == "small_airport" and row.get("scheduled_service") == "yes"


def runway_sort_key(ident: str):
    # Sort numerically by heading, then by L/C/R suffix: 08L, 08R, 26L, 26R.
    digits = "".join(ch for ch in ident if ch.isdigit())
    suffix = "".join(ch for ch in ident if not ch.isdigit())
    return (int(digits) if digits else 99, suffix)


def load_airports() -> dict:
    """Returns {ourairports_ident: airport_dict} for airports we keep."""
    by_ident = {}
    seen_icao = set()

    with AIRPORTS_IN.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if not should_keep(row):
                continue

            icao = pick_icao(row)
            if not icao or icao in seen_icao:
                continue
            seen_icao.add(icao)

            # Key by OurAirports' ident because that's what runways.csv references.
            by_ident[row["ident"].strip().upper()] = {
                "icao": icao,
                "iata": (row.get("iata_code") or "").strip().upper(),
                "name": (row.get("name") or "").strip(),
                "city": (row.get("municipality") or "").strip(),
                "country": (row.get("iso_country") or "").strip(),
                "rwys": [],
            }
    return by_ident


def attach_runways(by_ident: dict) -> int:
    count = 0
    with RUNWAYS_IN.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row.get("closed") == "1":
                continue

            airport = by_ident.get((row.get("airport_ident") or "").strip().upper())
            if airport is None:
                continue

            # Each row is one physical runway with two usable ends.
            for key in ("le_ident", "he_ident"):
                ident = (row.get(key) or "").strip().upper()
                # Skip blanks and helipads (H1, H2...).
                if not ident or ident.startswith("H"):
                    continue
                if ident not in airport["rwys"]:
                    airport["rwys"].append(ident)
                    count += 1
    return count


def main() -> None:
    by_ident = load_airports()
    runway_count = attach_runways(by_ident)

    airports = sorted(by_ident.values(), key=lambda a: a["icao"])
    for airport in airports:
        airport["rwys"].sort(key=runway_sort_key)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8") as f:
        # Compact separators keep the APK smaller; ensure_ascii=False preserves accented names.
        json.dump(airports, f, ensure_ascii=False, separators=(",", ":"))

    size_kb = OUTPUT.stat().st_size / 1024
    print(f"Wrote {len(airports)} airports, {runway_count} runway ends "
          f"to {OUTPUT} ({size_kb:.0f} KB)")


if __name__ == "__main__":
    main()