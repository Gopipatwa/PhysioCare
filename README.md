# PhysioCare – home physiotherapy for older adults (Android)

A lightweight Android app with **98 physiotherapy exercises**, a **paralysis & stroke recovery** section, an animated guide for every one, a **camera that counts your repetitions**, a spoken voice coach, timed holds, rest timers, pain tracking and progress history – all stored on the phone, with no account and no internet needed.

The APK is built for you by **GitHub Actions** – you don't need Android Studio.

---

## 1. Build the APK with GitHub (no coding needed)

1. Create a free account at **github.com** and click **New repository**. Give it a name (e.g. `PhysioCare`), choose **Private** or Public, and click **Create repository**.
2. On the new repository page click **“uploading an existing file”**. Unzip `PhysioCare.zip` on your computer and drag **everything inside the `PhysioCare` folder** (including the hidden `.github` folder) into the browser. Click **Commit changes**.
   - On a Mac, press `Cmd + Shift + .` in Finder to show the hidden `.github` folder. On Windows, enable *View → Hidden items*.
   - If the `.github` folder will not upload, create it by hand: **Add file → Create new file**, type the name `.github/workflows/build-apk.yml`, and paste in the contents of that file.
3. Open the **Actions** tab. A run called **Build Android APK** starts by itself (about 6–10 minutes). If you don't see it, click it and press **Run workflow**.
4. When it shows a green tick, open the run and scroll to **Artifacts**. Download **PhysioCare-APK** (a zip containing the `.apk`).

**Optional – make a download page:** in *Code → Tags* create a tag such as `v1.0.0` (or push one with git). The workflow then attaches the APK to a **Release**, which gives you a simple link to send to family members' phones.

## 2. Install on the phone

1. Copy the `.apk` to the phone (WhatsApp/email it to yourself, Google Drive, or USB cable).
2. Tap the file. Android will ask to **allow installs from this source** – allow it once.
3. Tap **Install**, then **Open**. Requires Android 8.0 or newer.

## 3. Keep progress when updating (recommended, one-time setup)

Android only lets a new version replace the old one if both were signed with the **same key**. Without your own key, each build uses a temporary key and you would have to uninstall (losing history) to update.

On any computer with Java installed:

```bash
keytool -genkeypair -v -keystore physiocare.jks -alias physiocare \
        -keyalg RSA -keysize 2048 -validity 10000
# Windows/Linux:
base64 -w 0 physiocare.jks > keystore-base64.txt
# Mac:
base64 -i physiocare.jks -o keystore-base64.txt
```

In GitHub go to **Settings → Secrets and variables → Actions → New repository secret** and add four secrets:

| Secret name        | Value                                          |
|--------------------|------------------------------------------------|
| `KEYSTORE_BASE64`  | the whole text inside `keystore-base64.txt`    |
| `KEYSTORE_PASSWORD`| the store password you typed in `keytool`      |
| `KEY_ALIAS`        | `physiocare`                                   |
| `KEY_PASSWORD`     | the key password (often the same as above)     |

Keep `physiocare.jks` and the passwords somewhere safe. Never upload the `.jks` file itself to the repository.

---

## 4. What the app does

