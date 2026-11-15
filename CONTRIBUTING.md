# Contributing to FDV Flight Logger

Thanks for your interest in improving FDV Flight Logger. This project values clarity, maintainability and pragmatic design over cleverness.

## 🧭 Project philosophy

- Tablet-first, phone- and foldable-friendly
- Function over flash, then polish
- Explicit UI over hidden behaviour
- Readable code over clever code
- Works offline; the network is only for FDV, SimBrief and updates
## 🛠️ Development setup

1. Install a recent stable Android Studio (the bundled JDK is fine) and Android SDK 37.
2. Clone the repository and open it in Android Studio.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.
   Debug and release builds share an application ID, so back up your logbook before replacing a release build with a debug one. Release signing needs a local `keystore.properties` (see the README). You don't need one for debug builds.

## 🧩 Code guidelines

- Kotlin only; Jetpack Compose and Material 3 for UI (no XML layouts)
- MVVM: screens read `StateFlow`s from the ViewModel and send events back. Keep business logic out of composables.
- Prefer immutable `data class`es and pure functions. Decisions such as parsing, matching and rules belong in plain functions that unit tests can call.
- Keep composables small, stateless where possible, and previewable
- Comments explain **why**, not what
- FDV API DTOs stay `internal`; the rest of the app uses the domain models
- Database changes need a Room migration and a version bump. Commit the generated schema JSON under `app/schemas/`.
## 🔒 Security rules

These aren't optional:

- **Never commit secrets:** `keystore.properties`, `*.jks`, tokens, API keys, or logs that contain them. Check `git status` before every commit.
- **Never log** FDV tokens, sign-in codes, PKCE verifiers or full API responses. Remove temporary diagnostic logging before committing.
- The FDV token is only ever sent to `flydeltavirtual.com` over HTTPS. Don't add other places that send it.
- Don't add analytics, tracking, crash reporting or new network destinations without discussing it first. The README's Privacy section lists every service the app talks to, so update it if that list changes.
- New exported components, intent filters or permissions need a clear reason in the PR.
  See [SECURITY.md](SECURITY.md) for how to report a vulnerability.

## 🧪 Testing

- Run `./gradlew testDebugUnitTest` before opening a PR. All tests must pass.
- Add or update unit tests for any parsing, matching, rules or statistics change. Base API tests on real response shapes.
- Test UI changes on a phone-sized screen and on a tablet (or emulators of both), in all three themes.
## 🔀 Branching

- `main` is stable and releasable
- Features: `feature/<short-description>`
- Fixes: `fix/<short-description>`
## 📝 Commit messages

A short summary line in the imperative ("Add trophy case", "Fix gate chip on narrow screens"), then a bullet list of what changed and why when it isn't obvious.

## 📬 Pull requests

- Keep each PR focused on one change
- Describe **why**, not just what
- Include screenshots for UI changes (phone and tablet if the layout differs)
- Note any database migration, new permission or new network call
  Thanks for helping improve the project.