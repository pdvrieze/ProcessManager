<?php
  error_reporting(E_ALL);
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authbase.php';

  function showSuccessScreen() {
    darwinHeader("Bye", "Bye - Logged out");
    echo "<p>You have successfully logged out</p>";
    darwinFooter();
  }

  $HEADERS = getallheaders();
  $accept=$HEADERS["Accept"];
  if (strpos($accept, "text/html")===False) {
    $htmloutput=false;
    header("Content-type: text/plain");
  } else {
    $htmloutput=true;
  }



  $MAXTOKENLIFETIME=1800; // Tokens remain valid for half hour

  $user = getDarwinUser();
  if ($user!==NULL) {
    if (isset($_COOKIE[$DARWINCOOKIENAME])) {
      $authtoken=str_replace(" ", "+", $_COOKIE[$DARWINCOOKIENAME]);
    }
    unset($_COOKIE[$DARWINCOOKIENAME]);
    $cookieexpire=1;
    if (isset($_SERVER['HTTP_HOST'])) {
      $host=$_SERVER['HTTP_HOST'];
      $secure= $host!='localhost';
      if (! $secure) {
        $host=NULL;
      }
    } else {
      $host='darwin.bournemouth.ac.uk';
      $secure=TRUE;
    }
    // Actually unset the cookie
    setrawcookie($DARWINCOOKIENAME, '', $cookieexpire, '/', $host, $secure);
    if (isset($authtoken)) {
      $db=getAuthDb();
      $requestip=$_SERVER["REMOTE_ADDR"];
      $stmt=checkPrepare($db, 'DELETE FROM `tokens` WHERE `ip`=? AND `token`=?');
      checkBindParam($db, $stmt, "ss", $requestip, $authtoken);
      checkExecute($db, $stmt);
      $stmt->close();
      $db->commit();
      cleanTokens($db);
      $db->close();
    }

    // Whatever happens set the user for the rest of the page to null.
    setDarwinUser(NULL);
    if (isset($_REQUEST['redirect'])) {
      header('Location: '.$_REQUEST['redirect']);
      exit(); // Finished
    } else {
      if ($htmloutput) {
        showSuccessScreen();
      } else {
        echo "logout:$user";
      }
    }
  } else {
    if (isset($_REQUEST['redirect'])) {
      header('Location: '.$_REQUEST['redirect']);
      exit(); // Finished
    } else {
      if ($htmloutput) {
        darwinHeader("Not logged in", "You were not logged in.");
        darwinFooter();
      } else {
        echo "error:Not logged in";
      }
    }
  }