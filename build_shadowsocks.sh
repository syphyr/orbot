#!/bin/bash

# We can't use rust-android-gradle to build shadowsocks-rust, since it uses functionality that
# breaks with gradle 9. Rust binaries for shadowsocks can be built via shadowsocks-android which
# uses the plugin...
# Rust Android Gradle Plugin:
# https://github.com/mozilla/rust-android-gradle/
# Open PR for gradle 9 support:
# https://github.com/mozilla/rust-android-gradle/pull/168

set -eo pipefail

if ! command -v rustup >/dev/null 2>&1; then
  echo "Error: rustup is not installed. Please install before running this script."
  echo ""
  echo "To install rust if needed:"
  echo "curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
  exit 1
fi

cd ..

if [ ! -d shadowsocks-android ]; then
   echo "Cloning shadowsocks-android repo"
   git clone --recurse-submodules https://github.com/shadowsocks/shadowsocks-android
fi

cd shadowsocks-android
echo "Updating shadowsocks-android repo"
git fetch
git submodule update --init --recursive

echo ""
echo "Adding android native arm64 target for rust"
#rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
rustup target add aarch64-linux-android

echo ""
echo "Building shadowsocks-android library"
#./gradlew mergeReleaseJniLibFolders
./gradlew mergeReleaseJniLibFolders -PTARGET_ABI=arm64

cd core/build/rustJniLibs/android/
echo "Built shadowsocks-android binaries:"
ls -al *

echo ""
cp -av arm64-v8a ../../../../../orbot/app/src/main/jniLibs/
#cp -av armeabi-v7a ../../../../../orbot/app/src/main/jniLibs/
#cp -av x86 ../../../../../orbot/app/src/main/jniLibs/
#cp -av x86_64 ../../../../../orbot/app/src/main/jniLibs/
