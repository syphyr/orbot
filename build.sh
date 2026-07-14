sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
java -version
javac -version

unset ANDROID_NDK_HOME

export PKG_CONFIG_LIBDIR="$(pwd)/external/lib/pkgconfig"

export ANDROID_TOOLCHAIN_HOME="$HOME/src/android-ndk-r21e"

export PATH=$ANDROID_TOOLCHAIN_HOME:$PATH

cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=.
cd ../../..
make -C external
#android update project --name Orbot --target android-23 --path .
#./gradlew assemble
./gradlew assembleRelease
./gradlew --stop
