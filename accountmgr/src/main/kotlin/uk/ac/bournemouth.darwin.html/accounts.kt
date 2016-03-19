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
import uk.ac.bournemouth.darwin.accounts.AccountDb
import uk.ac.bournemouth.darwin.accounts.accountDb
import java.security.MessageDigest
import java.security.Principal
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


internal const val FIELD_USERNAME = "username"
internal const val FIELD_PASSWORD = "password"
internal const val FIELD_PUBKEY = "pubkey"
internal const val FIELD_REDIRECT = "redirect"
internal const val FIELD_KEYID = "keyid"
internal const val FIELD_RESPONSE = "response"
internal const val FIELD_RESETTOKEN = "resettoken"
internal const val FIELD_NEWPASSWORD1 = "newpassword1"
internal const val FIELD_NEWPASSWORD2 = "newpassword2"

internal const val DARWINCOOKIENAME = "DWNID"
internal const val MAXTOKENLIFETIME = 864000 /* Ten days */
internal const val MAXCHALLENGELIFETIME = 60 /* 60 seconds */
internal const val MAX_RESET_VALIDITY = 1800 /* 12 hours */

/*
 * This file contains the functionality needed for managing accounts on darwin. This includes the underlying database
 * interaction through [AccountDb] as well as web interaction through [AccountController].
 */

/** Create a SHA1 digest of the source */
internal fun sha1(src:ByteArray):ByteArray = MessageDigest.getInstance("SHA1").digest(src)

const val DBRESOURCE = "java:comp/env/jdbc/webauth"
const val AUTHDBADMINUSERNAME = "java:comp/env/webauthAdm/userName"
const val AUTHDBADMINPASSWORD = "java:comp/env/webauthAdm/password"

private inline fun <R> accountDb(block:AccountDb.()->R): R = accountDb(DBRESOURCE, block)

class AccountController : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        when(req.pathInfo) {
            "/login" -> tryLogin(req, resp)
            "/challenge" -> challenge(req, resp)
            "/chpasswd" -> chpasswd(req, resp)
            "/regkey" -> resp.darwinError(req, "HTTP method GET is not supported by this URL", HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Get not supported")
            else -> resp.darwinError(req, "The resource ${req.contextPath}${req.pathInfo} was not found", HttpServletResponse.SC_NOT_FOUND, "Not Found")
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        when(req.pathInfo) {
            "/login" -> tryCredentials(req, resp)
            "/challenge" -> challenge(req, resp)
            "/chpasswd" -> chpasswd(req, resp)
            "/regkey" -> registerkey(req, resp)
            else -> resp.darwinError(req, "The resource ${req.contextPath}${req.pathInfo} was not found", HttpServletResponse.SC_NOT_FOUND, "Not Found")
        }

    }

