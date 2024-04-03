if test -d "$TMPDIR"; then
    :
elif test -d "$TMP"; then
    TMPDIR=$TMP
elif test -d /var/tmp; then
    TMPDIR=/var/tmp
else
    TMPDIR=/tmp
fi

TEMPDIR="$TMPDIR/IPtProxy"

if [ -d "$TEMPDIR" ]; then
    rm -rf "$TEMPDIR"
fi

cd OrbotIPtProxy/IPtProxy
git clean -fdx
git reset --hard
cd snowflake
git clean -fdx
git reset --hard
cd ../lyrebird
git clean -fdx
git reset --hard
