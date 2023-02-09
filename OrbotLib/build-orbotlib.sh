#!/bin/sh

rm *.aar *.jar
cd OrbotIPtProxy
bash build-orbot.sh
mv OrbotLib.aar ..
mv OrbotLib-sources.jar ..
