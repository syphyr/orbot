#!/usr/bin/env sh

BASE=$(dirname "$0")

cd "$BASE/../app/src/main/assets" || exit

rm -f builtin-bridges.json
rm -f dns-*.json

wget -O builtin-bridges.json https://bridges.torproject.org/moat/circumvention/builtin || echo "Builtin bridges failed to update"

COUNTRY_LIST="ae af bd cn co global id ir kw pk qa ru sy tr ug uz"

for country in $COUNTRY_LIST; do
    wget -O dns-"$country".json https://raw.githubusercontent.com/dnstt-xyz/dnstt_xyz_app/refs/heads/main/assets/dns/$country.json \
	    || echo "DNSTT server for $country failed to update"
done
