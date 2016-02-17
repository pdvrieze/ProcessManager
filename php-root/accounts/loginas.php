<?php
  error_reporting(E_ALL);
  error_log(__FILE__. ": Trying to log in user");
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  $groups = getDarwinGroups();

  if (!is_admin()) {
    handleError("You are not an administrator", 403, "FORBIDDEN");
    exit();
  }

  if (!isset($_REQUEST['newuser'])) {
    handleError("Darwin - Please provide the new user in the newuser parameter", 400, "Bad Request");
    exit();
  }

  if (isset($HEADERS["Accept"])) {
    $accept=$HEADERS["Accept"];
  } else {
    $accept="";
  }
  if (strpos($accept, "text/html")===False) {
    $htmloutput=false;
    header("Content-type: text/plain");
  } else {
    $htmloutput=true;
  }


  $MAXTOKENLIFETIME=86400; // Tokens remain valid for one day on the client side (becomes invalid after half hour of inactivity on the server)

  if ($db=getAuthDb()) {
    $authtoken = getauthtoken($db, $_REQUEST['newuser'], $_SERVER["REMOTE_ADDR"]);
    $cookieexpire=time()+$MAXTOKENLIFETIME;
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

    setrawcookie($DARWINCOOKIENAME, $authtoken, $cookieexpire, '/', $host, $secure);

    error_log(__FILE__.": Cookie set.");

    if (isset($_REQUEST['redirect'])) {
      error_log(__FILE__.": redirecting");
      header('Location: '.$_REQUEST['redirect']);
      echo "Redirect!\n";
    } else {
      if ($htmloutput) {
        error_log(__FILE__.": Showing success screen");
        showSuccessScreen($_REQUEST['newuser']);
      } else {
        error_log(__FILE__.": Setting success");
        echo "login:".$_REQUEST['newuser']."\n";
        for ($x=0; $x<0;$x++) {
          echo "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n";
        }
      }
    }

    error_log(__FILE__.": Cleaning tokens");
    $epoch=time();
    cleanTokens($db,$epoch);
    error_log(__FILE__.": Closing database");
    $db->close();


  } else {
    error_log("Could not get database connection");
    handleError("Could not connect to the database");
  }
