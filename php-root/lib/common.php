<?php

  function getAppRoot() {
    global $__MYAPPROOT;
    if (isset($__MYAPPROOT)) {
      return $__MYAPPROOT;
    }
    $calledScript=realpath($_SERVER['SCRIPT_FILENAME']);
    $reference=__FILE__;
    $reference=substr($reference, 0, strlen($reference)-strlen("lib/common.php"));
    if (strpos($calledScript, $reference)===0) {
      $__APPROOT="";
      for($startpos=strpos($calledScript,"/",strlen($reference));$startpos!==FALSE;$startpos=strpos($calledScript,"/",$startpos+1)){
        $__MYAPPROOT=$__MYAPPROOT."../";
      }
      return $__MYAPPROOT;
    }
    return "";
  }

  /**
   * Try to get the user for the app, but don't use the database.
   * @return string The known user, or FALSE if there is none
   */
  function pollDarwinUser($allowDB=TRUE) {
    global $DARWIN__USER;
    if (isset($_SERVER['AP_MAD_UID'])) {
      // Set the user through server variables before trying to get the user ourselves
      $user = $_SERVER['AP_MAD_UID'];
    } else if (isset($_SERVER['PHP_AUTH_USER'])) {
      $user = $_SERVER['PHP_AUTH_USER'];
    } else if(function_exists('getDarwinUser')&& $allowDB) {
      $user = getDarwinUser();
    } else if(isset($DARWIN__USER) && ($DARWIN__USER!==False)) {
      $user = $DARWIN__USER;
    } else {
      $user = NULL;
    }
    setDarwinUser($user);
    return $user;
  }

  /**
   *
   * @return array An array of the groups
   */
  function pollDarwinGroups($allowDB=TRUE) {
    global $DARWIN__GROUPS;
    if(isset($_SERVER['AP_MAD_GIDS'])) {
      $groups = preg_split("/,/", $_SERVER['AP_MAD_GIDS']);
      setDarwinGroups($groups);
    } else if (function_exists('getDarwinGroups') && $allowDB) {
      $groups = getDarwinGroups();
    } else if (isset($DARWIN__GROUPS) && ($DARWIN__GROUPS!==FALSE)) {
      $groups = $DARWIN__GROUPS;
    } else {
      $groups = array();
    }
    if($groups==NULL) {
      return array();
    }
    return $groups;
  }

  function setDarwinGroups($groups) {
    global $DARWIN__GROUPS;
    if (is_array($groups)) {
      if (count($groups)==0) {
        $DARWIN__GROUPS=NULL;
      } else {
        $DARWIN__GROUPS=$groups;
      }
    } elseif (is_null($groups)){
    	$DARWIN__GROUPS=NULL;
    } else {
      handleError(500, "Setting darwin group to invalid type ".var_export($groups,True));
    }
  }

  /**
   * Update the global user.
   * @param string $user
   */
  function setDarwinUser($user) {
    global $DARWIN__USER;
    $DARWIN__USER=$user;
  }

  /**
   * Get the header with the given name
   * @param string $headername
   * @return string value of the header, or NULL if nonexistent
   */
  function getHeader($headername) {
  	foreach(getallheaders() as $header => $value) {
  		if ($header == $headername) {
  			return $value;
  		}
  	}
  	return NULL;
  }

  /**
   *
   * @param string $title Title to use in the browser
   * @param string $pagetitle Tittle to use on the page itself. By default the same as $title
   */
  function darwinHeader($title="Darwin", $pagetitle=NULL, $checkuser=TRUE, $lightweight=FALSE) {
  	if (getHeader("X-Darwin")!==NULL) {
    	header("Content-Type: text/xml");
  	?>
<?xml version="1.0" ?>
<root>
<title><?php echo $title; ?></title>
<body>
<?php
  	} else {
    	header("Content-Type: text/html");
    ?>
<!DOCTYPE html>


<html>
<head>
<title><?php echo $title; ?></title>
<link rel="stylesheet" href="<?php echo getAppRoot();?>css/darwin.css" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <?php
	    if (! $lightweight) {
	      echo '      <script type="text/javascript" src="'.getAppRoot().'darwinjs/darwinjs.nocache.js"></script>'."\n";
	    }
    ?>
  </head>
<body>
  <!--div id="top">  -->
  <h1 id="header">
    <a href="<?php echo getAppRoot()?>" id="logo">Darwin</a><span
      id="title"><?php
       if ($pagetitle===NULL) { echo $title; } else { echo $pagetitle; }
     ?></span>
  </h1>
  <div id="menu">
      <?php
        echo getDarwinMenu(FALSE, !$lightweight);
      ?>
    </div><?php
       if ($checkuser!==FALSE) {
         if ($checkuser!==TRUE) {
           $user=$checkuser; // We have been given the user, not through the cookie/database yet;
         } else {
           $user = pollDarwinUser(!$lightweight);
         }
         if ((!$lightweight) && (($user!==NULL) || function_exists('getDarwinUser'))) {
           if ($user===NULL) {
             echo '    <div id="login"><a href="'.getAppRoot().'/accounts/login.php" id="logout">login</a></div>';
           } else {
             echo '    <div id="login"><a href="'.getAppRoot().'/accounts/myaccount" id="username">'.$user.'</a><span class="hide">
                  </span><a href="'.getAppRoot().'/accounts/logout.php" id="logout">logout</a></div>';
           }
         }
       }
      echo "      <div id='content'>\n";
  	}
  }

  function darwinFooter() {
    if (getHeader("X-Darwin")!==NULL) {
      echo "  </body>\n".
           "</root>";
    } else {
      echo "      </div>\n".
           "    </div><!--\n".
           "    --><div  id=\"footer\"><span id=\"divider\"></span>Darwin is a Bournemouth University Project</div>\n".
           "  </body>\n".
           "</html>\n";
    }
  }

  /**
   * Normalize the incomming base64 string to an unwrapped, not urlsafe base64 string.
   * @param string $in
   * @return string
   */
  function normalizebase64($in) {
    $out='';
    $length = strlen($in);

    for ($i = 0; $i < $length; $i++) {
      $ch=$in{$i};
      if ($ch=='-') {
        $out.='+';
      } elseif ($ch=='_') {
        $out.='/';
      } elseif (ord($ch)>31) { // Ignore control characters
        $out.=$ch;
      }
    }
    return $out;
  }

  function handleError($error, $code=500, $status="Server error") {
    global $__ERROR_HANDLING;
    if ($__ERROR_HANDLING) {
      echo "<div>Error handler called while handling error($code): $error</div>\n";
      return;
    }
    $__ERROR_HANDLING=TRUE;
    header("HTTP/1.1 $code $status");
    darwinHeader("Error: ".htmlentities($code.' '.$status), "Error (".htmlentities($code).")", FALSE, TRUE);

    ?>
        <h2><?php print(htmlentities($status));?></h2>
  <p style="margin-top: 2em">
    <?php
          print("      ".htmlentities(str_replace("\n","<br />\n      ", trim($error)))."\n");
    ?>
        </p>
  <h3>Backtrace</h3>
        <?php
        debug_print_backtrace();
    darwinFooter();
    exit();
  }

  function darwinDialogStart($title="Darwin", $id=NULL) {
    echo '<div class="dialog centerContents"';
    if ($id!==NULL) { echo " id='".$id."'"; }
    echo '>';
    echo '<div class="dialogOuter">';
    echo '<h1 class="dlgTitle">'.$title.'</h1>';
    echo '<div class="dialogInner centerContents">';
    echo '<div class="dlgContent">';
  }


  /**
   * Get the menu for the current user.
   *
   * @param boolean $wrapper If a wrapper is needed either specify the tag content (without brackets) or True for a generic one.
   * @return string The html for the menu content.
   */
  function getDarwinMenu($wrapper=FALSE, $allowDB=TRUE) {
  	define("DISPLAYPMT", FALSE);
    $user = pollDarwinUser($allowDB);
    $groups = pollDarwinGroups($allowDB);

    // Pages with /#/... urls are virtual pages. They don't have valid other urls
    if ((! isset($user)) || $user===Null || $user===False) {
      $menuItems= array(array("Welcome" => "/"), array("About"=> "/#/about"));
    } else {
      $menuItems=array();
      if(in_array("appprogramming", $groups) || is_admin()) {
        $menuItems[] = array("Home" => "/");
        $menuItems[] = array("Trac" => "/$user/trac/");
      }
      if(in_array("websystems", $groups) || is_admin()) {
        $menuItems[] = array("Presentations" => "/#/presentations");
      }
      $menuItems[] = array("About"=> "/#/about");
    }
    foreach ( $groups as $group ) {
      if (substr ( $group, 0, 3 ) == "pmt") {
        if (DISPLAYPMT) {
          $groupno = trim ( substr ( $group, 3 ) );
          $menuItems[] = array ("PMT " . $groupno => "/$group/trac/");
        }
      } else if (substr ( $group, 0, 3) != "itp" ) {
        $menuItems[] = array("$group" => "/$group/trac");
      }
    }
    if ($wrapper === True) {
      $result = '<div id="menu">';
    } else if ($wrapper!==False) {
      $result = '<'.$wrapper.'>';
    } else {
      $result = '';
    }
    foreach($menuItems as $menuItem) {
      foreach ($menuItem as $label => $ref) {
        $result.= '<a class="menuitem" href="'.$ref.'">'.$label."</a>\n";
      }
    }
    if ($wrapper === True) {
      $result .= '</div>';
    } else if ($wrapper!==False) {
      $spacepos = strpos($wrapper, " ");
      if ($spacepos!==False) {
        $result .= '</'.substr($wrapper, 0, $spacepos).'>';
      } else {
        $result .= '</'.$wrapper.'>';
      }
    }
    return $result;
  }


  function darwinDialogEnd() {
    echo '</div></div></div></div>';
  }

