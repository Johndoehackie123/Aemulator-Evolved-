# AEmulator Evolved — Firmware Hub fork

Repository: **https://github.com/Johndoehackie123/Aemulator-Evolved-**
GitHub: **https://github.com/Johndoehackie123**

This repository is a fork-style overlay on **[uxazu/AEmulator](https://github.com/uxazu/AEmulator)**
(v0.0.0.3, pinned at `3b97ed585e484395c455aca7338d7ddcbf955d9f`). It adds a **Firmware Hub**.
The app is now named **AEmulator Evolved** (approved rename); the engine, importer and
upstream attribution are preserved. AEmulator is GPL-3.0 and based on the original SENSES
emulator by Istratii-tech; the in-app settings link to both this fork and the upstream project.

## Updater

The in-app updater checks **this repository's releases**, not upstream:
`https://api.github.com/repos/Johndoehackie123/Aemulator-Evolved-/releases`.
Settings → Updates → "Check for updates" uses your fork's APK assets.

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
- Entry point: **+ → Firmware Hub** on the library screen, with its own Material icon.
- Expressive Material 3 styling: dynamic color (when enabled in AEmulator settings), extra-large
  shapes, animated cards and a stats header. Verified downloads are queued for import
  automatically; unverified files still need your explicit confirmation.
- **Ten layouts** — Cards (default), Compact, Grid, Mosaic, List, Timeline, Featured, Table,
  Carousel and Text only. Switch between them in the Hub settings sheet; your choice persists.
  Every layout uses the same actions and verification rules.
- **Main-app navigation** — Classic (unchanged default), Bottom bar, Navigation rail,
  Modal drawer, Top tabs and Side strip. Settings → Navigation style saves the choice.
  The shell reaches Machines, Firmware Hub, Settings and Updates. The FAB is Classic-only;
  other styles put Import in the top bar.
- **Glass theme** — Settings → Theme → Glass theme adds frosted translucent surfaces and
  luminous edges across the library, Hub and settings.

## Signing and updates

The APK is signed with the fixed keystore in `signing/`, so every release has the same app
signature and can upgrade in place. CI verifies each APK with
`apksigner verify --print-certs` and refuses to publish a mismatched certificate.

Pinned signing certificate SHA-256:
`8220F2EA53A512C7F8F7D576A8796D744799602F083C0B83C56A05511D285C3A`
(see `signing/CERTIFICATE_SHA256.txt`). The keystore and its password are in the repository:
this is a **public** sideloading key, not a secret. Because earlier releases used a
runner-generated debug key, install v0.0.0.6 once over the old app (one-time uninstall),
then all later releases update without uninstalling.

Release notes are generated from `CHANGELOG.md`; every release includes the changes, APK
SHA-256, signing-certificate fingerprint and unit-test count.

## Customising layouts

Hub settings → **Layout** picks the view. Under **Details in &lt;layout&gt;** you can show or
hide Status, Size, Checksum, Source, Format and Compatibility, and reorder them with the
up/down buttons. Those choices are saved **per layout**, so a dense Table can show different
fields from the Featured banner. **Reset details** restores the canonical order.

The implementation is in `overlay/app/src/main/java/app/aemu/hub/` plus small patched copies
of `MainActivity.kt`, `Home.kt`, `AppSettingsActivity.kt`, `UpdateDialog.kt`,
`AppUpdateManager.kt`, `build.gradle.kts` and `values/strings.xml`. The original draft files are kept
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
checksum file, a ZIP contents listing and the unit-test summary. It is also attached to
the workflow run as an artifact named `Aemulator-Evolved-arm64-debug`.

Direct download from the branch:
`https://raw.githubusercontent.com/Johndoehackie123/Aemulator-Evolved-/arena/f3a2b860-aemulator-evolved/artifacts/Aemulator-Evolved-standard-debug.apk`

Releases: https://github.com/Johndoehackie123/Aemulator-Evolved-/releases
SHA-256 is published in each release's `.sha256` asset and in `artifacts/`.

- Requires Android 8.0+ (API 26), arm64-v8a, ~1–3 GB free storage per firmware.
- `targetSdk 28` is inherited from upstream; this is intentional (guest binaries need it).
- The debug build is unsigned-debug; Android accepts it for sideloading.
- Installed package ID is **`app.aemu.evolved`** (clone: `app.aemu.evolved.clone`,
  v0003: `app.aemu.evolved.v0003`). The Kotlin namespace stays `app.aemu` so upstream
  source can be patched without a mass rename. It installs **side by side** with the
  upstream `app.aemu` build; the FileProvider authority is `app.aemu.evolved.provider`.

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
Fork maintained by [Johndoehackie123](https://github.com/Johndoehackie123) at
[Johndoehackie123/Aemulator-Evolved-](https://github.com/Johndoehackie123/Aemulator-Evolved-).
Firmware images, Android and device trademarks belong to their respective owners.
