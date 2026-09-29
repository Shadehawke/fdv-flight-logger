# FDV Flight Logger
 
A flight-sim logbook for Android, built for virtual airline pilots. Log each flight as you fly it, from departure runway to landing flaps. Afterwards you can review, search, edit and export your history as CSV or PDF.
 
Built with Jetpack Compose, Material 3 and Room. Designed around [FlyDelta Virtual (FDV)](https://www.flydeltavirtual.com/), but usable by any VA pilot. It works fully offline, and airport, runway and aircraft data are bundled in the app.
 
**Current version:** 1.0.0
 
---
 
## ✈️ Features
 
### Logging
- **Structured flight entry** in four tabbed sections: Departure + Enroute, Arrival, Aircraft + Performance, and ATC
- **Three flight types:** Online, Offline with ATC, and Offline No ATC. The ATC section hides when it doesn't apply.
- **Always-visible scratchpad** for taxi instructions, clearances and notes
- **Auto-save** about every 60 seconds and when you leave the screen, updating the same record rather than creating duplicates
- **Unsaved-changes guard** that confirms before you discard edits
### Smart lookups
- **Airport search:** about 5,900 airports, searchable by ICAO code, name or city, with the name confirmed under the field
- **Runway picker:** the RWY fields list the runways at the selected DEP/ARR airport
- **Aircraft search:** common airliner types by ICAO code or name
- **Aircraft-aware flap settings:** takeoff and landing flap suggestions match the aircraft family (Airbus `1+F / 2 / 3`, Boeing `1 / 5 / 15 / 30`, and so on)
- **Airline list:** Delta, Delta Connection carriers, current SkyTeam members and Delta codeshare partners
- **Delta Connection handling:** flights operated by Endeavor, SkyWest, Republic and other Connection carriers display under their DAL flight number everywhere
- Every lookup field still accepts free text
### Input formatting
- Times type as `0345` and display as `03:45`
- Cruise wind types as `29045` and displays as `290/45`
- QNH displays as `29.92` (inHg) or `1013` (hPa), following your unit setting
- Procedure, gate, alternate and route fields are capitalized automatically
- Values are stored as clean digits, and formatting is applied only for display
### History & export
- Flight history with search across route, airline, flight number, aircraft, procedures and notes
- Newest-first / oldest-first sorting
- Flight detail view with edit and delete
- **Export to CSV and PDF**, with a Share shortcut when the export finishes
- Last-landed airport tracking
### Interface
- Raised, reflective card styling with inset fields and FDV blue/red branding
- Responsive layouts for **compact** (phones), **medium** (foldables / small tablets) and **expanded** (tablets) widths
- Initial pilot setup (Pilot ID, name, hub, QNH and temperature units)
---
 
## 📱 Requirements
 
- **Android 12 (API 31) or newer**
- Optimized for tablets and fully usable on phones and foldables
- Tested on Samsung Galaxy Tab devices and modern Android phones
---
 
## 📥 Installing
 
1. Download the latest `app-release.apk` from the [Releases](../../releases) page.
2. Open it on your device. If prompted, allow your browser or file manager to **install unknown apps**.
3. When updating, install over the existing app. Your logbook carries over.
> 💾 Flights are stored only on your device. Export a CSV now and then as a backup, because uninstalling the app deletes its data.
 
A user guide PDF is attached to each release.
 
---
 
## 🧰 Tech Stack
 
| Area | Choice |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (`AndroidViewModel` + `StateFlow`) |
| Persistence | Room (with migrations) + DataStore |
| Navigation | Navigation Compose |
| Build | Gradle (Kotlin DSL, Version Catalog) + KSP |
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
Open the project in Android Studio and run the `app` configuration. Debug builds use the `.debug` application ID suffix, so they install alongside a release build.
 
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
The bundled `app/src/main/assets/airports.json` is generated from [OurAirports](https://ourairports.com/data/) data:
 
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
├── assets/airports.json          # Trimmed airport + runway data
└── java/com/fdv/fdvflightlogger/
    ├── data/
    │   ├── Airline.kt            # Airline list + DAL flight-number formatting
    │   ├── aircraft/             # Aircraft types and flap settings by family
    │   ├── airports/             # Airport repository (search, runways)
    │   ├── db/                   # Room entities, DAO, migrations
    │   └── prefs/                # DataStore profile + settings
    ├── export/                   # CSV and PDF export
    └── ui/
        ├── screens/              # Log, History, Detail, Setup, Settings
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
- [ ] XLSX export
- [ ] Filtering (by date range, airline, aircraft)
- [ ] Airline-aware aircraft suggestions (selected airline's fleet first)
- [ ] Configurable VA branding
- [ ] Flight statistics (totals, hours, most-flown routes)
---
 
## 🙏 Data Sources
 
- Airport and runway data from [OurAirports](https://ourairports.com/data/), released into the public domain.
---
 
## ⚠️ Disclaimer
 
This project is not affiliated with or endorsed by Delta Air Lines, FlyDelta Virtual, or any airline named in the app. All trademarks belong to their respective owners.
 
For flight simulation use only. Not for real-world navigation or flight planning.
 
---
 
## 📄 License
 
Licensed under the MIT License. See [`LICENSE`](LICENSE) for details.
