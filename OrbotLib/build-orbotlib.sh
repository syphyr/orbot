#!/bin/sh

sudo update-java-alternatives -s java-1.17.0-openjdk-amd64
java -version
javac -version

rm *.aar
cd OrbotIPtProxy
bash build-orbot.sh
