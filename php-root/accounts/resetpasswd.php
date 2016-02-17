<?php
  error_reporting(E_ALL);
  ini_set('display_errors','On');

  define("MIN_RESET_DELAY", 60); // Minimally 60 seconds between reset attempts

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  function showResetScreen($extra=NULL) {
    darwinHeader("Please give your username");
    darwinDialogStart("Give your username");
    if($extra!==NULL) {
      echo "<div class='warning'>".$extra."</div>\n";
    }
    echo "<form method='POST' acceptCharset='utf8'>\n";
    echo "  <div class='centerContents'><p style='width:22em;'>Please provide your BU username such that a reset email can be sent to your email address.</p></div>\n";
    echo "  <table style='border:none'>\n";
    echo "    <tr><td for='#user'><label>Username:</label></td><td><input name='user' type='text' />@bournemouth.ac.uk</td></tr>\n";
    echo "  </table><span style='margin-top:1em;float:right;'>";
    echo     "<input type='button' value='Cancel' onclick='window.open(\"login\")'/>";
    echo     "<input type='submit' value='Reset'/></span>\n</form>";
    darwinDialogEnd();
    darwinFooter();
  }

  $user = getDarwinUser();
  if ($user!==NULL && (!is_admin())) {
  	// Short circuit resetting password when user is logged in.
    header('Location: /accounts/login');
    exit(); // Finished
  }

  if (isset($_GET["resettoken"]) && strlen($_POST["resettoken"])>0) {
  	header('Location: /accounts/chpasswd?resettoken='.$_GET["resettoken"]);
    exit(); // If there is a reset token, redirect to chpasswd.
  }
  if (isset($_POST["user"]) && strlen($_POST["user"])>0) {
    $user=$_POST["user"];
  }


  if (! (isset($user))) {
    showResetScreen();
    exit();
  }

  // handle the case that we got the username.

  $db = getAuthDb();
  if ($db===NULL) {
    handleError("Could not connect to the database");
  }

  $stmt= checkPrepare($db, "SELECT UNIX_TIMESTAMP()-UNIX_TIMESTAMP(`resettime`) FROM `users` WHERE `user`=?");
  checkBindParam($db, $stmt, "s", $user);
  checkBindResult($db, $stmt, $resettime);
  checkExecute($db,$stmt);

  $result=$stmt->fetch();
  if ($result===FALSE) {
  	stmtError($db, $stmt);
  } elseif ($result===NULL) {
  	$stmt->close();
  	$db->close();
  	showResetScreen("Invalid user");
  	exit();
  }
  $stmt->close();

  if ($resettime!==NULL && $resettime<MIN_RESET_DELAY) {
  	$db->close();
		handleError("Only one reset attempt allowed per ".MIN_RESET_DELAY."seconds");
  }
  $db->autocommit(FALSE);

  $fp = fopen('/dev/urandom', 'rb');
  if ($fp!==FALSE) {
		$token = bin2hex(fread($fp, 10)); // 20 characters
		fclose($fp);
  } else {
  	$db->close();
  	handleError("urandom not available");
  	$token = bin2hex(mt_rand());
  }

  $stmt = checkPrepare($db, "UPDATE `users` SET `resettoken`=?, `resettime`=NOW() WHERE `user`=?");
  checkBindParam($db, $stmt, "ss", $token, $user);
  checkExecute($db, $stmt);
  if ($stmt->affected_rows!=1) {
  	$stmt->close();
  	$db->rollback();
  	$db->close();
  	handleError("Updating reset token affected ".$stmt->affected_rows." rows");
  	exit();
  } else {
  	$db->commit();
  }
  $db->close();

  $reseturl = 'https://darwin.bournemouth.ac.uk/accounts/chpasswd?user='.$user.'&resettoken='.$token;
  $mailbody = "<html><head><title>Darwin account password reset</title></head><body>\n".
      '<p>Please visit <a href="'.$reseturl.'">'.$reseturl."</a> to reset your password.</p>\n".
      '<p>This token will be valid for 30 minutes. If you didn'."'t initiate the reset,\n you can safely ignore this message</p>\n</body></html>";

  if(mail($user."@bournemouth.ac.uk",'Darwin account password reset',$mailbody,"From: Darwin Automated Admin<no-reply@darwin.bournemouth.ac.uk>\nContent-Type: text/html; charset=utf8")) {
//  if(mail(getDarwinEmail(), 'Darwin account password reset', $mailbody, "From: Darwin Automated Admin<no-reply@darwin.bournemouth.ac.uk>\nContent-Type: text/html; charset=utf8")) {
    darwinHeader("Reset request sent");
    darwinDialogStart("Reset request sent");
    echo "<div>A reset token has been sent to your email address. Please follow the instructions in the email.</div>\n";
    darwinDialogEnd();
    darwinFooter();
  } else {
    handleError("Failure to send reset token");
  }

