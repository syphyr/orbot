#!/bin/sh

rm *aar *jar
git clone https://github.com/bitmold/OrbotIPtProxy
cd OrbotIPtProxy
git pull
bash build-orbot.sh
mv OrbotLib.aar ..
mv OrbotLib-sources.jar ..
cd ..
