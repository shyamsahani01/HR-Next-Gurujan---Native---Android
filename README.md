# HR Next Gurujan

A native Android client for **Frappe HR / ERPNext HRMS**, built with Kotlin and Jetpack Compose. It talks directly to your Frappe site's REST API — no custom backend required — and gives employees a fast, mobile-first way to check in/out, track attendance, apply for and approve leave, and browse the rest of their HR data on the go.

## Features

- **Login** — connect to any self-hosted, Frappe Cloud, or local ERPNext/Frappe site with your existing username and password (cookie-based session).
- **Home** — a personalized daily dashboard for the logged-in employee:
  - Check in / check out, with a live "checked in since …" status
  - While checked in, a foreground service pings the device's location every 2 minutes (creates `Employee Checkin` records) until checkout
  - This month's attendance snapshot and current leave balance
  - Team widgets: birthdays this month, work anniversaries this month, who's on leave today
- **Attendance** — a full month calendar view per employee, color-coded by status, showing check-in/check-out times on Present/Half Day cells, with a tap-through detail dialog and month-to-month paging.
- **Leave** — the employee's own leave applications, a per-leave-type balance breakdown (allocated / used / remaining, this month and overall), and a "pending approval" strip covering both the employee's own pending requests and anyone else's awaiting their approval.
- **Payroll** — the employee's own Salary Slips.
- **More** — a generic, permission-aware browser for every other HR/Payroll doctype exposed by the site's Workspace configuration (Expense Claim, Employee Advance, Job Openings, Appraisals, etc.), with a "my records / everyone's records" toggle on doctypes like Expense Claim and Employee Advance.
- **Profile** — employee details pulled from the linked `Employee` record (designation, department, employee ID, joining date, contact info), plus theme (system/light/dark) and logout.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for UI
- **Coroutines / Flow** for async work
- **Retrofit + OkHttp + Gson** for the Frappe REST API (`/api/method/*`, `/api/resource/*`)
- **DataStore Preferences** for session and app-preference persistence
- **Coil 3** for image loading
- **Navigation Compose** for in-app navigation
- Manual, hand-rolled DI (`AppContainer` / `AppViewModelFactory`) — no Hilt/Koin
- A foreground `Service` (`CheckinLocationService`) for background location pings while checked in

No fixed data class exists per Frappe doctype — most doc payloads are passed around as raw `JsonObject` and rendered generically (`DocTypeMeta` / `DocField`), so the same list/detail screens work across any doctype the connected site exposes.

## Project structure

```
app/src/main/java/com/example/hrnext/
├── data/          # Repositories — thin wrappers over the generic Frappe REST API
├── di/            # AppContainer — manual dependency container
├── location/      # One-shot device location fetch (plain LocationManager, no Play Services)
├── model/         # Small data classes shared across screens
├── network/       # Retrofit setup, cookie jar, session persistence
├── service/       # CheckinLocationService — the background check-in ping
├── ui/
│   ├── components/    # Shared composables (icons, formatters, animations)
│   ├── navigation/    # NavHost + routes
│   ├── screens/       # One package per screen (home, attendance, leave, profile, …)
│   └── theme/         # Color, typography, accent palette
└── util/          # Small Gson/JsonObject extension helpers
```

## Requirements

- Android Studio (latest stable)
- JDK 17+ (the project also works with the JBR bundled in Android Studio)
- An accessible Frappe/ERPNext site with the HR module installed, and a user account on it

## Building

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch, enter your site URL, username, and password on the login screen.

## Permissions

The app requests, at check-in time:

- **Location** (fine + background) — to attach coordinates to `Employee Checkin` records and to keep sending updates every 2 minutes while checked in
- **Notifications** (Android 13+) — for the persistent "checked in" foreground-service notification

Denying background location does not block check-in; only fine location is required for the check-in action itself.
