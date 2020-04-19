export ANDROID_TOOLCHAIN_HOME=~/src/android-ndk-r20b
export ANDROID_NDK_HOME=~/toolchains/arm-linux-androideabi-4.9
export PATH=$ANDROID_NDK_HOME/bin:$PATH

cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=.
cd ../../..
make -C external
android update project --name Orbot --target android-23 --path .
./gradlew assemble