    private fun registerkey(req: HttpServletRequest, resp: HttpServletResponse) {
        val username = req.getParameter(FIELD_USERNAME)
        val password = req.getParameter(FIELD_PASSWORD)
        val keyid = req.getParameter(FIELD_KEYID)
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            resp.darwinError(req, "Missing credentials", HttpServletResponse.SC_FORBIDDEN, "Missing credentials")
        } else {

            val pubkey = req.getParameter(FIELD_PUBKEY)
            accountDb(DBRESOURCE) {
                if (verifyCredentials(username, password)) {
                    if (pubkey.isNullOrBlank()) {
                        resp.contentType("text/plain")
                        resp.writer.use { it.append("authenticated ").appendln(username) }
                    } else {
                        registerkey(username, pubkey, keyid?.toLong())
                    }

                } else {
                    resp.darwinError(req, "Invalid credentials", HttpServletResponse.SC_FORBIDDEN, "Invalid credentials")
                }
            }
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
                    val authtoken = createAuthtoken(username, req.remoteAddr)
                    resp.addCookie(createAuthCookie(authtoken))

                    if (redirect!=null) {
                        resp.sendRedirect(resp.encodeRedirectURL(redirect))
                    } else {
                        if (req.htmlAccepted) {
                            loginSuccess(req, resp, this)
                        } else {
                            resp.writer.use { it.append("login:").appendln(username) }
                        }
                    }
                } else {
                    invalidCredentials(req, resp)
                }
            }
        }

    }

    private fun createAuthCookie(authtoken: String) = Cookie(DARWINCOOKIENAME, authtoken).let { it.maxAge = MAXTOKENLIFETIME; it }


    private fun tryLogin(req: HttpServletRequest, resp: HttpServletResponse) {
        if (req.userPrincipal!=null) {
            accountDb {
                loginSuccess(req, resp, this)
            }
        } else {
            if (req.htmlAccepted) {
                loginScreen(req, resp)
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                resp.writer.use { it.appendln("error:Login is required\n") }
            }
        }
    }

    private fun challenge(req: HttpServletRequest, resp: HttpServletResponse) {
        val keyId = req.getParameter(FIELD_KEYID).toLong()
        if (keyId==null) resp.darwinError(req, "Insufficient credentials", 403, "Forbidden")
        val responseParam = req.getParameter(FIELD_RESPONSE)
        if (responseParam==null) {
            issueChallenge(req, resp, keyId)
        } else {
            val response = Base64.decoder().decode(responseParam)
            //handleResponse(req, resp, keyId, response)
        }
    }

    private fun issueChallenge(req:HttpServletRequest, resp: HttpServletResponse, keyid: Long) {
        val requestIp = req.remoteAddr
        accountDb {
            cleanChallenges()
            val challenge = newChallenge(keyid, req.remoteAddr) // This should fail on an incorrect keyid due to integrity constraints
            resp.contentType("text/plain")
            resp.writer.use { it.append(challenge) }
        }
    }

    private class wrapUser(val req: HttpServletRequest, val username: String) : HttpServletRequest by req {
        override fun getUserPrincipal(): Principal {
            return Principal { username }
        }

        override fun isUserInRole(role: String): Boolean {
            return accountDb { isUserInRole(username, role) }
        }

        override fun getAuthType()= HttpServletRequest.FORM_AUTH
    }

    private fun showChangeScreen(req: HttpServletRequest, resp: HttpServletResponse, message: String? = null, resetToken: String? = null, changedUser: String? = null) {
        resp.darwinResponse(request = req, title="Change password", checkuser = false) {
            darwinDialog("Change password") {
                if (message!=null) div("warning") { +message }
                form(method = FormMethod.post) {
                    acceptCharset="UTF-8"
                    if (resetToken!=null) input(type = InputType.hidden) { value=resetToken }
                    table { style = "border:none"
                        if (resetToken != null || req.isUserInRole("admin")) {
                            tr { td { label { for_="#$FIELD_USERNAME"; +"Username:"}};
                                td {
                                    input(name = FIELD_USERNAME, type = InputType.text) {
                                        if (resetToken != null) { disabled = true }
                                        if (changedUser !=null) { value = changedUser }
                                    }
                                }
                            }
                        } else {
                            tr { td { label { for_="#$FIELD_PASSWORD"; +"Current password:"}};
                                td {
                                    input(name = FIELD_PASSWORD, type = InputType.password)
                                }
                            }
                        }
                        tr { td { label { for_="#$FIELD_NEWPASSWORD1"; + "New password:" } }
                             td { input(name=FIELD_NEWPASSWORD1, type= InputType.password) }}
                        tr { td { label { for_="#$FIELD_NEWPASSWORD2"; + "Repeat new password:" } }
                            td { input(name=FIELD_NEWPASSWORD2, type= InputType.password) }}
                    }
                    span { style="margin-top: 1em; float:right"
                        input(type= InputType.submit) { value = "Change" }
                    }
                }
            }
        }

    }

    private fun chpasswd(req: HttpServletRequest, resp: HttpServletResponse) {
        val resetToken = req.getParameter(FIELD_RESETTOKEN).let { if (it.isNullOrBlank()) null else it }
        if (req.userPrincipal==null && resetToken == null) {
            // No user identification, require login first
            resp.sendRedirect(resp.encodeRedirectURL("${req.servletPath}/login?redirect=${req.servletPath}/chpasswd"))
            return
        }
        val changedUser: String? = req.getParameter(FIELD_USERNAME).let { if (it.isNullOrBlank()) null else it }
        val origPasswd: String? = req.getParameter(FIELD_PASSWORD).let { if (it.isNullOrBlank()) null else it }
        val newPasswd1: String? = req.getParameter(FIELD_NEWPASSWORD1).let { if (it.isNullOrBlank()) null else it }
        val newPasswd2: String? = req.getParameter(FIELD_NEWPASSWORD2).let { if (it.isNullOrBlank()) null else it }

        if (changedUser==null || newPasswd1==null || newPasswd2 == null) {
            showChangeScreen(req=req, resp=resp, resetToken = resetToken, changedUser = changedUser)
            return
        }

        if (newPasswd1!=newPasswd2) {
            val message = if (changedUser!=null) {
                "Please provide two identical copies of the new password"
            } else {
                "Please provide all of the current password and two copies of the new one"
            }
            showChangeScreen(req, resp, message = message, resetToken = resetToken)
            return
        }

        if (newPasswd1.length<6) {
            showChangeScreen(req, resp, message="The new passwords must be at lest 6 characters long", resetToken = resetToken, changedUser = changedUser)
            return
        }

        accountDb {
            val requestingUser = req.userPrincipal?.name ?: changedUser
            if (req.isUserInRole("admin")) {
                if (updateCredentials(changedUser, newPasswd1)) {
                    resp.darwinResponse(req, "Password Changed") { darwinDialog("Successs") { div { +"The password has been changed successfully" } } }
                } else { resp.darwinError(req, "Failure updating password") }
            } else {
                if (requestingUser==changedUser && resetToken!=null && verifyResetToken(changedUser, resetToken)) {
                    if (updateCredentials(changedUser, newPasswd1)) {
                        resp.darwinResponse(req, "Password Changed") { darwinDialog("Successs") { div { +"The password has been changed successfully" } } }
                    } else {
                        resp.darwinError(req, "Failure updating password")
                    }
                } else {
                    resp.darwinError(req, "The given reset token is invalid or expired")
                }
            }


        }
    }

    private fun loginSuccess(req: HttpServletRequest, resp: HttpServletResponse, db: AccountDb) {
        val userName = req.userPrincipal?.name
        if (userName==null) {
            loginScreen(req, resp)
        } else {
            val token = req.cookies.find({ it.name == DARWINCOOKIENAME })?.value

            if (token !=null) resp.addCookie(createAuthCookie(db.updateAuthToken(userName, token)))

            if (req.htmlAccepted) {
                resp.darwinResponse(req) {
                    resp.darwinResponse(request = wrapUser(req, userName), title = "Welcome", pageTitle = "Welcome - Login scucessful") {
                        p { +"Congratulations with successfully authenticating on darwin." }
                    }
                }
            } else {
                resp.writer.use { it.append("login:").appendln(userName) }
            }
        }
    }

    private fun invalidCredentials(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        if (req.htmlAccepted) {
            loginScreen(req, resp, "Username or password not correct")
        } else {
            resp.writer.use { it.appendln("invalid:Invalid credentials") }
        }
    }

    private fun loginScreen(req: HttpServletRequest, resp: HttpServletResponse, errorMsg:String? = null) {
        resp.darwinResponse(req) {
            this.darwinDialog("login") {
                if (errorMsg!=null) {
                    div("errorMsg") { +errorMsg }
                }
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