# GymBro Roadmap

Plan for getting GymBro from its AI Studio prototype to a Play Store release that regular gym-goers keep using.
Work top to bottom: each phase must be done before the next one starts, and tickets inside a phase can run in parallel unless they list a dependency.

- **App ID / namespace:** `com.gymbro.fitness` (permanent once uploaded to Play)
- **Size:** S ≈ ≤1 day, M ≈ 2–4 days, L ≈ 1–2 weeks
- **Status:** ☐ todo · ◐ in progress · ☑ done (Phase 1 rows show ☑ before the task when done)

---

## Phase 0 — Foundation (week 1)

Goal: a clean, buildable, testable project to build on.

| ID | Task | Size | Status |
|---|---|---|---|
| F-01 | Change app ID to `com.gymbro.fitness`, move the `com.example` package, rename the project and theme | S | ☑ |
| F-02 | Update the Compose BOM, lifecycle, activity, and OkHttp; remove unused deps (Firebase BOM, commented-out libs) | S | ☐ |
| F-03 | Turn on Room `exportSchema`, commit `app/schemas/`, add migration tests (also covers the missing 1→2 path). Export is on; commit the generated `app/schemas/` from the first build, then add tests | M | ◐ |
| F-04 | Set up CI on GitHub Actions: `lint`, `testDebugUnitTest`, `assembleDebug` on every PR (lint is non-blocking until the backlog is cleared) | S | ◐ |
| F-05 | Add Hilt for dependency injection; the ViewModel currently builds its own repository and database | M | ☐ |
| F-06 | Split the 800-line `GymBroViewModel` into Onboarding, Workout, Nutrition, Progress, and Coach ViewModels | M | ☐ |
| F-07 | Add Navigation Compose (tabs plus detail screens, back stack, deep links) | M | ☐ |
| F-08 | Add DataStore for settings (units, theme, reminders) | S | ☐ |
| F-09 | Replace `SimpleDateFormat(Locale.getDefault())` with `java.time.LocalDate` keys (fixes locales that use non-Latin digits). Date keys now use `Locale.US`; full `java.time` move still to do | S | ◐ |
| F-10 | Rewrite the README: real tech stack (Compose, Room), setup, `.env` keys, how to build | S | ☐ |

**Done when:** CI is green, the schema is exported, and there are no `com.example` or AI Studio leftovers.

---

## Phase 1 — Trust & correctness (weeks 2–3)

Goal: every number on screen is real and no user data is lost.

### 1A. Remove fake data
| ID | Task | Size |
|---|---|---|
| T-01 | ☑ Streak badge: replace `12 + sessions.size` with the real `workoutStreak` (`GymBroApp.kt`) | S |
| T-02 | ☑ Volume PR card: drop the `12400.0` fallback; show "NEW RECORD" only when the latest session is the max; add an empty state | S |
| T-03 | ☑ Steps tile shows calories labelled as steps; show the step count and calories separately | S |
| T-04 | ☑ Replace the canned "Correlation Insights" text with values computed from the user's data, or hide the card | S |
| T-05 | ☑ Remove the "LIVE" and "GYMBRO PRO" labels and the empty avatar until those features exist | S |

### 1B. Data integrity bugs
| ID | Task | Size |
|---|---|---|
| D-01 | ☑ `logMorningWeightAndMetrics` logs weight twice; separate "update profile" from "log weight" | S |
| D-02 | ☑ Replace the hand-written routine JSON parser with Moshi, or better, a `routine_exercises` table (migration) | M |
| D-03 | ☑ Give custom exercises a UUID ID so they can't REPLACE built-in ones; block duplicate names | S |
| D-04 | ☑ Save the active workout to Room on every change and resume it after process death or app restart | M |
| D-05 | ☑ PR detection: compare estimated 1RM (Epley), count a first lift as a baseline, persist `isPr`, include sets from the current session | M |
| D-06 | ☑ Confirm before deleting a routine or log entry, with an undo snackbar | S |
| D-07 | ☑ Handle the system back button during a workout ("Discard / Finish / Keep going") | S |
| D-08 | ☑ Input validation everywhere: ranges for age, weight, height, reps, and macros; no silent defaults | S |
| D-09 | ◐ Allow editing and deleting past workout sessions and their sets | M |