if (!function_exists('http_response_code')) {
  function http_response_code($code = NULL) {

    if ($code !== NULL) {

      switch ($code) {
        case 100: $text = 'Continue'; break;
        case 101: $text = 'Switching Protocols'; break;
        case 200: $text = 'OK'; break;
        case 201: $text = 'Created'; break;
        case 202: $text = 'Accepted'; break;
        case 203: $text = 'Non-Authoritative Information'; break;
        case 204: $text = 'No Content'; break;
        case 205: $text = 'Reset Content'; break;
        case 206: $text = 'Partial Content'; break;
        case 300: $text = 'Multiple Choices'; break;
        case 301: $text = 'Moved Permanently'; break;
        case 302: $text = 'Moved Temporarily'; break;
        case 303: $text = 'See Other'; break;
        case 304: $text = 'Not Modified'; break;
        case 305: $text = 'Use Proxy'; break;
        case 400: $text = 'Bad Request'; break;
        case 401: $text = 'Unauthorized'; break;
        case 402: $text = 'Payment Required'; break;
        case 403: $text = 'Forbidden'; break;
        case 404: $text = 'Not Found'; break;
        case 405: $text = 'Method Not Allowed'; break;
        case 406: $text = 'Not Acceptable'; break;
        case 407: $text = 'Proxy Authentication Required'; break;
        case 408: $text = 'Request Time-out'; break;
        case 409: $text = 'Conflict'; break;
        case 410: $text = 'Gone'; break;
        case 411: $text = 'Length Required'; break;
        case 412: $text = 'Precondition Failed'; break;
        case 413: $text = 'Request Entity Too Large'; break;
        case 414: $text = 'Request-URI Too Large'; break;
        case 415: $text = 'Unsupported Media Type'; break;
        case 500: $text = 'Internal Server Error'; break;
        case 501: $text = 'Not Implemented'; break;
        case 502: $text = 'Bad Gateway'; break;
        case 503: $text = 'Service Unavailable'; break;
        case 504: $text = 'Gateway Time-out'; break;
        case 505: $text = 'HTTP Version not supported'; break;
        default:
          exit('Unknown http status code "' . htmlentities($code) . '"');
        break;
      }

      $protocol = (isset($_SERVER['SERVER_PROTOCOL']) ? $_SERVER['SERVER_PROTOCOL'] : 'HTTP/1.0');

      header($protocol . ' ' . $code . ' ' . $text);

      $GLOBALS['http_response_code'] = $code;

    } else {

      $code = (isset($GLOBALS['http_response_code']) ? $GLOBALS['http_response_code'] : 200);

    }

    return $code;

  }
}

