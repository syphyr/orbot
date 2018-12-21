cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=. clean
cd ../../..
export ANDROID_NDK_HOME=~/src/android-ndk-r15c
make -C external clean
./gradlew clean