### 1C. Rest timer that works in the background
| ID | Task | Size |
|---|---|---|
| R-01 | ◐ Rest timer outside the app: countdown notification + alarm-backed "rest over" alert (done); +15s / skip actions still to do | M |
| R-02 | ☑ Vibration and sound when rest ends; request `POST_NOTIFICATIONS` on Android 13+ | S |

### 1D. Correct calculations
| ID | Task | Size |
|---|---|---|
| C-01 | ☑ Protein in g/kg of body weight (1.6–2.2 depending on goal), fats ≥ 0.6 g/kg, carbs fill the rest | S |
| C-02 | ☑ "Other" gender: average the male and female BMR formulas, or let the user choose a formula | S |
| C-03 | ☑ MET-based workout calories (time × MET × body weight) instead of `volume*0.03` | S |
| C-04 | ☑ Unit tests for BMR, TDEE, macros, streak, 1RM, and PR logic | M |

**Done when:** a fresh install shows only zeros and empty states, and a workout survives the app being killed mid-set.

---

## Phase 2 — Play Store readiness (week 4) → Closed testing

| ID | Task | Size |
|---|---|---|
| P-01 | **Move the Gemini key off the device**: Firebase AI Logic with App Check, or a Cloud Function proxy; per-user rate limit | M |
| P-02 | ☑ Friendly AI error and retry states; never show raw exception text | S |
| P-03 | ◐ (R8 + resource shrinking on, CI runs minifyReleaseWithR8; still needs an on-device release smoke test) R8 on (`isMinifyEnabled = true`, `isShrinkResources = true`), keep rules for Moshi and Room; test the release build | S |
| P-04 | Upload keystore, Play App Signing, AAB build, `versionCode` from CI | S |
| P-05 | ◐ Privacy policy (hosted), health and AI disclaimer at first launch and on the Coach screen (disclaimers in Coach header and Settings done; hosted policy still needed) | S |
| P-06 | ☑ Settings → "Export my data" (JSON; CSV later) and "Delete all my data" | M |
| P-07 | ☑ Configure `backup_rules.xml` and `data_extraction_rules.xml` (back up the DB, exclude photos and caches) | S |
| P-08 | Crashlytics plus privacy-friendly analytics, behind a consent toggle | S |
| P-09 | Real launcher icon, splash screen (`core-splashscreen`), light theme option | M |
| P-10 | Accessibility pass: content descriptions, 48dp touch targets, font scaling, contrast | M |
| P-11 | Move all UI strings into `strings.xml` (ready for translation, e.g. Hindi) | M |
| P-12 | Store listing: screenshots, feature graphic, short and full description, Data Safety form, content rating | S |
| P-13 | Internal test track → closed test (≥12 testers × 14 days, required for new personal accounts) | — |

**Done when:** the release AAB is signed and minified, the policy forms are submitted, and closed testing is running.

---

## Phase 3 — The core loop lifters care about (weeks 5–10) → Production v1.0

### 3A. Pro-level workout logger
| ID | Feature | Size |
|---|---|---|
| W-01 | ☑ kg/lb unit toggle (store kg, convert in the UI) | S |
| W-02 | ☑ "Previous" column per set (last weight × reps), prefill from the last session | S |
| W-03 | Set types: warm-up, working, drop, failure; optional RPE/RIR | M |
| W-04 | Add, swap, or reorder exercises mid-workout; supersets | M |
| W-05 | ☑ Plate calculator (bar weight setting, available plates) | S |
| W-06 | Notes per exercise and per session | S |
| W-07 | ☑ Workout summary screen: volume, PRs hit, duration, muscles worked | M |

