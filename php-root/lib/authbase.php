<?php

require_once 'common.php';

$MAXTOKENLIFETIME=1800; // Tokens remain valid for half hour
$DARWINCOOKIENAME='DWNID';
$authdbuser='webauth';
$authdbpasswd='setthistothedbauthenticationpassword';



/**
 * @param mysqli $db
 * @param int $epoch
 */
function cleanTokens($db, $epoch=NULL) {
  global $MAXTOKENLIFETIME;
  if ($epoch===NULL) {
    $epoch=time();
  }
  $cutoff=$epoch-$MAXTOKENLIFETIME;

  $stmt=checkPrepare($db, 'DELETE FROM tokens WHERE `epoch` < ?');
  checkBindParam($db, $stmt, 'i', $cutoff);
  if ($stmt->execute()) {
    $db->commit();
  } else {
    $db->rollback();// Don't report, this is just maintenance.
  }
  $db->commit();
}

/**
 * @param string $base binary value to decode
 * @param string $key key as string
 * @return string The decrypted value
 * @return the decrypted value
 */
function rsadecrypt($base, $key) {
  $pos=strpos($key, ':');

  $base=byterangetonumber($base);
  $modulus=byterangetonumber(base64_decode(substr($key,0,$pos)));
  $publicexponent=byterangetonumber(base64_decode(substr($key,$pos+1)));

  $result = bcpowmod($base, $publicexponent, $modulus);
  return numbertobyterange($result);
}

/**
 * Convert a byte range (as string) to a number
 * @param string $orig
 * @return string
 */
function byterangetonumber($orig) {
  $length = strlen($orig);
  $result='';
  $base=ord('0');

  for ($i = 0; $i < $length; $i++) {
     $number[$i] = ord($orig{$i});
  }

  do {
    $divide = 0;
    $newlen = 0;
    for ($i = 0; $i < $length; $i++) {
      $divide = $divide * 256 + $number[$i];
      if ($divide >= 10) {
        $number[$newlen++] = (int)($divide / 10);
        $divide = $divide % 10;
        } elseif ($newlen > 0) {
          $number[$newlen++] = 0;
        }
      }
      $length = $newlen;
      $result = chr($divide+$base) . $result;
    } while ($newlen != 0);
  return $result;

}

/**
 * Convert a number to a byte range (as string)
 * @param string $orig
 * @return string
 */
function numbertobyterange($orig) {
  $length = strlen($orig);
  $result='';
  $base=ord('0');

  for ($i = 0; $i < $length; $i++) {
    $number[$i] = ord($orig{$i})-$base;
  }

  do {
    $divide = 0;
    $newlen = 0;
    for ($i = 0; $i < $length; $i++) {
      $divide = $divide * 10 + $number[$i];
      if ($divide >= 256) {
        $number[$newlen++] = (int)($divide / 256);
        $divide = $divide % 256;
      } elseif ($newlen > 0) {
        $number[$newlen++] = 0;
      }
    }
    $length = $newlen;
    $result = chr($divide) . $result;
  } while ($newlen != 0);
  return $result;

}

/**
 * Get an admin capable database connection. This can write.
 * @return mysqli
 */
function getAuthDb() {
  global $authdbpasswd;
  global $authdbuser;

  if(! class_exists('mysqli')) {
    handleError("No support for mysqli in the PHP installation");
  }

  if (!(isset($authdbpasswd) && isset($authdbuser))) {
    handleError("Database user not set");
  }

  $result=new mysqli(NULL, $authdbuser, $authdbpasswd, 'webauth', 3306);
  if ($result===NULL || mysqli_connect_error()) {
    handleError("Could not establish database connection".mysqli_connect_error());
  }
  $error = $result->connect_error;
  if ($error!=NULL) {
    $result->close();
    handleError("Database connection error: ".$error);
  }
  return $result;
}

/**
 * Determine the user logged in.
 * @return string The user logged in or FALSE if none.
 */
function getDarwinUser() {
  global $DARWIN__USER;

  if (isset($_SERVER['AP_MAD_UID'])) {
    setDarwinUser($_SERVER['AP_MAD_UID']);
    return $_SERVER['AP_MAD_UID'];
  }

  if (!isset($_COOKIE['DWNID'])) {
    if (isset($DARWIN__USER) && ($DARWIN__USER!==False) && $DARWIN__USER!==Null) {
      return $DARWIN__USER; // Set it to the value as we might be just logged in.
    } else {
      return NULL; // No cookie, no history, not logged in.
    }
  }
  $authtoken=str_replace(" ", "+", $_COOKIE['DWNID']);

  $db=getAuthDb();
  cleanTokens($db);

  $stmt=checkPrepare($db, "SELECT `user` FROM `tokens` WHERE `ip`=? AND `token`=?");
  checkBindParam($db, $stmt, "ss", $_SERVER["REMOTE_ADDR"], $authtoken);
  checkBindResult($db, $stmt, $user);
  checkExecute($db,$stmt);

  $result=$stmt->fetch();
  if ($result===FALSE) {
    stmtError($db, $stmt);
  }

  $stmt->close();
  $db->close();

  if ($result) {
    setDarwinUser($user);
    return $user;
  } else {
    setDarwinUser(NULL);
    return NULL; // No user
  }

}

/**
 * @return string
 */
function getDarwinEmail() {
  $user = getDarwinUser();
  if ($user===NULL) {
    return NULL;
  } else {
    return $user."@bournemouth.ac.uk";
  }
}

/**
 * @return bool true if the user is an admin, false if not.
 */
function is_admin() {
	return in_array("admin", getDarwinGroups());
}

function getDarwinGroups() {
  global $DARWIN__GROUPS;

  if (isset($DARWIN__GROUPS) && ($DARWIN__GROUPS!==False) && $DARWIN__GROUPS!==Null) {
    return $DARWIN__GROUPS; // Set it to the value as we might be just logged in.
  }
  if (isset($_SERVER['AP_MAD_GIDS'])) {
    $groups = preg_split("/,/", $_SERVER['AP_MAD_GIDS']);
    setDarwinUser($groups);
    return $groups;
  }

  $user = getDarwinUser();

  if ($user===NULL) {
    return array(); //No user, no groups
  }

  $db=getAuthDb();
  cleanTokens($db);

  $stmt=checkPrepare($db, "SELECT `role` FROM `user_roles` WHERE `user`=?");
  checkBindParam($db, $stmt, "s", $user);
  checkBindResult($db, $stmt, $role);
  checkExecute($db,$stmt);

  $result=array();

  while($stmt->fetch()) {
    $result[]=$role;
  }
  if (count($result)>0) {
    setDarwinGroups($result);
    return $result;
  }
  setDarwinGroups(NULL);
  return array();

}

?>
