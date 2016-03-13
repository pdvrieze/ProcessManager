/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.darwin.html

import kotlinx.html.*
import net.sourceforge.migbase64.Base64
import uk.ac.bournemouth.util.kotlin.sql.ConnectionHelper
import uk.ac.bournemouth.util.kotlin.sql.connection
import java.security.MessageDigest
import javax.naming.InitialContext
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource


internal const val FIELD_USERNAME = "username"
internal const val FIELD_PASSWORD = "password"
internal const val FIELD_REDIRECT = "redirect"

/**
 * Created by pdvrieze on 12/03/16.
 */

internal fun sha1(src:ByteArray):ByteArray = MessageDigest.getInstance("SHA1").digest(src)

const val DBRESOURCE = "java:comp/env/jdbc/webauth"

public class AccountDb internal constructor(val connection: ConnectionHelper) {

    private fun getSalt(username:String):String {
        return ""
    }

    private fun createPasswordHash(salt:String, password:String):String {
        return "{SHA}${Base64.encoder().encodeToString(sha1(password.toByteArray()))}";
    }


    public fun verifyCredentials(username:String, password:String): Boolean {

        val salt = getSalt(username)
        val passwordHash = createPasswordHash(salt, password)

        connection.prepareStatement("SELECT `user` FROM users WHERE `user`=? AND `password`=?") {
            setString(1, username)
            setString(2, passwordHash)
            execute() { resultSet ->
                 return resultSet.next() // If we can get a record, the combination exists.
            }
        }
    }

}

public fun <R> accountDb(block:AccountDb.()->R): R {
    val dataSource: DataSource =  InitialContext.doLookup<DataSource>(DBRESOURCE)
    return dataSource.connection { uk.ac.bournemouth.darwin.html.AccountDb(it).block() }
}


class AccountController : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        when(req.pathInfo) {
            "login" -> tryLogin(req, resp)
            else -> super.doGet(req, resp)
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        when(req.pathInfo) {
            "login" -> tryCredentials(req, resp)
            else -> super.doPost(req, resp)
        }

    }

    private fun tryCredentials(req: HttpServletRequest, resp: HttpServletResponse) {
        val username = req.getParameter(FIELD_USERNAME)
        val password = req.getParameter(FIELD_PASSWORD)
        val redirect = req.getParameter(FIELD_REDIRECT)
        if (username==null || password ==null) {
            tryLogin(req, resp)
        } else {
            accountDb {
                if (verifyCredentials(username, password)) {

                    /*
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

                     */
                } else {
                    invalidCredentials(req, resp)
                    /*
                        if ($htmloutput) {
              presentLoginScreen('Username or password not correct');
            } else {
              http_response_code(401);
              echo "invalid:Invalid credentials";
            }

                 */
                }
            }
        }

    }


    private fun tryLogin(req: HttpServletRequest, resp: HttpServletResponse) {
        if (req.userPrincipal!=null) {
            loginSuccess(req, resp)
        } else {


            if (req.htmlAccepted) {
                resp.darwinResponse(req) {
                    loginScreen(req, resp)
                }

            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                resp.writer.use { it.append("error:Login is required\n") }
            }
        }
    }

    private fun loginSuccess(req: HttpServletRequest, resp: HttpServletResponse) {
        val userName = req.userPrincipal?.name
        if (userName==null) { loginScreen(req, resp) }
        if (req.htmlAccepted) {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.

        } else {
            resp.writer.use { it.append("login:").append(userName).append('n') }
        }
    }

    private fun loginScreen(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.darwinResponse(req) {
            this.darwinDialog("login") {
                form(action = "login", method = FormMethod.post, encType = FormEncType.applicationXWwwFormUrlEncoded) {
                    acceptCharset="utf8"
                    val redirect:String? = req.getParameter("redirect")
                    if(redirect!=null) {
                        input(name=FIELD_REDIRECT, type = InputType.hidden) { value = redirect }
                    }
                    val requestedUsername= req.getParameter("username")
                    table {
                        style = "border:none"
                        tr {
                            td {
                                label { for_='#'+FIELD_USERNAME
                                    +"User name:"
                                }
                            }
                            td {
                                input(name=FIELD_USERNAME, type= InputType.text) {
                                    if (requestedUsername!=null) { value=requestedUsername }
                                }
                            }
                        }
                        tr {
                            td {
                                label { for_='#'+FIELD_PASSWORD
                                    +"Password:"
                                }
                            }
                            td {
                                input(name=FIELD_PASSWORD, type= InputType.password)
                            }
                        }
                    } // table
                    span {
                        style="margin-top: 1em; float: right;"
                        input(type= InputType.submit) {
                            value="Log in"
                        }
                    }
                    div { id="forgotpasswd"
                        a(href=req.contextPath+"/resetpasswd")
                    }
                }
            }
        }
    }
}