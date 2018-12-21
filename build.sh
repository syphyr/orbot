cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=.
cd ../../..
export ANDROID_NDK_HOME=~/src/android-ndk-r15c
make -C external
android update project --name Orbot --target android-23 --path .
./gradlew assemble
