#!/usr/bin/env bash
# Installs JDK 17 (Temurin) + Android SDK (platform 36, build-tools) into /home/user
set -euo pipefail
cd /home/user
mkdir -p jdk android-sdk/cmdline-tools tools-dl

echo "== JDK 17 =="
if [ ! -x /home/user/jdk/bin/java ]; then
  curl -fL --retry 3 -o tools-dl/jdk17.tar.gz \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
  tar -xzf tools-dl/jdk17.tar.gz -C /home/user
  JDK_DIR=$(find /home/user -maxdepth 1 -type d -name "jdk-17*" | head -1)
  rm -rf /home/user/jdk && ln -s "$JDK_DIR" /home/user/jdk
fi
/home/user/jdk/bin/java -version

echo "== Android cmdline-tools =="
if [ ! -x /home/user/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
  for V in 13114758 12266719 11076708; do
    if curl -fL --retry 2 -o tools-dl/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${V}_latest.zip"; then
      echo "got cmdline-tools ${V}"; break
    fi
  done
  rm -rf android-sdk/cmdline-tools/latest android-sdk/cmdline-tools/tmp
  mkdir -p android-sdk/cmdline-tools/tmp
  unzip -q tools-dl/cmdline-tools.zip -d android-sdk/cmdline-tools/tmp
  mv android-sdk/cmdline-tools/tmp/cmdline-tools android-sdk/cmdline-tools/latest
  rm -rf android-sdk/cmdline-tools/tmp
fi

export JAVA_HOME=/home/user/jdk
export ANDROID_HOME=/home/user/android-sdk
export ANDROID_SDK_ROOT=/home/user/android-sdk
SM=/home/user/android-sdk/cmdline-tools/latest/bin/sdkmanager
chmod +x "$SM" || true

yes | "$SM" --licenses >/dev/null 2>&1 || true
echo "== SDK packages =="
"$SM" "platform-tools" "platforms;android-36" "build-tools;36.0.0" 2>&1 | tail -3

echo "== DONE: $(date) =="
ls /home/user/android-sdk
