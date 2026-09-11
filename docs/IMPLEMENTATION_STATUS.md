# Implementation checkpoint — 2026-09-10

The native Android app and Windows companion are implemented. The initial validation below was performed locally. Source and installable previews are distributed through the private ksrdhilip/adhils-fitness repository and its GitHub Releases page; publication does not deploy the Windows companion or install the mobile app. No paid AI calls have been made.

## Verified so far

- Final debug APK assembly and Android lint completed successfully (0 errors, 18 warnings). No remaining 16 KB alignment warning.
- All 16 core tests passed: 9 profile/progression/backup tests and 7 synthetic camera counting, pause, visibility and cue tests. Together with the companion tests, 27 tests passed.
- All 11 companion tests passed, including a real local TLS handshake, pairing expiration/replay protection, device revocation, shared budget persistence, request deduplication, and selected-profile context.
- npm audit reported zero known vulnerabilities. All 29 transitive/direct npm package records use `registry.npmjs.org` and include integrity hashes.
- Gradle dependency verification metadata was generated from artifacts resolved through configured official repositories. It detects later changes; generated checksums are not independent publisher attestations.
- A private companion TLS identity was created using installed Git for Windows OpenSSL. Codex App Server initialized successfully in an isolated home and correctly reported that ChatGPT login is required.
- Companion serving and QR generation were exercised on loopback only. No firewall rule or startup service was installed.
- Final x86 APK installed and launched on the existing Pixel 9a API 36 emulator. Onboarding, Today, workout logging and profile switching worked. Dhilip's logged warm-up and unfinished workout persisted across profile switches and APK updates; Guest received a separate bodyweight plan and empty history. Scroll-position carryover was found and corrected.
- Google's MediaPipe 0.10.26 vision library contains ARM64 binaries only. An observed x86 camera crash was fixed with an explicit manual-logging fallback, verified on the final emulator APK. This does not verify ARM64 camera inference.
- Final Samsung APK signature verified. Defender completed the final APK-folder scan with no new detection events in the checked interval. See SECURITY_REVIEW.md for scan ID and SHA-256.

## Artifacts

- Samsung installable preview: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
- Actual emulator screenshots: `docs/screenshots/`. These show test profiles and data, not the user's measured fitness history.
- Phone Today, workout logging and the x86 camera fallback were captured. Tablet visual verification remains pending: the emulator disconnected during the display-size change and a different emulator process returned without this app installed. Temporary display overrides were reset; no unrelated emulator data was erased.
- Setup, navigation and pairing instructions: `README.md`.

## Requires the user's devices or account

- Sign in to ChatGPT with the companion `login` command and pair the S22/Tab A9+ on the trusted private network.
- Verify real exercise counts, movement cues, camera orientation and sustained performance on both Samsung devices. Synthetic tests and an emulator cannot establish posture-coaching accuracy.
- Optional OpenAI/Claude API keys need to be configured by the user if those providers are wanted.

The earlier project-wide Defender scan was interrupted. Defender reported recorded detections inactive and no threat execution; this does not certify the whole computer malware-free.

## Camera startup fix — version 0.1.1

The user's front-camera screenshot confirmed "cannot use a recycled source in createBitmap". The app was displaying/mirroring the bitmap after MPImage.close recycled it. The fix creates an independent preview copy before releasing the inference image, including zero-rotation frames, and cleans up temporary buffers on failure.

Four Android instrumentation tests passed on API 35, reproducing the original invalid preview and checking rear rotations, front mirroring and error cleanup. The 16 core tests passed; the final build and lint completed (0 errors, 18 warnings). The package version is 0.1.1 / code 2; its signing certificate matches 0.1.0 for normal in-place updates. See the 0.1.1 release notes and SECURITY_REVIEW.md for checksum and scan evidence.

These tests validate Android bitmap ownership without requiring native ARM64 inference. Real Samsung movement accuracy and sustained performance still need user-device validation.
