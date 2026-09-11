# Security review — 2026-09-10

Implementation and build execution were paused after the user reported a Microsoft Defender alert.

## Detection evidence

- Defender threat: `Trojan:Win32/ClickFix.GLHD!MTB`, ID `2147943966`.
- The affected resources are command-line records for the agent's PowerShell bootstrap repair commands, including a command that combined source writes with that repair.
- Defender reported `DidThreatExecute: False` and an active alert. An active alert must not be treated as resolved based on its apparent cause alone.
- The agent initially interpreted the blocked process launches as sandbox errors and retried. That assumption was incorrect; the blocked commands will not be rerun.
- No antivirus exclusions, disabled protection, bypass execution policy, or allow-list actions were used.

## Artifact checks

- Gradle 8.11.1 ZIP was downloaded from the official Gradle distribution endpoint.
- Its local SHA-256 is `f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6`, matching https://gradle.org/release-checksums/ for 8.11.1.
- A read-only verifier in `scripts/verify_downloads.py` additionally compares extracted files with that verified ZIP.
- The MediaPipe model was downloaded from Google's versioned `mediapipe-models` storage location.
- Model local SHA-256: `59929e1d1ee95287735ddd833b19cf4ac46d29bc7afddbbf6753c459690d574a`. This is an inventory hash, not an independent publisher attestation.
- Its archive contains `pose_detector.tflite` and `pose_landmarks_detector.tflite`.
- Existing PowerShell executable: valid Microsoft Corporation Authenticode signature.
- Existing Android Studio Java executable: valid Google LLC Authenticode signature.

## Actions and limits

- No matching Gradle Java processes remained when queried during the review.
- Defender antivirus and real-time protection were confirmed enabled.
- A Defender custom scan was requested for this project, including downloaded artifacts.
- Normal Defender remediation was requested only after checking that this ClickFix detection was the sole active threat.
- The remediation command returned successfully, but the immediate follow-up still reported `IsActive: True` and `DidThreatExecute: False`. The detection is not considered cleared.
- Custom scan ID: `{CABAC563-2C1E-43BC-BCA5-A1DCB7B9AD0A}`, started at local time 2026-09-10 16:58:46. Completion was not yet confirmed when this note was written.
- Scan and remediation results must be checked before resuming builds. These checks do not certify the entire machine as malware-free.
- Build setup now separates installation from build execution. Future source edits use reviewable file patches; do not construct and immediately execute downloaded code inside repair command lines.

This report describes the evidence and actions taken; it is not a Microsoft false-positive determination.

## Follow-up

- Subsequent Defender query reported both recorded ClickFix detections inactive and `DidThreatExecute: False`.
- Read-only integrity verification passed for the official Gradle ZIP and all 314 extracted files.
- The user authorized continued implementation using trustworthy sources. The blocked command pattern remains retired.
- The broader custom scan was still running at that check; no completion claim is made here.

## Resume after desktop restart

- Defender again reported both detections inactive, with `DidThreatExecute: False`.
- Event 1002 records that the project custom scan stopped at 17:23:11 with an unknown stop reason. It did not complete; no clean project-scan claim is made.
- The Java build startup issue was resolved by setting the documented `jdk.net.unixdomain.tmpdir` property to the project's `.tmp` directory. This avoids packaged-app temporary-path redirection. Antivirus settings were not changed.
- New companion code uses Node built-ins for HTTPS, SQLite, and provider requests. The only npm dependency is pinned `qrcode`, installed from the official npm registry with lifecycle scripts disabled.

## Final package checks

- MediaPipe was updated to Google's 0.10.26 release to resolve the native 16 KB alignment warning. Gradle recorded SHA-256 verification metadata for dependency artifacts.
- All 29 npm dependency records use `https://registry.npmjs.org/` and have integrity hashes. npm audit reported zero known vulnerabilities at installation time.
- Defender custom scan `{53CF77D0-FE48-4959-B7A3-1E24D8C0EAB4}` covered the final APK output folder, started at 18:53:29 and finished at 18:53:39 on 2026-09-10. No new detection event appeared in the checked interval. Both recorded ClickFix detections remained inactive with no reported execution.
- Samsung ARM64 APK SHA-256: `3b4782c4478d71cb379806d23bfc5ca729aec2e477d59ba8380dca185b3aa47b`.
- Android SDK `apksigner verify --verbose` verified the ARM64 APK signature (v2 signing, one signer). This is a development signing key, not a store release identity.
- The earlier project-wide scan remained interrupted. A completed APK scan is not a certification of the whole computer.
