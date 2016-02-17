<?php
  error_reporting(E_ALL);
  error_log(__FILE__. ": Trying to log in user");
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  function presentLoginScreen($errorMsg=NULL) {
    darwinHeader("Please log in");
    darwinDialogStart("Log in");
    echo "<form method='POST' acceptCharset='utf8' action='login.php'>\n";
    if (isset($_REQUEST['redirect'])) {
      echo "  <input name='redirect' type='hidden' value='".htmlentities($_REQUEST['redirect'])."'/>\n";
    }
    echo "  <table style='border:none'>\n";
    echo "    <tr><td><label for='#username'>User name:</label></td><td><input name='username' type='text'";
    if (isset($_REQUEST['username'])) {
      echo " value='".htmlentities($_REQUEST['username'])."'";
    }
    echo "/></td></tr>\n";
    echo "    <tr><td><label for='#password'>Password:</label></td><td><input name='password' type='password' /></td></tr>\n";
    echo "  </table><span style='margin-top:1em;float:right;'>";
    echo     "<input type='submit' value='Log in'/></span>\n";
    echo "<div id='forgotpasswd'><a href='/accounts/resetpasswd'>Forgot password</a></div>";

    darwinDialogEnd();
    darwinFooter();
  }

  function showSuccessScreen($db, $user) {
    updateauthtoken($db);
    if (isset($_REQUEST['redirect'])) {
      error_log(__FILE__.": redirecting");
      header('Location: '.$_REQUEST['redirect']);
      echo "Redirect!\n";
      exit();
    }
    setDarwinUser($user);
    darwinHeader("Welcome", "Welcome - Login successful");
    echo "<p>Congratulations with successfully authenticating on darwin.</p>";
    darwinFooter();
  }


//  $MAXTOKENLIFETIME=86400; // Tokens remain valid for one day on the client side (becomes invalid after half hour of inactivity on the server)
  $MAXTOKENLIFETIME=864000; // Tokens remain valid for ten days on the client side (becomes invalid after half hour of inactivity on the server)

  error_log(__FILE__.": Getting user");
  $user = getDarwinUser();
  error_log(__FILE__.": Got user $user");
  $HEADERS = getallheaders();
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

  if ($user!==NULL) {
    if ($htmloutput) {
      $db=getAuthDb();
      showSuccessScreen($db, $user);
    } else {
      echo "login:$user\n";
    }
  } elseif (isset($_REQUEST['username']) && isset($_REQUEST['password'])) {

    if ($db=getAuthDb()) {
      error_log(__FILE__.": Got a database connection");
      if (verifyCredentials($db, $_REQUEST['username'], $_REQUEST['password'])===True) {
        error_log(__FILE__.": verified credentials");
        $authtoken = getauthtoken($db, $_REQUEST['username'], $_SERVER["REMOTE_ADDR"]);
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
        error_log(__FILE__.": The host for the cookie has been determined as '$host'");

        setrawcookie($DARWINCOOKIENAME, $authtoken, $cookieexpire, '/', $host, $secure);

        error_log(__FILE__.": Cookie set.");

        if (isset($_REQUEST['redirect'])) {
          error_log(__FILE__.": redirecting");
          header('Location: '.$_REQUEST['redirect']);
          echo "Redirect!\n";
        } else {
          if ($htmloutput) {
            error_log(__FILE__.": Showing success screen");
            showSuccessScreen($db, $_REQUEST['username']);
          } else {
            error_log(__FILE__.": Setting success");
            echo "login:".$_REQUEST['username']."\n";
            for ($x=0; $x<0;$x++) {
              echo "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n";
            }
//             flush();
          }
        }
      } else {
        error_log(__FILE__.": user info incorrect");
        if ($htmloutput) {
          presentLoginScreen('Username or password not correct');
        } else {
          http_response_code(401);
          echo "invalid:Invalid credentials";
        }
      }
      error_log(__FILE__.": Cleaning tokens");
      $epoch=time();
      cleanTokens($db,$epoch);
      error_log(__FILE__.": Closing database");
      $db->close();
      exit();

    } else {
      error_log("Could not get database connection");
      handleError("Could not connect to the database");
    }
  } else {
    error_log("Login system needs user info");
    if ($htmloutput) {
      presentLoginScreen();
    } else {
      http_response_code(401);
      print("error:Login is required\n");
    }
  }
  error_log(__FILE__.": Finished");

  $db->close();
