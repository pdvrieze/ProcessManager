#!/bin/bash
QUAL="$1"
DP="$2"

if [ "$QUAL" == "ldpi" ]; then
  DPI=120
elif [ "$QUAL" == "tvdpi" ]; then
  DPI=213
elif [ "$QUAL" == "hdpi" ]; then
  DPI=240
elif [ "$QUAL" == "xhdpi" ]; then
  DPI=320
else
  echo "$DP"
  exit
fi
(( PX = ( DP * DPI + 80 ) / 160 ))
echo "$PX"
