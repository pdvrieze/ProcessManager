<?php
  require_once 'lib/authbase.php';
  darwinHeader("Darwin", "Welcome");
  darwinDialogStart("loading", "banner");
  echo '<img src="assets/progress_large.gif" width="192" height="192" />';
  darwinDialogEnd();
?>
  <form id="xloginform" style="display:none;" method='POST' accept-charset='utf8' action='/accounts/login'>
    <table style='border:none'>
      <tr><td><label for='#username'>User name:</label></td><td><input name='username' type='text' /></td></tr>
      <tr><td><label for='#password'>Password:</label></td><td><input name='password' type='password' /></td></tr>
    </table>
    <input type='hidden' name="redirect" value="/"/>
    <span style='margin-top:1em;float:right;'><!--
      --><input type='button' value='Cancel' name="cancel"/><!--
      --><input type='button' value='Log in' name="login"/></span>
        <div id='forgotpasswd'><a href='/accounts/resetpasswd'>Forgot password</a></div>
      </form>
<?php
  darwinFooter();
?>
