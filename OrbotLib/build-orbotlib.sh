#!/bin/sh

rm *.aar
cd OrbotIPtProxy
bash build-orbot.sh
mv OrbotLib.aar ..
