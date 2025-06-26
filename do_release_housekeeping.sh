# update geoip files from tor
cd libs || exit
./build-geoip-jar.sh
cd ../scripts || exit
./update_snowflake_bridges.sh
