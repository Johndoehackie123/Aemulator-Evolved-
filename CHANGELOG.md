# Changelog

Release notes are generated from the latest section of this file by
`.github/workflows/release.yml`. Add a `## vX.Y.Z` heading above with bullet points before
dispatching a release.

## v0.0.0.6

- **Stable APK signature.** A fixed signing key now lives in `signing/`; every CI build is signed
  with it and verified with `apksigner verify --print-certs`. The certificate SHA-256 is pinned in
  `signing/CERTIFICATE_SHA256.txt` and printed in every release. After installing this build once,
  later releases update in place without uninstalling.
- **Main-app navigation styles.** Classic (unchanged default), Bottom bar, Navigation rail,
  Modal drawer, Top tabs and Side strip are available under Settings → Navigation style.
  Navigation reaches Machines, Firmware Hub, Settings and Updates.
- **Updates destination.** A dedicated Updates screen shows the installed version and checks this
  fork's GitHub releases.
- **Release notes from CHANGELOG.md.** Every release now includes the changes for its version,
  the APK SHA-256, the signing-certificate fingerprint and the unit-test count.

## v0.0.0.5

- Added List, Timeline, Featured, Table, Carousel and Text only layouts alongside Cards, Compact,
  Grid and Mosaic.
- Added per-layout detail customisation: show/hide and reorder Status, Size, Checksum, Source,
  Format and Compatibility.
- Added pure-JVM tests for field ordering.

## v0.0.0.4

- Renamed the app to AEmulator Evolved.
- Updater and settings links now point to Johndoehackie123/Aemulator-Evolved-.
- Package ID changed to `app.aemu.evolved`.

## v0.0.0.3-hub

- Initial Firmware Hub: curated catalog, one-tap resumable downloads, host checksum verification,
  import queue, Cards/Compact/Grid layouts and Wi-Fi-only/storage settings.
