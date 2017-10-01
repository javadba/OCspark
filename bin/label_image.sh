#!/usr/bin/env bash
CURDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Tag=$2"
$CURDIR/label_image $1 2>&1 | grep -v "W tensorflow"
#OUTF1=$$
#OUTF=$OUTF1.log
#$CURDIR/label_image > $OUTF 2>&1  &
#tail -n 70 -f $OUTF | grep -v "W tensorflow"
