#!/usr/bin/env sh

BASE=$(dirname "$0")

# Update geoip files from tor.
sh "$BASE/libs/build-geoip-jar.sh"

# Update built-in bridges.
cd scripts || exit
./update_snowflake_bridges.sh
