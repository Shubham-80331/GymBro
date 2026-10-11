# GymBro 🏋️

GymBro is an Android workout logger, nutrition tracker and AI coach for people who lift. It tracks every set, suggests when to add weight, logs your food (including Indian staples), and keeps your progress in one place.

App ID: `com.gymbro.fitness`

## Features

**Training**
- Routines you build yourself, plus one-tap programs: Full Body Beginner, 5×5 Strength, Upper/Lower, Bro Split
- Library of 90+ exercises with search and muscle-group filters, plus custom exercises
- Workout logger with a "previous" column, progressive-overload suggestions and plateau/deload detection
- Rest timer that keeps running in the background, with a countdown notification and a vibration when rest ends
- Plate calculator (kg or lb plates), kg/lb units throughout
- In-progress workouts survive the app being closed; post-workout summary with new PRs
- Workout history: edit or delete past sets and sessions

**Nutrition**
- Daily calorie and macro targets from your profile (protein set in g/kg of body weight)
- Food search through [Open Food Facts](https://world.openfoodfacts.org) with gram portions
- 45 built-in common foods (roti, dal, paneer, rice, idli, dosa…) that also work offline
- Recent and saved foods for one-tap logging, water tracking

**Progress**
- Weight and body measurement log with a trend chart
- Best estimated 1RM for each lift, with a trend chart per lift
- Weekly sets per muscle group against a 10–20 sets/week guide
- Workout streak, steps and calories burned

**AI coach (Gemini)**
- Chat that uses your profile, recent workouts and weight trend; history is saved
- Describe a session and the coach builds a routine you can save in one tap

**Everything else**
- Daily workout reminder, home-screen widget (streak plus calories and protein left)
- Settings: units, edit profile and goals, export all data as JSON, delete all data

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Room (SQLite) with exported schemas and migrations
- Retrofit + OkHttp + Moshi (codegen) for Gemini and Open Food Facts
- Coroutines / Flow, AndroidX Lifecycle ViewModel
- JUnit + Robolectric + Roborazzi for tests
- GitHub Actions CI: debug build, unit tests, R8 release shrink, lint

## Project structure

```text
app/src/main/java/com/gymbro/fitness/
├── data/            # Room entities, DAOs, database, repository, Gemini + food search clients
├── domain/          # Pure Kotlin logic: FitnessCalculator, WeightUnit, programs, common foods
├── notifications/   # Rest timer and daily reminder
├── widget/          # Home-screen widget
└── ui/              # GymBroApp, ViewModel, screens (Coach, Workouts, Diet, Progress, Settings)
docs/ROADMAP.md      # Phased plan with task status
```

## Building

1. Install Android Studio with JDK 21 and the Android SDK.
2. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` if you want the AI coach. Without it the app works, but the coach explains that it isn't set up yet.
3. `./gradlew assembleDebug` builds the app. `./gradlew testDebugUnitTest` runs the unit tests.

Release builds use R8 and expect an upload key via the `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` environment variables.

> **Before publishing:** the Gemini key is currently compiled into the app. Move AI calls behind a backend or Firebase AI Logic with App Check first (roadmap item P-01).

## Roadmap

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for what's done and what's next.