| Area | What's included |
|---|---|
| **Exercise library** | 98 exercises in 12 areas: Neck, Shoulders, Arms/Wrists/Hands, Back & Spine, Core & Posture, Hips, Knees, Ankles & Feet, Balance & Daily Strength, Breathing & Relaxation, Stroke & Paralysis Recovery, Face (Bell's palsy). Each has plain-language steps, tips, who it helps, difficulty and a safety note. Search by body part or problem (“knee”, “grip”, “balance”). |
| **Paralysis & stroke recovery** | Three questions – what happened (stroke, spinal cord injury legs / arms and legs, Bell's palsy, Guillain-Barré or nerve weakness), which side, and how much the person can move (not yet / a little / weak) – then the app shows only matching exercises, plus "when you get stronger" next steps and condition-specific safety advice. Includes carer-assisted exercises written for the helper, mirror therapy, pressure relief, huff cough and face exercises. Camera counting switches to "count small movements" for weak muscles. One tap makes it the daily plan. |
| **Animated guide** | Every exercise has a moving demonstration figure (on a chair, standing or lying, from the side or the front), drawn in code – no images or videos, so the app stays small. |
| **Camera counting** | 17 exercises (sit-to-stand, arm raises, squats, marching, knee straightening, bridges, heel raises and more) are counted automatically using on-device body tracking. A live skeleton shows what the camera sees, with the joint being measured highlighted, plus a progress bar and coaching (“Almost there – a little more”). A **+1** button fixes a missed count. Nothing is recorded or uploaded. |
| **Tap counting** | Exercises the camera can't judge (hands, neck, ankles) use a huge “tap after each one” button – or turn on **Voice counts for me** for a slow “up… and down” rhythm that counts by itself. |
| **Timed holds** | Stretches and balance holds get a large countdown circle, spoken countdown and automatic 5-second relax between holds. Breathing exercises speak “breathe in… breathe out” at a calm pace. |
| **Sets, sides and rest** | Sets with rest timers, left/right side handling, skip rest, pause, and a safe Stop that saves what you did. |
| **My plan** | A daily list started with one button from Home (comes with a gentle starter plan). Add/remove any exercise. Targets (reps, sets, hold seconds) can be changed per exercise to match what the physiotherapist prescribed. |
| **Progress** | Day streak, sessions this week, total reps, 7-day bar chart, pain-after-exercise trend, full history by day, and **Send report** to share a text summary with a physio or family by WhatsApp/email. |
| **Pain check** | After each exercise: “How does it feel now?” with 0–10 faces. A gentle warning appears for high scores. |
| **Reminders** | Optional daily notification at a chosen time (survives phone restarts). |
| **Voice coach** | Uses the phone's built-in text-to-speech, works offline. “Read the steps aloud” on every exercise. |

---

## 5. Design plan

### Who it's for
Older adults doing home exercises from a physiotherapist, often with reduced eyesight, less confident with phones, and sometimes with tremor or stiff fingers. Secondary users: family members setting it up, physios reviewing the report.

### Accessibility principles
- **Big everything**: body text 20sp, buttons at least 60–80dp tall, the rep counter at 88sp. A **Text size** setting (Normal / Large / Largest) scales the whole app on top of the phone's own font setting.
- **Words, not just icons**: every button has a label (“Back”, “Start exercise”, “Skip rest”). The bottom bar uses four large labelled tabs instead of the small standard bar.
- **Hands-free during exercise**: voice counting, camera counting, the screen stays on, and haptic feedback on each rep.
- **Forgiving**: confirmation before stopping or deleting, a Stop that still saves progress, +1 to correct the camera, everything undoable from the detail screen.
- **Calm, high-contrast palette**: deep pine `#0F5A5E` for actions, sunflower `#F5B700` reserved for “you did it” moments (rep counter, tap area, active joint), paper `#F3F6F2` background, ink `#17201F` text, coral `#C2412D` only for Stop/Delete.
- **Plain language**: “Helps with: getting up from chairs”, not clinical terms; safety notes in everyday words.

### Screens and flow
```
Home ──► Start my plan ──► Session (exercise 1 → 2 → …) ──► Well done + pain check
  │
  ├─► Exercises ──► Body area ──► Exercise detail ──► Session
  │                     (search)        │
  │                                      └─ Read aloud · My target · Add to plan
  ├─► Progress  (streak, charts, history, send report)
  └─► Settings  (name, text size, voice, camera, reminder, delete data)

Session:  Ready → 3-2-1 → Active (camera / tap / hold) → Rest → … → Done
```

### Architecture (why it stays light)
- **Kotlin + Jetpack Compose**, single activity, Navigation Compose. No Room/Hilt/Retrofit/Firebase.
- **Storage**: history as a small JSON file in app storage (written atomically on a background thread); settings, plan and custom targets in SharedPreferences.
- **Camera**: CameraX preview + ImageAnalysis at 640×480, newest-frame-only so it never lags.
- **Body tracking**: Google ML Kit Pose Detection (bundled model, works offline). `RepCounter` smooths the joint angle and counts a rep when it travels from a start angle past a target angle and back – with hysteresis so jitter doesn't double count. Rules are per exercise (e.g. knee angle hip-knee-ankle 115° → 158° for sit-to-stand) and either both sides averaged or each side independently.
- **Guide animation**: a jointed stick figure in `ExerciseFigure.kt` interpolates between two key poses per exercise, drawn on a Canvas.
- **APK size**: R8 shrinking + resource shrinking, and only `arm64-v8a` / `armeabi-v7a` native code (real phones). Expect roughly 15–25 MB, most of which is the offline body-tracking model.

### Project layout
```
app/src/main/java/com/physiocare/app/
├── MainActivity.kt
├── data/
│   ├── Models.kt            exercise, guide pose, camera rule, target
│   ├── ExerciseLibrary.kt   all 98 exercises (edit here to add more)
│   ├── Recovery.kt          paralysis type + movement level → exercises, safety text
│   └── AppRepository.kt     local storage + streak / stats / report
├── pose/
│   ├── CameraPoseView.kt    CameraX + ML Kit + skeleton overlay
│   └── RepCounter.kt        joint-angle maths and rep counting
├── ui/
│   ├── PhysioApp.kt         navigation, big bottom tabs, safety welcome
│   ├── Theme.kt             colours and large type scale
│   ├── components/          big buttons, steppers, animated figure
│   └── screens/             Home, Library, Detail, Session, Recovery, Progress, Settings
└── util/                    voice coach, daily reminder
.github/workflows/build-apk.yml   builds the APK on every push
```

### Adding your own exercise
Add an `ex(...)` entry in `ExerciseLibrary.kt`: give it an id, name, category, steps, mode (`MANUAL`, `HOLD`, or add a `rule = AngleRule(...)` for camera counting) and a `guide` with two poses. Push to GitHub and a new APK is built.

---

## 6. Privacy and safety
- No account, no ads, no analytics, no internet permission. Camera frames are analysed live on the phone and immediately discarded.
- The app gives general exercise guidance and is not a medical device or a substitute for a physiotherapist. Users see a safety notice on first launch and on every exercise.

## 7. Building on your own computer (optional)
Install Android Studio, open the folder, and press Run. Or with Android SDK + Gradle 8.7 + JDK 17: `gradle assembleRelease`.
