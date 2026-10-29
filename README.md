# FDV Flight Logger

A flight-sim logbook for Android, built for virtual airline pilots. Log each flight as you fly it, from departure runway to landing flaps. Import plans straight from SimBrief, track your progress on a stats dashboard, and review, search, edit, export and back up your history.

Built with Jetpack Compose, Material 3 and Room. Designed around [FlyDelta Virtual (FDV)](https://www.flydeltavirtual.com/), but usable by any VA pilot. Airport, runway and aircraft data are bundled in the app, and everything works offline except SimBrief import.

**Current version:** 1.1.0

---

## ✈️ Features

### Dashboard
- **Home screen with animated arc gauges:** flights and block hours (filling toward the next milestone), online share, and hub share
- **Summary tiles:** total great-circle distance, airports visited, longest flight
- **Top aircraft and top routes**, plus a tappable **last flight** card
- Updates live as flights are saved, edited, deleted or restored

### Logging
- **Structured flight entry** in four tabbed sections: Departure + Enroute, Arrival, Aircraft + Performance, and ATC
- **Three flight types:** Online, Offline with ATC, and Offline No ATC. The ATC section hides when it doesn't apply.
- **Always-visible scratchpad** for taxi instructions, clearances and notes
- **Auto-save** about every 60 seconds and when you leave the screen, updating the same record rather than creating duplicates
- **Unsaved-changes guard** that confirms before you discard edits; in-progress flights survive rotation and trips to Settings
- **Fly Return Leg** and **Duplicate** from any saved flight

### SimBrief import
- One tap imports your latest SimBrief plan: airports, runways, alternate, SID/STAR, route, cruise level, CI, airline, flight number, aircraft, fuel, weights, times, cruise wind and OAT, flaps, V2/Vref and QNH
- Weights, QNH and temperature are converted to your unit settings
- Planned fields replace what's there; gates, ATC details, scratchpad and flight type are kept
- Requests are made only when you tap Import, per SimBrief's API guidelines

### Smart lookups
- **Airport search** for DEP, ARR and ALTN: about 5,900 airports, by ICAO code, name or city
- **Runway picker:** the RWY fields list the runways at the selected airport
- **Great-circle distance** between DEP and ARR on the route card
- **Aircraft search** and **aircraft-aware flap settings** (Airbus `1+F / 2 / 3`, Boeing `1 / 5 / 15`, and so on)
- **Airline list:** Delta, Delta Connection carriers, current SkyTeam members and Delta codeshare partners
- **Delta Connection handling:** flights operated by Endeavor, SkyWest, Republic and other Connection carriers display under their DAL flight number everywhere
- **Learned suggestions:** Gate, SID and STAR suggest what you've used before at that airport (ranked by runway, then frequency); an empty Route field offers routes previously flown between the same city pair
- Every lookup field still accepts free text

### Input formatting
- Times type as `0345` and display as `03:45`
- Cruise wind types as `29045` and displays as `290/45`
- QNH displays as `29.92` (inHg) or `1013` (hPa), following your unit setting
- Procedure, gate, alternate and route fields are capitalized automatically
- Values are stored as clean digits, and formatting is applied only for display

### History, export & backup
- Flight history with search across route, airline, flight number (including DAL numbers), aircraft, procedures and notes
- Newest-first / oldest-first sorting
- Flight detail view with return leg, duplicate, edit and delete
- **Export to CSV and PDF**, with a Share shortcut when the export finishes
- **Back up and restore** the full logbook as versioned JSON; restoring merges and skips flights you already have

### Interface
- Raised, reflective card styling with inset fields and FDV blue/red branding
- Responsive layouts for **compact** (phones), **medium** (foldables / small tablets) and **expanded** (tablets) widths
- Initial pilot setup (Pilot ID, name, hub, units), plus a SimBrief / Navigraph alias in Settings

---

## 📱 Requirements

- **Android 12 (API 31) or newer**
- Optimized for tablets and fully usable on phones and foldables
- Internet access only for SimBrief import
- Tested on Samsung Galaxy Tab devices and modern Android phones

---

## 📥 Installing

1. Download the latest `app-release.apk` from the [Releases](../../releases) page.
2. Open it on your device. If prompted, allow your browser or file manager to **install unknown apps**.
3. When updating, install over the existing app. Your logbook carries over.

> 💾 Flights are stored only on your device. Use **History → ⋮ → Back Up Logbook** regularly, and always before uninstalling or switching devices.

A user guide PDF is attached to each release.

---

## 🧰 Tech Stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 (custom Canvas gauges) |
| Architecture | MVVM (`AndroidViewModel` + `StateFlow`) |
| Persistence | Room (with migrations) + DataStore |
| Networking | `HttpURLConnection` + `org.json` (SimBrief only, no extra libraries) |
| Navigation | Navigation Compose |
| Build | Gradle (Kotlin DSL, Version Catalog) + KSP |
| Tests | JUnit (JVM unit tests) |
| Min / Target SDK | 31 / 36 |

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio (recent stable). The bundled JDK is fine.
- Android SDK 36

### Debug build
```bash
git clone https://github.com/Shadehawke/fdv-flight-logger.git
```
Open the project in Android Studio and run the `app` configuration. Debug builds use the `.debug` application ID suffix, so they install alongside a release build with their own separate logbook.

### Running tests
```bash
./gradlew testDebugUnitTest
```
Unit tests cover the statistics calculator (totals, rankings, time parsing, milestones) and run on the JVM without a device.

### Signed release build
Release signing reads its credentials from an untracked `keystore.properties` in the project root:

```properties
storeFile=C\:/path/to/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then:
```bash
./gradlew assembleRelease
```
The signed APK is written to `app/build/outputs/apk/release/app-release.apk`.

> `keystore.properties` and `*.jks` are git-ignored. Never commit them, and back up your keystore. You can't publish updates without it.

### Regenerating airport & runway data
The bundled `app/src/main/assets/airports.json` (airports, runways and coordinates) is generated from [OurAirports](https://ourairports.com/data/) data:

1. Download `airports.csv` and `runways.csv` from OurAirports into `tools/raw/` (git-ignored).
2. Run:
   ```bash
   python tools/prepare_airports.py
   ```
3. Rebuild the app.

---

## 🗂️ Project Structure

```
app/src/main/
├── assets/airports.json          # Trimmed airport, runway and coordinate data
└── java/com/fdv/fdvflightlogger/
    ├── data/
    │   ├── Airline.kt            # Airline list + DAL flight-number formatting
    │   ├── aircraft/             # Aircraft types and flap settings by family
    │   ├── airports/             # Airport repository (search, runways, distance)
    │   ├── backup/               # Versioned JSON backup format
    │   ├── db/                   # Room entities, DAO, migrations
    │   ├── history/              # Logbook-learned suggestions
    │   ├── prefs/                # DataStore profile + settings
    │   ├── simbrief/             # SimBrief client, parser and plan model
    │   └── stats/                # Dashboard statistics calculator
    ├── export/                   # CSV and PDF export
    └── ui/
        ├── components/           # Reusable widgets (ArcGauge)
        ├── mappers/              # Entity/draft, SimBrief and template mapping
        ├── nav/                  # Routes and nav graph
        ├── screens/              # Dashboard, Log, History, Detail, Setup, Settings
        └── theme/                # Colors, typography, raised/inset surfaces
tools/
└── prepare_airports.py           # Builds airports.json from OurAirports CSVs
```

---

## 🗺️ Roadmap

- [x] Flight detail view
- [x] Editable flight entries
- [x] CSV and PDF export
- [x] Search and sort
- [x] Airport, runway and aircraft data
- [x] Flight statistics dashboard
- [x] SimBrief import
- [x] Backup and restore
- [x] Return leg and duplicate
- [x] Logbook-learned gate, SID, STAR and route suggestions
- [ ] XLSX export
- [ ] Filtering (by date range, airline, aircraft)
- [ ] Airline-aware aircraft suggestions (selected airline's fleet first)
- [ ] Configurable VA branding

---

## 🙏 Data Sources

- Airport, runway and coordinate data from [OurAirports](https://ourairports.com/data/), released into the public domain.
- Flight plan import uses the [SimBrief](https://www.simbrief.com/) API.

---

## ⚠️ Disclaimer

This project is not affiliated with or endorsed by Delta Air Lines, FlyDelta Virtual, SimBrief, Navigraph, or any airline named in the app. All trademarks belong to their respective owners.

For flight simulation use only. Not for real-world navigation or flight planning.

---

## 📄 License

Licensed under the MIT License. See [`LICENSE`](LICENSE) for details.
