# AEmulator — clone & inspection notes

Source: https://github.com/uxazu/AEmulator (cloned to `/home/user/AEmulator`) · GPL-3.0 · v0.0.0.3
Based on the earlier SENSES emulator by Istratii-tech.

## What it is
An Android app that boots **real Android firmware images on your phone** — no root, no PC.
Android 2.3–17; HTC Sense, TouchWiz, MIUI, AOSP skins work as shipped.

How: 32-bit firmware (2.3–4.4) runs under a patched user-mode QEMU; 64-bit firmware (5.0–17)
runs natively on the phone's cores with an emulated binder, a GL bridge to the host GPU,
and adapted audio/camera/sensor HALs.

## Repo at a glance
| Part | Size | Notes |
|---|---|---|
| `app/` | ~493 MB | 477 MB of it is **prebuilt guest engine binaries** (`assets/engines/*`, per-Android-version kernels) |
| `app/src/main/java/app/aemu/` | ~16 k lines Kotlin | Compose + Material 3 Expressive UI, 18 languages |
| `native/` | 3.9 MB C | 19 components: binderd, glbridge, audio/camera/sensor/light/power HALs, appwrap… |
| `app/src/main/jniLibs/arm64-v8a` | 15 MB | Prebuilt `.so` per API level 21–37 |

### Code map
- `core/` — Engine, GuestVm, Kernel64, GlHost, NetProxy/TlsBridge, AdbServer, VncServer, AudioIn/Out, CameraFeed, Sensors, Sdcard, ScreenRecorder, Keeper (foreground service)
- `importer/` — ZIP/TWRP, Odin `.tar.md5`, factory `.tgz`, `system.img`, OTA `system.new.dat.br`, 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, ext4 reader, boot image parser
- `ui/` — MainActivity, Home (library), VmActivity (running system: ⋮ menu, nav bar, keyboard), SettingsSheet, AppSettingsActivity, Theme
- `update/` — in-app update checker

## Already built in (don't reinvent)
Firmware import in ~14 formats · GPU rendering via GL bridge · sound, mic, camera, accelerometer, vibration ·
network + modern-TLS proxy · shared memory-card folder · ADB over Wi-Fi + built-in terminal ·
screenshots · screen recording · VNC server · fake SMS sender · airplane mode / stay-awake toggles ·
APK install into the guest · 3-button nav bar · notification-LED emulation · 18 languages · M3 Expressive look.

## Real gaps (candidate new features)
1. **Firmware Hub** — no in-app catalog or one-tap download; you must find a file on the forum first.
2. **Gamepad support** — none (a Gamepad icon exists in the UI, but no controller mapping).
3. **True save-state / quick resume** — "snapshot" in the code is only a screenshot; a running VM can't be frozen and resumed instantly.
4. **Multi-instance** — one VM at a time.
5. **Performance HUD** — frame stats are internal; no FPS/CPU/RAM overlay.
6. **Automation** — no per-firmware home-screen shortcut, no scheduled auto-backup, no import queue.
7. **Identity** — it's AEmulator with its branding; a clone could rebrand (name, icon, colors, strings).

## Practical constraints found
- **Build needs JDK 17 + Android SDK 36 + NDK r28**; this sandbox has JDK 11 and no SDK (installable, ~5–6 GB).
- Workspace snapshots cap ~128 MB → the 477 MB engine assets won't persist between sessions;
  keep a slim source tree + a fetch/restore script instead.
- **GPL-3.0**: a fork must stay open-source and credit uxazu / SENSES. Shipping it as your own closed app isn't allowed.
- Target device must be **arm64-v8a, Android 8+** (that's ~every phone since 2018).
