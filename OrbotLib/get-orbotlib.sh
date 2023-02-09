#!/bin/sh

if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/guardianproject/OrbotIPtProxy
fi
cd OrbotIPtProxy
git fetch
git rebase
