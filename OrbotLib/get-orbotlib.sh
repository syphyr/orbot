#!/bin/sh

if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/syphyr/OrbotIPtProxy -b stable-release
fi
cd OrbotIPtProxy
git fetch
git rebase
