<?php
  error_reporting(E_ALL);
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  function showChangeScreen($extra=NULL, $resettoken = False, $user=NULL) {
    darwinHeader("Please provide new password");
    darwinDialogStart("Change password");
    if($extra!==NULL) {
      echo "<div class='warning'>".$extra."</div>\n";
    }
    echo "<form method='POST' acceptCharset='utf8'>\n";
    echo "  <table style='border:none'>\n";
    if ($resettoken!==FALSE) {
    	echo "    <input type='hidden' value='$resettoken'/>\n";
	  	echo "    <tr><td><label for='#user'>Username:</label></td><td><input name='username' type='text' disabled value='$user'></td></tr>\n";
   	} else {
	    if (is_admin()) {
	    	echo "    <tr><td><label for='#user'>Username:</label></td><td><input name='username' type='text'></td></tr>\n";
	    } else {
	    	echo "    <tr><td><label for='#password'>Current password:</label></td><td><input name='password' type='password'></td></tr>\n";
	    }
    }
    echo "    <tr><td><label for='#newpassword'>New password:</label></td><td><input name='newpassword' type='password'></td></tr>\n";
    echo "    <tr><td><label for='#newpassword2'>Repeat new password:</label></td><td><input name='newpassword2' type='password'></td></tr>\n";
    echo "  </table><span style='margin-top:1em;float:right;'>";
    echo     "<input type='submit' value='Change'/>\n</form>";
    if($extra!==NULL) {
    }
    darwinDialogEnd();
    darwinFooter();
  }

  if (isset($_REQUEST["resettoken"]) && strlen($_REQUEST["resettoken"])) {
    $resettoken = $_REQUEST["resettoken"];
  } else {
    $user = getDarwinUser();
    if ($user===NULL) {
      header('Location: /accounts/login?redirect=/accounts/chpasswd');
      exit(); // Finished
    }
  }

  if (isset($_POST["newpassword"]) && strlen($_POST["newpassword"])>0) {
    $newpassword=$_POST["newpassword"];
  }
  if (isset($_POST["newpassword2"]) && strlen($_POST["newpassword2"])>0) {
    $newpassword2=$_POST["newpassword2"];
  }
  if (isset($_POST["password"]) && strlen($_POST["password"])>0) {
    $password=$_POST["password"];
  }
  if (isset($_REQUEST["user"]) && strlen($_REQUEST["user"])) {
  	$requestuser = $_REQUEST["user"];
  }


  if (! (isset($password) || isset($newpassword) || isset($newpassword2) )) {
    if (isset($resettoken)) {
  		showChangeScreen(NULL, $resettoken, $requestuser);
  	} else {
    	showChangeScreen();
  	}
  	    exit();
  } elseif (! ((isset($password) ||isset($requestuser))&& isset($newpassword) && isset($newpassword2) )) {
  	if (isset($requestuser)) {
  		$msg = "Please provide two identical copies of the new password";
  	} else {
  		$msg = "Please provide all of the current password and two copies of the new one";
  	}
  	if (isset($resettoken)) {
  		showChangeScreen($msg, $resettoken);
  	} else {
    	showChangeScreen($msg);
  	}
    exit();
  }
  $db = getAuthDb();
  if ($db===NULL) {
    handleError("Could not connect to the database");
  }
  if (isset($resettoken) && isset($requestuser)) {
    if (verifyResetToken($db, $requestuser, $resettoken)) {
      $user = $requestuser;
    } else {
      handleError("The given reset token is invalid or expired");
      exit();
    }
  } elseif (is_admin() && isset($requestuser)) {
  	$user = $actualuser;
  } else {
	  if (verifyCredentials($db, $user, $password)!==True) {
	    showChangeScreen("Your password is incorrect");
	    exit();
	  }
  }
  if ($newpassword!==$newpassword2) {
    showChangeScreen("The new passwords do not match");
    exit();
  }
  if (strlen($newpassword)<6) {
    showChangeScreen("The new passwords must be at least 6 characters long");
    exit();
  }

  $db->autocommit(FALSE);


  updateCredentials($db, $user, $newpassword);
  $db->close();

  darwinHeader("Password Changed");
  echo '<div>Your password has been changed successfully</div>';
  darwinFooter();