/**
 * @param mysqli $db
 * @param mysqli_stmt $stmt
 * @param string $error
 * @param int $code
 * @param string $status
 */
function stmtError($db, $stmt, $error="Database error", $code=500, $status="Server error") {
  $dberror = $stmt->error;
  $stmt->close();
  $db->rollback();
  $db->close();
  handleError($error.': '.$dberror);
}

/**
 * @param mysqli $db
 * @param string $error
 * @param int $code
 * @param string $status
 */
function dbError($db, $error="Database error", $code=500, $status="Server error") {
  $dberror = $db->error;
  $db->rollback();
  $db->close();
  handleError($error.': '.$dberror);
}


/**
 * Prepare a statement, but in a way that checks the result, and errors out when it fails.
 * @param mysqli $db
 * @param string $sql
 * @return mysqli_stmt
 */
function checkPrepare($db, $query) {
  $result = $db->prepare($query);
  if ($result===FALSE) {
    dbError($db);
  }
  return $result;
}

/**
 * Prepare a statement, but in a way that checks the result, and errors out when it fails.
 * @param mysqli $db
 * @param mysqli_stmt $stmt
 * @param string $types
 * @param mixed $vars
 *
 */
function checkBindParam($db, $stmt, $types, &$var1, &$var2=NULL, &$var3=NULL, &$var4=NULL) {
  $num=func_num_args();
  if ($num==4) {
    $result = $stmt->bind_param($types, $var1);
  } else if ($num==5) {
    $result = $stmt->bind_param($types, $var1, $var2);
  } else if ($num==6) {
    $result = $stmt->bind_param($types, $var1, $var2, $var3);
  }
  if ($result===FALSE) {
    stmtError($db,$stmt);
  }
}

/**
 * Prepare a statement, but in a way that checks the result, and errors out when it fails.
 * @param mysqli $db
 * @param mysqli_stmt $stmt
 * @param mixed $var1
 * @param mixed $var2
 * @param mixed $var3
 * @param mixed $var4
 */
function checkBindResult($db, $stmt, &$var1, &$var2=NULL, &$var3=NULL, &$var4=NULL) {
  $num=func_num_args();
  if ($num==3) {
    $result = $stmt->bind_result($var1);
  } else if ($num==4) {
    $result = $stmt->bind_result($var1, $var2);
  } else if ($num==5) {
    $result = $stmt->bind_result($var1, $var2, $var3);
  } else if ($num==6) {
    $result = $stmt->bind_result($var1, $var2, $var3, $var4);
  }
  if ($result===FALSE) {
    stmtError($db,$stmt);
  }
}

/**
 * @param mysqli $db
 * @param mysqli_stmt $stmt
 * @return boolean The result of mysqli_stmt->execute()
 */
function checkExecute($db, $stmt) {
  $result = $stmt->execute();
  if ($result===FALSE) {
    stmtError($db, $stmt);
  }
  return $result;
}

