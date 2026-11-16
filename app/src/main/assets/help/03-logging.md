# Logging a flight

Everything about creating, filling in and saving a logbook entry.

## Starting a new flight
<!-- keywords: new flight, create, log, start -->
On the dashboard, tap **Start new flight**. Fill in the departure and arrival airports, airline, flight number and aircraft, then work through the sections as you fly.

You only need the basics to save. Every other field is optional.

## Starting from your FDV booking
<!-- keywords: booking, bid, schedule, charter, load booking, start from booking -->
If you've booked a flight on FDV, the app can fill in the entry for you.

1. In a new flight, tap the **take-off icon** in the title bar (Start from my FDV booking).
2. If you have more than one booking, choose one.
3. Confirm with **Load booking**.

The airports, airline, flight number, aircraft and times are filled in. If you generated a SimBrief plan from the booking on FDV, its route, fuel, weights, runways and gates are filled in too.

## Importing from SimBrief
<!-- keywords: simbrief, navigraph, ofp, flight plan, import, alias -->
The app can fill in a flight from your latest SimBrief plan.

1. Enter your SimBrief / Navigraph alias once, in **Settings**. It's in SimBrief under Account Settings.
2. In a flight, tap the **download icon** in the title bar (Import from SimBrief) and confirm.

Airports, runways, alternate, SID and STAR, route, cruise level, cost index, airline, flight number, aircraft, fuel, weights, times, cruise wind and temperature, flaps, V-speeds and QNH are imported and converted to your units.

Planned values replace what's in those fields. Your gates, ATC details, scratchpad and flight type are kept.

> The app only contacts SimBrief when you tap Import.

## The four sections
<!-- keywords: sections, departure, arrival, aircraft, performance, atc, tabs -->
A flight is split into four sections. Use the chips at the top to jump between them:

- **Departure + Enroute:** runway, gate, SID, cruise level, take-off flaps, V2, departure QNH and route
- **Arrival:** runway, gate, STAR, alternate, QNH, landing flaps and Vref
- **Aircraft + Performance:** fuel, passengers, payload, block and air time, cost index, reserve fuel, ZFW, fuel used, landing rate, cruise wind and temperature
- **ATC:** ATIS information, initial altitude and squawk

On a tablet, the sections are laid out side by side.

## Flight types
<!-- keywords: online, offline, offline with atc, offline no atc, flight type -->
Choose the type that matches how you flew:

- **Online:** on a network such as VATSIM
- **Offline with ATC:** offline, with simulated ATC
- **Offline No ATC:** offline, without ATC. The ATC section is hidden.

## Getting a gate from FDV
<!-- keywords: gate, assign gate, parking, stand, terminal -->
For a flight without a booking, FDV can pick a gate for you, the same way it does for bookings.

1. Enter the airport, and ideally the airline and aircraft.
2. Under the Gate field, tap **Get FDV gate at** (airport).

The gate is filled in and the terminal is shown. FDV picks a random suitable gate each time, so the chip only appears while the Gate field is empty. If FDV has no gates for that airport, the app tells you.

## Choosing an aircraft
<!-- keywords: aircraft, type, fleet, can fly, rank, lock, padlock, icao -->
Start typing an aircraft code or name, and the app suggests matching types. When you're signed in to FDV, the suggestions also show FDV's fleet:

- **A tick:** in FDV's fleet, and your rank can fly it
- **A padlock and a rank:** in FDV's fleet, but normally above your rank
- **Dimmed:** not in FDV's fleet

Types you can fly are listed first. The line under the field repeats what FDV says about the type you picked.

You can still type any aircraft. The marks are information, not limits.

## Event flights: CAT Free and double hours
<!-- keywords: cat free, category free, double hours, bonus, event flight, group flight, fly in, fly out, rank waived -->
On FDV, a flight that matches a **Group Flight**, **Fly In**, **Fly Out** or **In or Out** event, flown inside the event's time window, is CAT Free and earns double hours. CAT Free means the aircraft rank limits don't apply.

When the flight you're logging matches such an event, live or starting within two hours, a banner at the top of the flight says so. While it applies, every fleet type shows as flyable, marked "rank waived".

The app matches events by airport:

- **Group Flight:** your departure and arrival match the event's
- **Fly In:** your arrival matches the event's
- **Fly Out:** your departure matches the event's
- **In or Out:** either airport matches

This only applies to a new flight being logged now. Other events, and FDV bonuses such as Flight of the Day, aren't detected yet.

## The scratchpad
<!-- keywords: notes, scratchpad, taxi, clearance -->
The scratchpad is always on screen. Use it for taxi instructions, clearances or anything else you want to note during the flight. It's saved with the flight and included in exports.

## Saving and auto-save
<!-- keywords: save, autosave, auto save, discard, unsaved changes, lost -->
Tap **Save Flight** (or **Update Flight** when editing) when you're done.

While you're logging, the app also saves about every 60 seconds and when you leave the screen, so a flight isn't lost if the app closes. Auto-save updates the same entry rather than creating copies.

If you try to leave with unsaved changes, the app asks whether to keep editing or discard them.

## Typing times, wind and QNH
<!-- keywords: format, time format, wind format, qnh format, 0345, 29045 -->
To keep typing fast, some fields take digits only:

- **Times:** type `0345`, shown as 03:45
- **Cruise wind:** type `29045`, shown as 290/45
- **QNH:** shown as 29.92 (inHg) or 1013 (hPa), following your units

Procedures, gates, alternates and routes are capitalised for you.

## Suggestions as you type
<!-- keywords: autocomplete, suggestions, airport search, runway, sid, star, route, remembered -->
Many fields suggest values:

- **Airports:** search about 5,900 airports by ICAO code, name or city
- **Runways:** pick from the runways at the airport you entered
- **Flaps:** the settings that match your aircraft
- **Gate, SID and STAR:** values you've used before at that airport
- **Route:** routes you've flown before between the same airports

Every field still accepts anything you type.
