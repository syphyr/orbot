wget https://gitlab.torproject.org/tpo/applications/tor-browser-build/-/raw/main/projects/tor-expert-bundle/pt_config.json?ref_type=heads
mv "pt_config.json?ref_type=heads" pt_config.json
function bridges_conf {
  local bridge_type="$1"
  jq -r ".bridges.\"$bridge_type\" | .[]" "pt_config.json" | while read -r line; do
    echo $line
    echo
  done
}

function fronts_conf {
  local bridge_type="$1"
  jq -r ".bridges.\"$bridge_type\" | .[]" "pt_config.json" | while read -r line; do
    if [ "$bridge_type" == "meek-azure" ]; then
	    arr=($line)
	    moat_url=$(echo "${arr[2]}" | cut -d "=" -f 2)
	    moat_front=$(echo "${arr[3]}" | cut -d "=" -f 2)
	    echo "moat-url $moat_url"
	    echo "moat-front $moat_front"
	    sed -ri "s|^moat-url .+|moat-url ${moat_url}|" orbotservice/src/main/assets/fronts
	    sed -ri "s|^moat-front .+|moat-front ${moat_front}|" orbotservice/src/main/assets/fronts
	    echo
    fi
    if [ "$bridge_type" == "snowflake" ] && [ "$finished" == "" ]; then
	    arr=($line)
	    snowflake_target=$(echo "${arr[4]}" | cut -d "=" -f 2)
	    snowflake_front=$(echo "${arr[5]}" | cut -d "=" -f 2)
	    snowflake_stun=$(echo "${arr[6]}" | cut -d "=" -f 2)
	    echo "snowflake-target $snowflake_target"
	    echo "snowflake-front $snowflake_front"
	    echo "snowflake-stun $snowflake_stun"
	    sed -ri "s|^snowflake-target .+|snowflake-target ${snowflake_target}|" orbotservice/src/main/assets/fronts
	    sed -ri "s|^snowflake-front .+|snowflake-front ${snowflake_front}|" orbotservice/src/main/assets/fronts
	    sed -ri "s|^snowflake-stun .+|snowflake-stun ${snowflake_stun}|" orbotservice/src/main/assets/fronts
	    echo
	    local finished="true"
    fi
  done
}

bridges_conf "meek-azure"
bridges_conf "snowflake"
fronts_conf "meek-azure"
fronts_conf "snowflake"
rm pt_config.json
