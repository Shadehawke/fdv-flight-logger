# Troubleshooting

What to do when something doesn't work.

## Reporting a problem or suggesting an idea
<!-- keywords: feedback, bug, report, suggestion, idea, contact, support, form, screenshot -->
In **Settings** (or at the bottom of Help), tap **Report a problem** or **Suggest an idea**. A short form opens in your browser with your app version and device model already filled in, so we know what you're using. Nothing is sent until you submit it, and you don't need to sign in.

For a problem, say what you were doing, what happened, and what you expected. If a screenshot would help, paste a link to it (for example from Discord or Google Drive), and leave your Discord name or email if you'd like a reply.

## Sign-in problems
<!-- keywords: sign in failed, login failed, expired, didn't match, no account, vatsim mismatch -->
- **"That sign-in has expired"** or **"didn't match this request":** the sign-in took too long, or the app restarted part-way. Tap **Sign in with FDV** again.
- **"No FDV account is linked to that login":** sign in with the email or VATSIM account your FDV profile uses.
- **"That VATSIM account isn't the one linked to your FDV profile":** sign in with the VATSIM account linked on FDV, or use your email and password.
- **Signed out unexpectedly:** sign-ins last 30 days. Sign in again.

## FDV data won't load
<!-- keywords: not loading, error, rate limit, too many requests, offline, couldn't load -->
- Check your internet connection, then tap refresh or try again.
- FDV allows a limited number of requests per minute. If the app says to wait, it retries on its own.
- If one screen keeps failing, sign out and back in.

## SimBrief import fails
<!-- keywords: simbrief error, import failed, alias wrong, no plan -->
- Check your alias in **Settings**. It's the one in SimBrief under Account Settings.
- Make sure you've generated a plan on SimBrief recently.
- Check your internet connection.

## An update won't install
<!-- keywords: update failed, install failed, signature, conflict, unknown apps, permission -->
- **The download failed or didn't match:** try again on a stable connection. The app won't install a download that doesn't match the published update.
- **Android asks for permission:** allow FDV Flight Logger to install unknown apps, then tap Update again.
- **"App not installed" or a conflict:** you have a test build signed differently. Back up your logbook, uninstall the app, install the new version, then restore.

## My stats don't match FDV
<!-- keywords: hours different, flights different, stats wrong, block hours -->
- The dashboard leaves out pending and rejected flights, like FDV. Check the note under the gauges.
- FDV Hours include bonus and transfer hours; the Block Hours gauge is your logbook's block time.
- Flights you logged without filing on FDV count in the app but not on FDV.
- Run **Sync new flights** in **Settings** to bring in recent PIREPs.

## Air times are missing
<!-- keywords: air time empty, missing, not filled -->
Air times are filled in in the background after an import. In **Settings**, tap **Fill in missing air times** to start it again.

If some flights stay empty after it finishes, their PIREPs have no takeoff or touchdown in the Black Box log, so FDV has no air time to give. Enter those by hand; the app won't overwrite them.

## Privacy and what the app sends
<!-- keywords: privacy, data, security, tracking, analytics, what is sent -->
The app has no analytics, ads or tracking, and no server of its own. It only contacts:

- **FlyDelta Virtual**, while you're signed in, to read your FDV data
- **SimBrief**, when you tap Import, with your alias
- **GitHub**, once a day, to check for updates

Your FDV sign-in is stored encrypted and is never included in backups.

## "Scan app" or "may be harmful" when installing
<!-- keywords: play protect, harmful, scan, unsafe, warning, blocked -->
Android suggests scanning apps that come from outside the Play Store, and may warn about apps it hasn't seen many times before. FDV Flight Logger is open source: its code is public on GitHub, and it only installs its own updates from its own GitHub releases.

If Android blocks the install, choose **More details** and then **Install anyway**.
