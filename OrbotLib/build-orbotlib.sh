#!/bin/sh

export ANDROID_NDK_HOME="/usr/local/src/androidSDK/android-sdk-linux/ndk/29.0.14206865"
export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"

echo "NDK is set to $ANDROID_NDK_ROOT"

sudo update-java-alternatives -s java-1.25.0-openjdk-amd64
java -version
javac -version

rm -v *.aar
cd OrbotIPtProxy
bash build-orbot.sh
