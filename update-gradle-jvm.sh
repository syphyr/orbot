#!/bin/bash

JDK_VERSION=25

# this is kept in .gitignore...
FILE="gradle/gradle-daemon-jvm.properties"


if [ -f $FILE ]; then
    echo ""
    echo "Warning: $FILE already exists with contents:"
    echo ""
    cat "$FILE"
    echo ""
    read -r -p "$FILE will be overwritten, do you want to continue [y/N] " response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]
    then
    	rm "$FILE"
    	echo ""
    else
    	exit
    fi
fi

echo "Configuring Orbot to use Java $JDK_VERSION..."
echo "Creating new $FILE..."
echo "toolchainVersion=$JDK_VERSION" > "$FILE"

echo "Obtaining correct Java configuration for Java $JDK_VERSION..."
echo ""

# uses toolchainVersion to fetch binaries for host OS/arc
./gradlew updateDaemonJvm
echo ""
echo "$FILE is updated to:"
cat "$FILE"

echo ""
echo "Attempting to clean Orbot and do a test build..."
./gradlew clean assembleFullpermDebug

echo ""
echo "Gradle is now configured to build Orbot on your machine with Java $JDK_VERSION. You should be able to run \"./gradlew assemble\" or use Android Studio to build Orbot..."
