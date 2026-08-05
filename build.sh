sudo update-java-alternatives -s java-1.11.0-openjdk-amd64
java -version
javac -version

export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/29.0.14206865
export ANDROID_NDK_ROOT=$ANDROID_NDK_HOME
export PATH=$ANDROID_NDK_HOME:$PATH

export PKG_CONFIG_LIBDIR="$(pwd)/external/lib/pkgconfig"

cd orbotservice/src/main
ndk-build NDK_PROJECT_PATH=.
cd ../../..
make -C external -f build-tools
make -C external
#android update project --name Orbot --target android-23 --path .
#./gradlew assemble
./gradlew assembleRelease
./gradlew --stop
