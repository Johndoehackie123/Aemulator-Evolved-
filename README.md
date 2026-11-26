# AEmulator — Firmware Hub fork

This repository is a fork-style overlay on **[uxazu/AEmulator](https://github.com/uxazu/AEmulator)**
(v0.0.0.3, pinned at `3b97ed585e484395c455aca7338d7ddcbf955d9f`). It adds a **Firmware Hub**
without changing the app's name, branding, engine or importer. AEmulator is GPL-3.0 and
based on the original SENSES emulator by Istratii-tech; that attribution is preserved in
the app's Hub screen and in upstream files.

## What was added

- **Firmware catalog** — six community-reported firmware images from `dl.google.com` and
  `archive.org`. No firmware is hosted or redistributed here; every link goes to the source.
- **One-tap downloads** — resumable `.part` downloads, serial queue, retry, live progress,
  speed and ETA. Closing the Hub screen does not stop a transfer.
- **Checksum verification** — files from Internet Archive are checked against the SHA-1
  fingerprint published in that item's `/metadata/<id>` API. Verification is streaming:
  the digest includes resumed bytes. A mismatch discards the partial file and blocks import.
  Two old Google factory images have **no published machine-readable checksum**; they are
  labelled **UNVERIFIED** in the UI and require explicit confirmation before import.
  Compatibility notes are community reports, not guarantees.
- **Import queue** — verified downloads can be queued and are imported sequentially with
  the upstream `Importer`, so two large imports never run at once. Queue state and progress
  are visible in the Hub.
- Entry point: **+ → Firmware Hub** on the library screen.

The implementation is in `overlay/app/src/main/java/app/aemu/hub/` plus small patched copies
of `MainActivity.kt`, `Home.kt` and `values/strings.xml`. The original draft files are kept
in `drafts/` for provenance only; they are not compiled.

## Building

The ~500 MB prebuilt engine assets are not duplicated in this overlay repository. CI restores
the pinned upstream tree and copies `overlay/` over it:

```bash
git clone https://github.com/Johndoehackie123/Aemulator-Evolved-.git
cd Aemulator-Evolved-
cp .github/workflows/build-apk.yml /tmp/reference.yml   # shows the exact steps
```

Local build prerequisites: JDK 17, Android SDK platform 36 + build-tools 36, ~6 GB RAM.
To build locally:

```bash
git clone --depth 1 --branch 0.0.0.3 https://github.com/uxazu/AEmulator.git upstream
git -C upstream checkout 3b97ed585e484395c455aca7338d7ddcbf955d9f
cp -a overlay/. upstream/
sed -i '/^org.gradle.java.home=/d' upstream/gradle.properties
echo "sdk.dir=$ANDROID_HOME" > upstream/local.properties
cd upstream && chmod +x gradlew && \
  ./gradlew --no-daemon :app:testStandardDebugUnitTest :app:assembleStandardDebug
```

GitHub Actions does exactly this on every push to the working branch (except artifact-only
commits) and on manual dispatch: `.github/workflows/build-apk.yml`.

## APK

The CI build commits the resulting arm64-v8a debug APK to
[`artifacts/Aemulator-Evolved-standard-debug.apk`](artifacts/) together with a SHA-256
checksum file and a ZIP contents listing. It is also attached to the workflow run as an
artifact named `Aemulator-Evolved-arm64-debug`.

- Requires Android 8.0+ (API 26), arm64-v8a, ~1–3 GB free storage per firmware.
- `targetSdk 28` is inherited from upstream; this is intentional (guest binaries need it).
- The debug build is unsigned-debug; Android accepts it for sideloading.

## Verification status of catalog entries

| Entry | Source | Fingerprint |
|---|---|---|
| Galaxy S II (I9100XEKE1) | archive.org `i9100xexe` | SHA-1 from item metadata |
| Galaxy S III (I9300XXUGNJ2) | archive.org `i-9300-xxugnj-2-i-9300-oxxgnj-1-xef` | SHA-1 from item metadata |
| Redmi 1S (MultiROM MIUI 8) | archive.org `HM2014011` | SHA-1 from item metadata |
| Galaxy S II (MIUI 7 port) | archive.org `FullotaMiui7I9100Initial.release` | SHA-1 from item metadata |
| Nexus 4 (occam KTU84P) | dl.google.com | **Unverified** — no published checksum found |
| Galaxy Nexus (takju JWR66Y) | dl.google.com | **Unverified** — no published checksum found |

## License

GPL-3.0, inherited from [uxazu/AEmulator](https://github.com/uxazu/AEmulator/blob/main/LICENSE).
Firmware images, Android and device trademarks belong to their respective owners.
