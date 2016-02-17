<?php
  error_reporting(E_ALL);
  error_log(__FILE__. ": Trying to log in user");
  ini_set('display_errors','On');

  require_once '../lib/common.php';
  require_once '../lib/authadmin.php';

  function showSuccessScreen($db, $user) {
    updateauthtoken($db);
    if (isset($_REQUEST['redirect'])) {
      error_log(__FILE__.": redirecting");
      header('Location: '.$_REQUEST['redirect']);
      echo "Redirect!\n";
    }
    setDarwinUser($user);
    darwinHeader("Welcome", "Welcome - Login successful");
    echo "<p>Congratulations with successfully authenticating on darwin.</p>";
    darwinFooter();
  }


  $user = getDarwinUser();
  if ($user===NULL) {
    header('Location: /accounts/login?redirect=/accounts/myaccount');
    exit(); // Finished
  }

  darwinHeader("My account");
?>

<table class="action"><tbody><tr><td class="ac_icon" style="text-align:center"><div><a href="/accounts/chpasswd"><object
 data="/assets/chpasswd.svg" type="image/svg+xml" width="500" height="500"><img src="/assets/chpasswd.png" width="148" height="148"/></object></a></div><div class="attribution"><div><a rel="license" href="http://creativecommons.org/licenses/by/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by/3.0/80x15.png" /></a></div><div>by <a href="https://www.iconfinder.com/designmodo">Designmodo</a></div></div>
 </td><td class="ac_label"><a href="/accounts/chpasswd">Change password</a></td></tr>
</tbody></table>

<?php
darwinFooter();