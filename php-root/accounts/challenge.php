<?php
  error_reporting(E_ALL);
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  $MAXCHALLENGELIFETIME=60; // Only keep challenges them 60 seconds

  /**
   * @param mysqli $db
   * @param int $epoch
   */
  function cleanChallenges($db, $epoch=NULL) {
    global $MAXCHALLENGELIFETIME;
    if ($epoch===NULL) {
      $epoch=time();
    }
    $cutoff=$epoch-$MAXCHALLENGELIFETIME;

    if($stmt=$db->prepare('DELETE FROM challenges WHERE `epoch` < ?')) {
      $stmt->bind_param("i", $cutoff);
      if ($stmt->execute()) {
        $db->commit();
      } else {
        $db->rollback();// Don't report, this is just maintenance.
      }
    }
  }

  /**
   * @param mysqli $db
   * @param int $keyid
   * @param string $requestip
   * @param string $challenge
   */
  function storeChallenge($db, $keyid, $requestip, $challenge) {
    $stmt=$db->prepare('CREATE TABLE IF NOT EXISTS challenges ( `keyid` INTEGER, `challenge` VARCHAR(100), `requestip` VARCHAR(24), epoch INT, PRIMARY KEY (`keyid`, `requestip`), FOREIGN KEY (`keyid`) REFERENCES `pubkeys` (`keyid`) ) ENGINE=InnoDB CHARSET=utf8');
    if (!$stmt->execute()) { handleError($db->error); }
    $stmt->close();

    $epoch = time();
    cleanChallenges($db, $epoch);


    if($stmt=$db->prepare('INSERT INTO challenges ( `keyid`, `requestip`, `challenge`, `epoch` ) VALUES ( ?, ?, ?, ? )  ON DUPLICATE KEY UPDATE `challenge`=?, `epoch`=?')) {
      $stmt->bind_param("issisi", $keyid, $requestip, $challenge, $epoch, $challenge, $epoch);
      if ($stmt->execute()) {
        $stmt->close();
        $db->commit();
      } else {
        $stmt->close();

        handleError($db->error);
      }
    } else {
      $db->rollback();
      handleError($db->error);
    }

  }

  function issuechallenge($keyid) {
    $requestip=$_SERVER["REMOTE_ADDR"];

    if (($db=getAuthDb())===NULL) {
      handleError("Database connection error", 500);
    }
    $db->autocommit(FALSE);

    $stmt=$db->prepare('SELECT user FROM pubkeys WHERE keyid=?');
    $stmt->bind_param("i", $keyid);
    $stmt->bind_result($user);
    if($stmt->execute()) {
      if (($result=$stmt->fetch())===TRUE) {
        $stmt->close();
      } elseif ($result===NULL) {
        handleError("Key not found",403,"invalid parameter");
      } else {
        handleError('Failure to get username');
      }
    }

    $random=openssl_random_pseudo_bytes(8);
    $challenge=base64_encode(sha1($requestip.$user.$random,TRUE));

    storeChallenge($db, $keyid, $requestip, $challenge);
    $db->close();
    header("Content-type: text/plain");
    print($challenge);
//     print ("<html><body>Key $keyid belongs to user $user<br/>Challenge:$challenge</body></html>");
  }


  /**
   * @param int $keyid
   * @param string $response
   */
  function handleresponse($keyid, $response) {
    global $DARWINCOOKIENAME;
    global $MAXTOKENLIFETIME;
    if (($db=getAuthDb())===NULL) {
      handleError("Database connection error", 500);
    }
    $db->autocommit(FALSE);
    cleanChallenges($db);

    $stmt=$db->prepare('SELECT `challenge`, `requestip` FROM `challenges` WHERE `keyid`=?');
    $stmt->bind_param("i", $keyid);
    $stmt->bind_result($challenge, $challengeip);
    if (!$stmt->execute()) { handleError($db->error); }

    if (($stmt->fetch()!==TRUE) || ( $challengeip!=$_SERVER["REMOTE_ADDR"])) {
      handleError("Invalid challenge", 403, "Not authorized");
    }
    $stmt->close();

    $stmt=$db->prepare('SELECT `user`, `privkey` FROM `pubkeys` WHERE `keyid`=?');
    $stmt->bind_param("i", $keyid);
    $stmt->bind_result($user, $pubkey);
    $stmt->execute();
    if ($stmt->fetch()===TRUE) {
      $stmt->close();
      $decryptresponse=rsadecrypt($response, $pubkey);
      if ($decryptresponse!==$challenge){
        handleError("Invalid response", 403, "Not Authorized");
//       } else {
//         print("Challenge successfully decrypted: $decryptresponse\n");
      }
      $db->commit();
      $authtoken=getauthtoken($db, $user, $challengeip, $keyid);
      header("HTTP/1.1 200 Success");

      $cookieexpire=time()+$MAXTOKENLIFETIME;
      setrawcookie($DARWINCOOKIENAME, $authtoken, $cookieexpire, '/', 'darwin.bournemouth.ac.uk', TRUE);
      print($authtoken);
    } else {
      $stmt->close();
      handleError("key not found: \"$decryptresponse\"", 403, "Not Authorized");
    }
    $db->close();
  }

  if (isset($_REQUEST['cleanup'])) {
    if ($db=getAuthDb()) {
      $epoch=time();
      cleanChallenges($db, $epoch);
      cleanTokens($db,$epoch);
      header("HTTP/1.1 204 No Content");
      $db->close();
      exit();
    }
  }
  if (!isset($_REQUEST['keyid'])) {
    handleError("insufficient credentials", 403, "Forbidden");
  }
  $keyid=$_REQUEST['keyid'];
  if (isset($_REQUEST['response'])){
    $response=normalizebase64($_REQUEST['response']);
    $responsebin=base64_decode($response);
//     print("Response received: $response");
//     print(", this should make 0x".bin2hex($responsebin)."\n");
    handleresponse($keyid, $responsebin);
  } else {
    issuechallenge($keyid);
  }



