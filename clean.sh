sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
java -version
javac -version

export ANDROID_TOOLCHAIN_HOME=~/src/android-ndk-r21d
export ANDROID_NDK_HOME=~/toolchains/arm-linux-androideabi-4.9
export PATH=$ANDROID_NDK_HOME/bin:$ANDROID_TOOLCHAIN_HOME:$PATH

cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=. clean
cd ../../..
make -C external clean
./gradlew clean
./gradlew --stop
rm -rf build