### 3B. Exercise library
| ID | Feature | Size |
|---|---|---|
| E-01 | ◐ (92 built-in exercises incl. Full Body; images/cues still to do) Expand from 24 to 300+ exercises (seed from an open dataset), with primary and secondary muscles and equipment | L |
| E-02 | Exercise detail page: animation or illustration, form cues, history, 1RM chart | M |
| E-03 | ☑ Search and filter by muscle and equipment | S |

### 3C. Progression intelligence
| ID | Feature | Size |
|---|---|---|
| G-01 | ☑ Automatic progressive overload suggestion ("hit 3×10 → try 62.5 kg") | M |
| G-02 | ◐ (best est. 1RM per lift on Progress done; trend charts next) Estimated 1RM trend charts per lift; PR history | M |
| G-03 | ◐ Weekly sets per muscle group plus a body heat-map of under-trained muscles (weekly sets bars done; body heat-map next) | M |
| G-04 | Plateau detection → deload suggestion | S |

### 3D. Programs
| ID | Feature | Size |
|---|---|---|
| PR-01 | ☑ Built-in templates: Full-Body beginner, PPL, Upper/Lower, 5/3/1, GZCLP, Bro split | M |
| PR-02 | Weekly schedule, "Today's workout" card on a new Home screen | M |

### 3E. Nutrition that people actually use
| ID | Feature | Size |
|---|---|---|
| N-01 | ◐ Food search (Open Food Facts plus USDA); serving sizes and quantity (Open Food Facts search + grams done; USDA later) | L |
| N-02 | Barcode scanner (CameraX plus ML Kit) | M |
| N-03 | ◐ (saved foods quick-pick done; recents/favourites next) Recent, favourite, and custom foods (custom foods are already in the DB but have no UI) | S |
| N-04 | ◐ (45 built-in common foods incl. Indian staples, offline; expand further later) Indian and regional food database (dal, roti, paneer, poha…) | M |
| N-05 | Copy a meal or day; saved meals and recipes | M |

### 3F. Health Connect & retention
| ID | Feature | Size |
|---|---|---|
| H-01 | Health Connect: read steps, weight, sleep, HR; write workouts (replaces manual steps) | M |
| H-02 | ◐ Reminders: workout days, evening protein gap, water, weekly weigh-in (daily workout reminder done) | M |
| H-03 | ◐ Home-screen widgets: today's workout, macros left, streak (streak + calories/protein left widget done; today's workout next) | M |
| H-04 | Achievements and badges; shareable weekly recap card | M |
| H-05 | Progress photos: private, stored on device, side-by-side compare | M |

**Done when:** a lifter can run a full program for 4 weeks without leaving the app → production release v1.0.

---

## Phase 4 — Differentiators (post-v1.0)

| ID | Feature | Size |
|---|---|---|
| AI-01 | ☑ Coach that knows your data: send recent workouts, PRs, adherence, and weight trend as context; persist chat history | M |
| AI-02 | ☑ Structured output → "Make me a 4-day split" creates a saveable routine in one tap | M |
| AI-03 | AI meal logging: photo or "2 eggs + toast" → macros pre-filled for confirmation | M |
| AI-04 | Weekly AI check-in report (volume trend, protein hit rate, next-week plan) | M |
| AI-05 | Form-cue Q&A per exercise | S |
| S-01 | Account (Firebase Auth) plus cloud backup and sync across devices | L |
| M-01 | GymBro Pro subscription (Play Billing, 7-day trial): AI features, advanced analytics, programs | L |
| X-01 | Wear OS companion: log sets and rest timer from the watch | L |

## Phase 5 — Community

Friends and following, sharing workouts, gym leaderboards, monthly challenges.

---

## Working agreement

- One ticket = one branch/PR, named `<ID>-short-desc` (e.g. `T-01-real-streak`).
- Every PR: CI green, unit tests for logic changes, before/after screenshots for UI changes.
- Any schema change ships with a Room migration and a migration test.
- Never commit secrets: keys live in `.env` (gitignored) or the backend.
