#!/usr/bin/php
<?php
  
  if ($argc != 6) {
    print("usage: $argv[0] <hsizeindp> <vsizeindp> <sourcesvg> <sourceid> <destfile>\n");
    exit(1);
  }

  function filedpi($destfile) {
    if (strpos($destfile,'drawable-ldpi')!==FALSE) {
      return 120;
    } elseif (strpos($destfile,'drawable-tvdpi')!==FALSE) {
      return 213;
    } elseif (strpos($destfile,'drawable-hdpi')!==FALSE) {
      return 240;
    } elseif (strpos($destfile,'drawable-xhdpi')!==FALSE) {
      return 320;
    } elseif (strpos($destfile,'drawable-xxhdpi')!==FALSE) {
      return 480;
    } elseif (strpos($destfile,'drawable-xxxhdpi')!==FALSE) {
      return 640;
    } else {
      return 160;
    }
  }

  function dp2px($size, $dpi) {
    return round(($dpi/160)*$size);
  }

  $hsizedp=$argv[1];
  $vsizedp=$argv[2];
  $source=$argv[3];
  $imgid=$argv[4];
  $dest=$argv[5];
  $dpi=filedpi($dest);
  print("Using dpi: ".$dpi."\n");

  $hsize=dp2px($hsizedp,$dpi);
  $vsize=dp2px($vsizedp,$dpi);

#  $tmparrow=tempnam(null, "arrowgen-");
  $tmparrow=$dest;
  $inkscapecmd="inkscape -w $hsize -h $vsize -j -i $imgid -e $tmparrow \"$source\"";
  exec($inkscapecmd);

#
#  $tmpback=tempnam(null, "arrowgen-");
#  $inkscapecmd="inkscape -w $hsize -h $vsize -j -i $imgid -e $tmpback \"$source\"";
#  exec($inkscapecmd);
#



?>
