#!/bin/sh

if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/syphyr/OrbotIPtProxy
fi
cd OrbotIPtProxy
git fetch
git rebase
