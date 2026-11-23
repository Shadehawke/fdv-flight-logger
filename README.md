# FDV Flight Logger

A flight-sim logbook for Android, built for [FlyDelta Virtual (FDV)](https://www.flydeltavirtual.com/) pilots. Log each flight as you fly it, from departure runway to landing flaps. Sign in with your FDV account to pull in your bookings, PIREPs, rank, awards, events and bonuses, or use it on its own as a standalone logbook.

Built with Kotlin, Jetpack Compose, Material 3 and Room. Airport, runway and aircraft data are bundled, so logging works offline. The network is used only for FDV, SimBrief and app updates (see [Privacy](#-privacy)).

**Latest release:** 1.4.0
 
---

## ✈️ Features

### FlyDelta Virtual account
- **Sign in with FDV** (email/password or VATSIM) through the system browser. The app never sees your password.
- **Pilot card:** rank, progress to the next rank, FDV hours (flown, bonus and transfer), flights and flight streak
- **Start from my booking:** fills a new flight from an FDV booking, including the SimBrief plan and gates
- **Get an FDV gate** for flights without a booking, picked the way FDV does for bookings
- **Sync actuals** from your PIREP (block and air time, fuel used, landing rate, passengers), import your **full FDV history**, and **sync new flights** later. Air times fill in in the background.
- **PIREP review status:** Pending and Rejected flights are badged. The dashboard counts approved flights only, like FDV.
- **Trophy Case:** your FDV awards on a wall and your trophies on rarity shelves, with locked ones shown
- **Events and bonuses:** what's live and coming up, including Flight of the Day and Airport of the Month, with times in local and Zulu and your registration status
- **CAT Free and double hours:** the Flight Log tells you when a flight earns them from an event or bonus, following FDV's rules for each, and when to take off and land for it to count
- **Fleet-aware aircraft picker:** shows which FDV types your rank can fly, lifts rank locks on CAT Free and double-hours flights, marks Wheels Up types as any rank, and tells you when a rank waiver pass would be used
### Dashboard
- Animated arc gauges: flights and block hours (toward the next milestone), online share and hub share
- Total distance, airports visited, longest flight, top aircraft and routes, and a last-flight card
- **Landings:** average touchdown, last 10 against your average, the spread across ratings, your best landing and your average by aircraft
- Event card for anything live or coming next, and a notice when an app update is available
### Logging
- Structured entry in four sections: Departure + Enroute, Arrival, Aircraft + Performance and ATC
- Online, Offline with ATC and Offline No ATC flight types. The ATC section hides when it doesn't apply.
- Always-visible scratchpad, auto-save about every 60 seconds, and an unsaved-changes guard
- **Fly Return Leg** and **Duplicate** from any saved flight
### Landing ratings
Every flight with a landing rate gets a rating, based on airline guidance (100–250 fpm is the normal target):

| Rating | Touchdown |
|---|---|
| Floated | under 60 fpm (usually held off too long) |
| Butter | 60–100 fpm |
| Smooth | 101–250 fpm |
| Firm | 251–400 fpm |
| Hard | 401–600 fpm |
| Very hard | over 600 fpm (hard-landing inspection line) |

### SimBrief import
- One tap imports your latest plan: airports, runways, alternate, SID/STAR, route, cruise level, CI, airline, flight number, aircraft, fuel, weights, times, cruise wind and OAT, flaps, V-speeds and QNH, converted to your units
- Requests are made only when you tap Import, per SimBrief's API guidelines
### Smart lookups
- Airport search (about 5,900 airports), runway pickers and great-circle distance
- Aircraft search with aircraft-aware flap settings (Airbus `1+F / 2 / 3`, Boeing `1 / 5 / 15`, …)
- Delta, Delta Connection, SkyTeam and partner airlines. Connection flights show under their DAL number.
- Gate, SID, STAR and route suggestions learned from your own logbook
- Every lookup field still accepts free text
### History, export and backup
- Search (including FDV status and landing rating), sort, and a detail view with edit, return leg, duplicate and delete
- **Filters:** date (presets or a custom range), airline, aircraft, flight type and FDV status
- **CSV and PDF export** with a Share shortcut. Exports follow your search and filters, so you can export one month's flights.
- **Back up and restore** the whole logbook (and your profile photo) as versioned JSON. Restoring merges and skips flights you already have.
### App
- **Built-in help:** searchable, in sections, and available offline (Settings → ?)
- **In-app updates:** checks GitHub Releases daily, then downloads, verifies and installs updates in the app (you confirm each install)
- Profile photo, animated splash, branded title bars, and FDV, Light and Dark themes
- Layouts for phones, foldables (including cover screens) and tablets
---

## 📱 Requirements

- **Android 12 (API 31) or newer**
- Designed for tablets and fully usable on phones and foldables
- Internet only for FDV features, SimBrief import and update checks
---

## 📥 Installing

1. Download `fdv-flight-logger-<version>.apk` from the [Releases](../../releases) page.
2. Open it on your device. If prompted, allow your browser or file manager to **install unknown apps**.
3. From then on, the app offers updates itself and installs them when you confirm. Your logbook carries over.
> 💾 Flights are stored only on your device. Use **History → ⋮ → Back Up Logbook** regularly, and always before uninstalling or switching devices.

Help is built into the app: open **Settings** and tap **?**.
 
---

## 🔒 Privacy

The app has **no analytics, ads, crash reporting or tracking**, and no server of its own. Your logbook stays on your device unless you export or back it up yourself.

It talks to exactly these services, and only for the features listed:

| Service | When | What's sent |
|---|---|---|
| `flydeltavirtual.com` | Signed in to FDV | Your FDV sign-in token. It reads your profile, bookings, PIREPs, awards, events, bonuses, fleet and gates. Read-only: the app never files or changes anything on FDV. |
| `simbrief.com` | You tap **Import** | Your SimBrief / Navigraph alias |
| `api.github.com`, `github.com` | Once a day, and when you download an update | Nothing about you: a request for the latest release |

- Your FDV sign-in token is encrypted with a key held in the Android Keystore, is excluded from device backups, and is deleted when you sign out. Tokens expire after 30 days.
- Sign-in uses PKCE in the system browser, so your FDV password never passes through the app.
- The token is only ever sent to `flydeltavirtual.com`, over HTTPS.
- Updates are only downloaded from this repository's releases, and only installed if their size and SHA-256 match what GitHub published. Android also refuses any update that isn't signed with the same key as the installed app.
  To report a security problem, see [SECURITY.md](SECURITY.md).

---

## 🧰 Tech Stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 (custom Canvas gauges and animations) |
| Architecture | MVVM (`AndroidViewModel` + `StateFlow`) |
| Persistence | Room (with migrations) + DataStore |
| Networking | Ktor client (OkHttp engine) + kotlinx.serialization |
| Images | Coil 3 |
| Background work | WorkManager |
| Sign-in | Custom Tabs + PKCE; token sealed with AES-GCM in the Android Keystore |
| Navigation | Navigation Compose |
| Help | Markdown in the APK, with a small built-in renderer and search |
| Build | Gradle (Kotlin DSL, version catalog) + KSP |
| Tests | JUnit (JVM unit tests) |
| Min / target SDK | 31 / 37 |
 
---

## 🛠️ Building from Source

### Prerequisites
- A recent stable Android Studio (the bundled JDK is fine)
- Android SDK 37
### Debug build
```bash
git clone https://github.com/Shadehawke/fdv-flight-logger.git
```
Open the project in Android Studio and run the `app` configuration.

> Debug and release builds share one application ID, so a debug build replaces an installed release (Android will ask you to uninstall first, because the signatures differ). Back up your logbook before switching.

FDV sign-in works in debug builds. Update checks are switched off in debug builds.

### Running tests
```bash
./gradlew testDebugUnitTest
```
The unit tests run on the JVM without a device. They cover:

- FDV API parsing, using real response shapes
- PIREP matching, history import and review status sync
- FDV event and bonus rules (matching and timing) and the aircraft picker
- Dashboard and landing statistics, and History filters
- SimBrief parsing
- Sign-in (PKCE, callbacks, sessions) and update checks
- The bundled help files (every file must parse)
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
The signed APK is written to `app/build/outputs/apk/release/`. Attach it to the GitHub release as `fdv-flight-logger-<version>.apk`, with a `v<version>` tag, so the in-app updater finds it. Don't mark it as a pre-release: the updater only sees the latest full release.

> `keystore.properties` and `*.jks` are git-ignored. **Never commit them,** and back up your keystore: updates can't be published without it.

### Editing the help
The in-app help is one Markdown file per section in `app/src/main/assets/help/`, read in file-name order (`01-…`, `02-…`). Each `#` heading is a section, each `##` heading a topic, and a `<!-- keywords: … -->` line under a topic adds search words. Topics support `###` headings, paragraphs, `-` bullets, `1.` steps, `>` tips, **bold** and `code`.

### Regenerating airport and runway data
`app/src/main/assets/airports.json` is generated from [OurAirports](https://ourairports.com/data/) data:

1. Download `airports.csv` and `runways.csv` from OurAirports into `tools/raw/`.
2. Run `python tools/prepare_airports.py`.
3. Rebuild the app.
---

## 🗂️ Project Structure

```
app/src/main/
├── assets/
│   ├── airports.json             # Trimmed airport, runway and coordinate data
│   └── help/                     # Built-in help, one Markdown file per section
└── java/com/fdv/fdvflightlogger/
    ├── data/
    │   ├── Airline.kt            # Airline list + DAL flight-number formatting
    │   ├── aircraft/             # Aircraft types, flap settings, fleet-aware suggestions
    │   ├── airports/             # Airport repository (search, runways, distance)
    │   ├── auth/                 # FDV sign-in: PKCE, callback checks, encrypted session
    │   ├── backup/               # Versioned JSON backup format
    │   ├── db/                   # Room entities, DAO, migrations, PIREP status
    │   ├── fdv/                  # FDV API client, models, event and bonus rules
    │   ├── help/                 # Help parser and search
    │   ├── history/              # Learned suggestions and History filters
    │   ├── network/              # Shared HTTP client and JSON settings
    │   ├── prefs/                # DataStore profile + settings
    │   ├── profile/              # Profile photo storage
    │   ├── simbrief/             # SimBrief client, parser and plan model
    │   ├── stats/                # Dashboard and landing statistics
    │   ├── sync/                 # PIREP matching, history import, status sync, air-time worker
    │   └── update/               # GitHub release check, download, verify, install
    ├── export/                   # CSV and PDF export
    └── ui/
        ├── components/           # Gauges, top bar, pilot card, events, landings, badges, help text
        ├── mappers/              # Entity/draft and SimBrief mapping
        ├── nav/                  # Routes and nav graph
        ├── screens/              # Dashboard, Log, History (+ filters), Detail, Setup, Settings,
        │                         # Trophy Case, Events, Help, Splash
        └── theme/                # Colors, typography, raised/inset surfaces
tools/
└── prepare_airports.py           # Builds airports.json from OurAirports CSVs
```
 
---

## 🗺️ Roadmap

Done:
- [x] Logging, history, search, edit, CSV/PDF export, backup and restore
- [x] Airport, runway and aircraft data; learned suggestions
- [x] Statistics dashboard
- [x] SimBrief import
- [x] FDV sign-in, bookings, PIREP sync and history import
- [x] In-app updates
- [x] Rank progress, Trophy Case, Events, gate assignment, PIREP review status, fleet-aware aircraft picker
- [x] Built-in, searchable help
- [x] FDV bonuses, CAT Free and double-hours rules, rank waiver passes
- [x] Landing stats and ratings
- [x] History filters (date, airline, aircraft, type, FDV status)
  Next:
- [ ] Route map
---

## 🙏 Data Sources

- Airport, runway and coordinate data from [OurAirports](https://ourairports.com/data/), released into the public domain.
- Flight plan import uses the [SimBrief](https://www.simbrief.com/) API.
- FDV data comes from FlyDelta Virtual's pilot API.
- Landing rating bands follow common airline and simulator guidance, including [SKYbrary's hard-landing article](https://skybrary.aero/articles/hard-landing).
---

## ⚠️ Disclaimer

This project is not affiliated with or endorsed by Delta Air Lines, SimBrief, Navigraph, or any airline named in the app. All trademarks belong to their respective owners.

For flight simulation use only. Not for real-world navigation or flight planning.
 
---

## 📄 License

Licensed under the MIT License. See [`LICENSE`](LICENSE) for details.