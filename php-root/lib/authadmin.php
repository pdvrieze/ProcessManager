<?php
  require_once 'authbase.php';

  define('MAX_RESET_VALIDITY', 1800);

  $authdbuser='webauthadm';
  $authdbpasswd='setthistothedbadminpassword';

  /**
   * @param mysqli $db
   * @param string $user
   * @param string $requestip
   * @param int $keyid
   * @return string;
   */
  function getauthtoken($db, $user, $requestip, $keyid=NULL) {
    $epoch=time();
    cleanTokens($db, $epoch);
    $token=str_replace("=", "", base64_encode(openssl_random_pseudo_bytes(32)));
    $token=str_replace("+","-",str_replace("/","_",$token));
    if($stmt=$db->prepare('INSERT INTO tokens (`user`, `ip`, `keyid`, `token`, `epoch`) VALUES (?,?,?,?,?)')) {
      $stmt->bind_param("ssisi", $user, $requestip, $keyid, $token, $epoch);
      if ($stmt->execute()) {
        $db->commit();
        $stmt->close();
        return $token;
      } else {
        $db->rollback();
      }
    }
    $stmt->close();
    $db->close();
    handleError("Could not get auth token: ".$stmt->error);

    return NULL;
  }

  /**
   * @param mysqli $db
   */
  function updateauthtoken($db) {
    if(isset($_COOKIE['DWNID'])) {
      $cookie=$_COOKIE['DWNID'];
      if($stmt=checkPrepare($db, "UPDATE `tokens` SET `epoch`=UNIX_TIMESTAMP() WHERE token=?")) {
        checkBindParam($db, $stmt, "s", $cookie);
        if($stmt->execute()) {
          $db->commit();
        } else {
          $db->rollback();
        }
        $stmt->close();
      }
    }
  }

  /**
   * Verify the credentials given
   * @param mysqli $db
   * @param string $username
   * @param string $resettoken
   */
  function verifyResetToken($db, $username, $resettoken) {
  	if($stmt = checkprepare($db, 'SELECT UNIX_TIMESTAMP()-UNIX_TIMESTAMP(`resettime`) AS `age` FROM `users` WHERE `user`=? AND `resettoken`=?')) {
	  checkBindParam($db, $stmt, "ss", $username, $resettoken);
	  checkBindResult($db, $stmt, $age);
  	  if(checkExecute($db, $stmt)) {
  	  	$result=$stmt->fetch();
  	  	$stmt->close();
  	  	if ($result===True) {
  	  	  return $age < MAX_RESET_VALIDITY;
  	  	} else {
  	  	  return False;
  	  	}
  	  }
  	  $stmt->close();
    }
    return False;
  }

  /**
   * @param string $password
   * @return string
   */
  function createPasswordHash($password) {
  	return "{SHA}".base64_encode(sha1($password,true));
  }

  /**
   * Verify the credentials given
   * @param mysqli $db
   * @param string $username
   * @param string $password
   */
  function verifyCredentials($db, $username, $password) {
    $passwordhash=createPasswordHash($password);

    if (($stmt=$db->prepare('SELECT user FROM users WHERE `user`=? AND `password`=?'))!==FALSE) {
      if (!$stmt->bind_param("ss", $username, $passwordhash)) {
        handleError($db->error);
      }
      if (($result = $stmt->execute())===FALSE) {
        $error=$db->error;
        $stmt->close();
        $db->close();
        handleError($error, 500);
      }
      $stmt->bind_result($user);
      $result=$stmt->fetch();
      if($result===FALSE) {
        $error=$db->error;
        $stmt->close();
        $db->close();
        handleError($error, 500);
      } else if ($result===NULL) {
        $stmt->close();
//         $db->close();
        return FALSE;
      }
      $stmt->close();
      return TRUE;
    } else {
      $error=$db->error;
      $db->close();
      handleError($error, 500);
    }
    // should not be reached, but fail verification anyway
    return FALSE;
  }

  /**
   * Update the credentials for the given user.
   * @author pdvrieze
   * @param mysqli $db The database connection.
   * @param string $user The user whose password to update.
   * @param string $newpassword The new password
   */
  function updateCredentials($db, $user, $newpassword) {
    $passwordhash=createPasswordHash($password);;
    if($stmt=$db->prepare('UPDATE `users` SET `password` = ? WHERE `user` = ?')) {
      if (!$stmt->bind_param("ss", $passwordhash, $user)) {
        handleError($db->error);
      }
      if ($stmt->execute()!==False) {
        $db->commit();
        return TRUE;
      } else {
        $db->rollback();
        handleError("Error updating password");
      }
    }
    return FALSE;
  }
