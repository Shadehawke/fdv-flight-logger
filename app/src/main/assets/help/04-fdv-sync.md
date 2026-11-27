# Syncing with FDV

Bring your FDV PIREPs into your logbook and keep them up to date.

## Syncing one flight's actuals
<!-- keywords: sync actuals, pirep, actuals, block time, air time, fuel used, landing rate, passengers -->
After you've filed a flight on FDV, the app can copy its actual figures onto your logbook entry.

1. Open the flight in **Flight History**.
2. Tap the **cloud icon** in the title bar (Sync actuals from FDV).
3. Confirm the PIREP the app found.

Block time, air time, fuel used, landing rate and passengers are filled in from FDV, and the flight is linked to its PIREP.

## Importing your FDV history
<!-- keywords: import history, import fdv history, all flights, only my flights, pireps, past flights, sign in -->
When you first sign in to FDV, the app offers to do this for you. You can also start it any time: in **Settings**, under Logbook sync, tap **Import FDV history**. The app reads all your PIREPs and shows what it found:

- PIREPs that match flights already in your logbook get their actuals
- PIREPs that are already synced are skipped
- PIREPs with no matching flight can be added or left out

Choose **Import all** to add every unmatched PIREP as a new flight, or **Only my flights** to fill in your own flights only. New PIREPs follow the same choice when you sync later.

## Syncing new flights
<!-- keywords: sync new flights, update, refresh, new pireps, automatic, auto sync -->
After the history import, new PIREPs come in on their own: when you open the dashboard, the app checks FDV for flights filed since the last sync (at most every 30 minutes) and lets you know if it found any.

To check right away, tap **Sync new flights** in **Settings**. Either way, the sync also updates the review status of pending flights.

## Background air times
<!-- keywords: air time, missing air times, background, fill in -->
FDV only provides a flight's air time on the flight itself, so the app fills these in in the background after an import, a few flights at a time.

In **Settings**, **Fill in missing air times** starts it again, and **Stop** pauses it.

Some older PIREPs have no takeoff or touchdown in their Black Box log, so FDV has no air time for them. The app asks about each flight once and then leaves it alone; you can enter those air times yourself.

## Pending and rejected flights
<!-- keywords: pending, rejected, approved, review, status, badge -->
Flights linked to a PIREP show their FDV review status:

- In **Flight History**, pending flights have an orange **Pending** badge and rejected flights a red **Rejected** badge
- On a flight's detail page, the PIREP number has a green, orange or red badge

The app checks pending flights when you open the dashboard (at most every 30 minutes) and when you sync new flights. Pending and rejected flights aren't counted in the dashboard, like on FDV.

Search History for "pending" or "rejected" to list them.

## Removing imported flights
<!-- keywords: remove imported, delete imported, undo import, start over -->
In **Settings**, **Remove imported** deletes every flight the FDV import created. Flights you logged yourself stay, including the actuals synced onto them. You can import again afterwards.

## Signing out
<!-- keywords: sign out, log out, logout, switch account -->
In **Settings**, tap **Sign out** on the account card. The app forgets your sign-in token and FDV data such as awards and events. Your logbook stays.
