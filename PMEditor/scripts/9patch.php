#!/usr/bin/php
<?php
  
  if ($argc != 14) {
    print("usage: $argv[0] <hsizeindp> <vsizeindp> <sourcesvg> <sourceid> <destfile> <offx1> <lenx1> <offy1> <leny1> <offx2> <lenx2> <offy2> <leny2>\n");
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

  $offx1=dp2px($argv[6],$dpi)+1; $lenx1=dp2px($argv[7],$dpi);
  $offy1=dp2px($argv[8],$dpi)+1; $leny1=dp2px($argv[9],$dpi);

  $offx2=dp2px($argv[10],$dpi)+1; $lenx2=dp2px($argv[11],$dpi);
  $offy2=dp2px($argv[12],$dpi)+1; $leny2=dp2px($argv[13],$dpi);

  $hsize=dp2px($hsizedp,$dpi);
  $vsize=dp2px($vsizedp,$dpi);

  $tmpframe=tempnam(NULL, "9patch-");
  $inkscapecmd="inkscape -w $hsize -h $vsize -j -i $imgid -e $tmpframe \"$source\"";
  exec($inkscapecmd);

  $srcimg=imagecreatefrompng($tmpframe);

  $dstimg=imagecreatetruecolor($hsize+2, $vsize+2);
  imagealphablending($dstimg, FALSE);
  imagesavealpha($dstimg, TRUE);

  $clr_transparent=imagecolorallocatealpha($dstimg,255,255,255,127);
  imagefilledrectangle($dstimg,0,0, $hsize+2, $vsize+2,$clr_transparent);

  $clr_black=imagecolorallocate($dstimg, 0,0,0);

  imagecopy($dstimg, $srcimg, 1, 1, 0, 0, $hsize, $vsize);

  imageline($dstimg, $offx1, 0, $offx1+$lenx1-1,0, $clr_black);
  imageline($dstimg, 0, $offy1, 0, $offy1+$leny1-1, $clr_black);

  imageline($dstimg, $offx2, $vsize+1, $offx2+$lenx2-1,$vsize+1, $clr_black);
  imageline($dstimg, $hsize+1, $offy2, $hsize+1, $offy2+$leny2-1, $clr_black);

  imagepng($dstimg, $dest);
  unlink($tmpframe);


?>
