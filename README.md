# ADhils Fitness

Personal Android fitness app for Samsung Galaxy S22 and Galaxy Tab A9+. Native Kotlin/Compose UI, local Room persistence, on-device MediaPipe camera analysis, and an optional Windows AI companion.

Download the Samsung APK and checksum from [GitHub Releases](https://github.com/ksrdhilip/adhils-fitness/releases). Sign in to the account with access to this private repository. Release notes describe tested behavior and remaining device validation.

## What this preview implements

- Separate profiles: preferences, exclusions, workouts, measurements, routines, chats, progression, and unfinished workouts.
- Workout generation for home equipment, experience, goal, time, energy and soreness. Transparent progression from actual completed sets and RPE.
- Sets/reps/weight/hold logging, warm-up tagging, notes, previous performance, rest timer, exercise substitutions, workout summaries and saved routines.
- Five camera variations: dumbbell Romanian deadlift, standing dumbbell shoulder press, goblet/bodyweight squat, standard push-up and forearm plank. Counts return to an editable set form before saving.
- Exercise instructions, placement illustrations, and optional personal demonstration videos selected on the device.
- Exercise trends, workout history, body-weight records, weekly coach review and reviewed reductions to active-workout sets.
- Encrypted multi-profile backup export/import. No automatic device synchronization.
- Dark, light and system themes; bottom navigation on phones and a navigation rail on larger displays.

This is a personal preview, not a reproduction of another app's proprietary videos, branding or database. Nutrition, wearables, social features, advanced periodization, trainer administration and iOS remain future work.

## Build Android

Prerequisites: Android Studio's installed JDK 21, Android SDK platform/build-tools 36 and the verified Gradle 8.11.1 distribution. `scripts/bootstrap.ps1` is a separate, reviewable download step using official Gradle and Google model URLs. Builds do not invoke it automatically.

```powershell
./scripts/build.ps1
```

This runs core tests, debug APK assembly and Android lint. The script uses the existing Android Studio runtime and a project-local Gradle cache. The `.tmp` directory supplies Java's local socket files to avoid packaged-app TEMP redirection on Windows.

Samsung APK output: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`. The separate `app-x86_64-debug.apk` is for the test emulator. Transfer the ARM64 file to your own phone/tablet and open it to install the preview. Android may request permission for the specific app opening the APK to install unknown apps; do not disable Play Protect. Future builds must use the same signing key to update existing data.

## Navigation

Actual emulator captures: [Today](docs/screenshots/today-phone.png), [workout logging](docs/screenshots/workout-phone.png), and [camera fallback on x86](docs/screenshots/camera-phone.png). These use test profiles. Live camera inference requires the Samsung ARM64 build.

1. **First launch → Profile:** enter name, goal, experience, equipment, weekly target, duration, units and exercise exclusions; save.
2. **Today → Check in & start:** adjust energy/soreness/time, then start the generated session. Today resumes an unfinished session.
3. **Workout:** choose a set, enter actual values and save. Open exercise guidance or replace an exercise. Camera-supported exercises open camera setup → permission → position → countdown → tracked set → editable set form. Pause/end controls remain available.
4. **Finish → Summary:** review logged sets and progression reasons; save the plan as a routine if desired.
5. **Workouts:** browse/search exercises, start saved routines, or open completed-session history.
6. **Progress:** view exercise trends and log body weight. Empty states remain empty until you record real data.
7. **Coach:** ask about the selected profile's recent training or request a weekly review. A workout change appears as a proposal; tap Apply only after reviewing it.
8. **Settings:** edit/switch profiles, choose AI provider, pair PC, configure voice/cue/theme preferences, export/import backup, or clear the selected profile's training data.
9. **Profile button at the top of root screens:** switch or add a profile. Each profile keeps its unfinished workout. Profiles are separate data collections, not password-protected user accounts.

## Windows AI companion

The companion uses the installed Node 24 runtime and Codex CLI. No cloud server or paid API is required for workout logging or camera tracking. ChatGPT mode uses an official Codex App Server client with its own local login; the PC must stay awake. Subscription availability and limits still apply.

```powershell
Set-Location companion
npm ci --ignore-scripts --registry=https://registry.npmjs.org
node src/cli.mjs setup
node src/cli.mjs login
node src/cli.mjs serve --host YOUR_PC_PRIVATE_IPV4
```

`setup` uses the OpenSSL executable already installed with Git for Windows to create a local TLS identity. It protects only `companion/.state` for the current Windows user, SYSTEM and Administrators. It does not install services, change antivirus settings, alter execution policy or open firewall ports.

Open `companion/.state/pairing.html` locally. On Android, choose **Settings → Scan PC pairing QR** (or Paste invitation). Each invitation expires after five minutes and works once; restart the companion to generate a new one. Pair phone and tablet separately. The app pins the TLS certificate fingerprint from the invitation. It will refuse a changed certificate until paired again.

Use your trusted private network. If Windows asks about network access, limit it to your private network. Do not forward the companion port from your router. Default `serve` binds only to loopback for local testing; specify the PC's private IPv4 to connect a physical device.

```powershell
node src/cli.mjs status
node src/cli.mjs devices
node src/cli.mjs revoke DEVICE_ID
```

API providers are optional. Save a key interactively on the PC; do not paste it into the mobile app or source files:

```powershell
./scripts/credential.ps1 -Action Set -Name openai
./scripts/credential.ps1 -Action Set -Name claude
```

Windows user encryption protects stored API keys. Run the companion as the same Windows account that saved them. The internal `Read` action is for the companion process only; it emits a decrypted key to its pipe and must not be run in a visible terminal.

## Budget and data

- One SQLite journal enforces a shared **$5 per UTC calendar month** API budget across profiles and paired devices. ChatGPT subscription mode has no API charge in this journal.
- A paid request reserves $0.10 before dispatch. Supported request sizes/output limits keep its estimated maximum below this reservation at the pinned model rates. Successful usage reconciles the reservation; unknown or failed outcomes keep it, conservatively.
- The same device/profile/request ID is never automatically billed twice. Incomplete requests after a crash retain their reservation. No automatic provider fallback or paid retry occurs.
- The budget covers requests through this companion only, not other uses of your API account, taxes or future provider price changes. Review provider prices before changing model IDs or prices. Deleting the journal resets its records; keep it backed up on the PC.
- The companion sends only the selected profile, prompt, active workout and bounded recent history, including up to eight recent chat messages, to the selected AI provider. It does not receive video frames. Replies and request hashes are stored locally for retry handling; device tokens are stored as hashes.
- The app database stores a validated `ProfileStore` snapshot atomically in Room. Profile selection and updates are serialized. AI replies retain their originating profile; restoring/clearing data invalidates in-flight replies.
- Backup uses AES-256-GCM with PBKDF2-HMAC-SHA256, random salt/nonce, and authenticated format header. It transfers all profiles. Import requires password and replacement confirmation. Personal video URIs and companion credentials do not transfer.

## Camera limits and validation

Rep counts, hold time and optional movement cues are experimental 2D estimates. They do not establish spinal alignment, injury risk or safe lifting loads. Movement cues default off; enable the preview in Settings if you want to evaluate it. Synthetic tests verify counting state transitions, visibility interruptions and pauses; they do not validate real human posture accuracy.

Google's MediaPipe 0.10.26 Android package contains only an ARM64 native vision library. The x86 emulator build therefore offers a clear manual-logging fallback instead of camera inference. Camera permission, model initialization and tracking performance must be checked on the Samsung devices with the ARM64 APK. Model initialization runs off the UI thread and library-loading errors have a manual fallback.

Use side view for RDL, squat, push-up and plank; face the camera for standing press. Keep required joints visible and use a stable support. If tracking is lost the counter pauses, and incomplete rep phases are discarded. Correct counts before saving. Stop a movement that causes pain.

Before relying on coaching, validate on **both S22 and Tab A9+**: supported variations, front/back camera, bright/dim rooms, full-body framing, clothing/background changes, partial reps, pauses, camera denial, rotation, app backgrounding and a 15-minute thermal/performance run. Compare counts with manual annotations. Do not label cues clinically validated.

## Tests and source trust

```powershell
./scripts/build.ps1
node --test companion/test/*.test.mjs
python scripts/verify_downloads.py
```

Android dependencies resolve from Google Maven, Maven Central and Gradle's plugin repository. The companion's only direct npm dependency is pinned `qrcode` 1.5.4, installed with lifecycle scripts disabled and locked integrity hashes. The model comes from Google's versioned MediaPipe model storage. See `docs/SECURITY_REVIEW.md` for the prior Defender incident and evidence; no blanket malware-free guarantee is made.

Primary implementation references: [MediaPipe Android](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker/android), [CameraX analysis](https://developer.android.com/media/camera/camerax/analyze), [Codex App Server](https://learn.chatgpt.com/docs/app-server), [OpenAI model pricing](https://developers.openai.com/api/docs/models/gpt-5-mini), [Claude structured outputs](https://platform.claude.com/docs/en/build-with-claude/structured-outputs), [Gradle checksums](https://gradle.org/release-checksums/).
