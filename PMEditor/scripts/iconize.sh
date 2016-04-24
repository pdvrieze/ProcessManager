#!/bin/bash

SRC=$3
DST=$4
if [ $# -gt 4 ]; then
  ID="-j -i $5"
else
  ID=""
fi

CORRECT=0
QUARTW=$(( $1 / 4 ))
QUARTH=$(( $2 / 4 ))
if [ "${DST/drawable*-nodpi}" != "${DST}" ]; then
	((WIDTH = $(basename "$DST"|sed -e "s,.*_\([0-9][0-9]*\)\.[^\.]*$,\1,") ))
	((HEIGHT = ( $2 * $WIDTH ) / $1 ))
elif [ "${DST/drawable*-ldpi}" != "${DST}" ]; then
	CORRECT=-1
	((WIDTH = ( $CORRECT * $QUARTW ) + $1 ))
	(( HEIGHT= ( $CORRECT * $QUARTH ) + $2 ))
elif [ "${DST/drawable*-hdpi}" != "${DST}" ]; then
	CORRECT=2
	((WIDTH = ( $CORRECT * $QUARTW ) + $1 ))
	(( HEIGHT= ( $CORRECT * $QUARTH ) + $2 ))
elif [ "${DST/drawable*-tvdpi}" != "${DST}" ]; then
	CORRECT=213
	((WIDTH = ( $CORRECT * $1 +80 ) /160 ))
	(( HEIGHT= ( $CORRECT * $2 +80 ) /160 ))
elif [ "${DST/drawable*-xhdpi}" != "${DST}" ]; then
	CORRECT=4
	((WIDTH = ( $CORRECT * $QUARTW ) + $1 ))
	(( HEIGHT= ( $CORRECT * $QUARTH ) + $2 ))
elif [ "${DST/drawable*-xxhdpi}" != "${DST}" ]; then
	CORRECT=8
	((WIDTH = ( $CORRECT * $QUARTW ) + $1 ))
	(( HEIGHT= ( $CORRECT * $QUARTH ) + $2 ))
elif [ "${DST/drawable*-xxxhdpi}" != "${DST}" ]; then
	CORRECT=12
	((WIDTH = ( $CORRECT * $QUARTW ) + $1 ))
	(( HEIGHT= ( $CORRECT * $QUARTH ) + $2 ))
else
	WIDTH=$1
	HEIGHT=$2
fi

echo "Creating icon ${DST}"
if [ "${DST/.xml}" != "${DST}" ]; then
  mkdir -p "$(dirname "${DST}")"
  cat <<EOF > "${DST}"
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/$(basename ${DST/.xml/_${WIDTH}}|sed -e "s,^btn_,peg_,")">
</bitmap>
EOF
else
  inkscape -w $WIDTH -h $HEIGHT -e $DST $ID $SRC
  exec optipng "${DST}"
fi

