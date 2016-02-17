<?php
  error_reporting(E_ALL);
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';
  $AUTHONLY=! isset($_POST["pubkey"]);

  if (! (isset($_POST["username"]) && isset($_POST["password"]))) {
    handleError("insufficient credentials", 403, "Forbidden");
  }

  $USERNAME=$_POST["username"];
  $PASSWORD=$_POST["password"];

  $DB=getAuthDb();
  if ($DB===NULL) {
    handleError("Database connection error", 500);
  }

  $DB->autocommit(FALSE);


  if (verifyCredentials($DB, $USERNAME, $PASSWORD)!==True) {
    handleError("Invalid credentials", 403, "Forbidden");
    exit(); // Exit just for certainty. HandleError should have exited already.
  }

  if($AUTHONLY) {
    header("HTTP/1.1 200 Created");
    header("Content-type: text/plain");
    print("authenticated $USERNAME\n");
    exit();

  }

  // Now we are authenticated. Now add the key

  if (isset($_POST["id"])) {
    $REQUESTKEYID=$_POST["id"];
  }
  $PUBKEY=normalizebase64($_POST["pubkey"]);

  if (isset($REQUESTKEYID)) {
    if ($stmt=$DB->prepare('UPDATE `pubkeys` SET privkey=? WHERE `keyid`=? AND `user`=?')) {
      $stmt->bind_param("sis", $PUBKEY, $REQUESTKEYID, $USERNAME);
      if (($result = $stmt->execute())===FALSE) {
        $error=$DB->error;
        $stmt->close();
        $DB->rollback();
        $DB->close();
        handleError($error, 500);
      } elseif ($result===NULL || $DB->affected_rows!=1) {
        $stmt->close();
        $DB->rollback();
        $DB->close();
        handleError("The requested keyid is not valid", 400, "Bad request");
      }
      $stmt->close();
      $DB->commit();
    	header("HTTP/1.1 200 Created");
      header("Content-type: text/plain");
      print("key: $REQUESTKEYID\n");
    } else {
      $error=$DB->error;
      $DB->close();
      handleError($error, 500);
    }
  } else {
    if ($stmt=$DB->prepare('INSERT INTO `pubkeys` ( user, privkey ) VALUES ( ?, ?)')) {
      $stmt->bind_param("ss", $USERNAME, $PUBKEY);
      if (($result = $stmt->execute())===FALSE) {
        $error=$DB->error;
        $stmt->close();
        $DB->rollback();
        $DB->close();
        handleError($error, 500);
      }
      $resultid=$DB->insert_id;
      $stmt->close();
      $DB->commit();
    	header("HTTP/1.1 201 Created");
      header("Content-type: text/plain");
      print("key: $resultid\n");
    } else {
      $error=$DB->error;
      $DB->close();
      handleError($error, 500);
    }
  }

  $DB->close();
