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

package uk.ac.bournemouth.darwin.catalina.authenticator

import org.apache.catalina.Lifecycle
import org.apache.catalina.authenticator.AuthenticatorBase
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.catalina.deploy.LoginConfig
import org.apache.juli.logging.LogFactory
import org.apache.naming.ContextBindings
import uk.ac.bournemouth.darwin.accounts.DARWINCOOKIENAME
import uk.ac.bournemouth.darwin.accounts.MAXTOKENLIFETIME
import uk.ac.bournemouth.darwin.accounts.accountDb
import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl
import java.io.IOException
import java.net.URLEncoder
import java.security.Principal
import javax.naming.Context
import javax.naming.NamingException
import javax.servlet.ServletException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource


class DarwinAuthenticator : AuthenticatorBase(), Lifecycle {

    private enum class AuthResult {
        AUTHENTICATED,
        ERROR,
        EXPIRED,
        LOGIN_NEEDED
    }


    var resourceName:String = DEFAULTDBRESOURCE

    val dataSource by lazy {
        val context: Context = /*if (globalResource) container. else */ContextBindings.getClassLoader().lookup("comp/env/") as Context

        context.lookup(resourceName) as DataSource
    }


    var loginPage: String? = "/accounts/login"


    private inline fun invokeNext(request: Request, response: Response) {
        next.invoke(request, response)
        (request.getNote(DARWINCOOKIENAME) as? String)?.let { token -> response.addCookie(createAuthCookie(token))}
    }

    @Throws(IOException::class, ServletException::class)
    override fun invoke(request: Request, response: Response) {

        val authresult = lazy { authenticateHelper(dataSource, request, response) }
        request.setNote("response", response)
        if (context.preemptiveAuthentication) { authresult.value }

        val realm = context.realm
        if (realm != null) {
            log.trace("This context has an authentication realm, enforce the constraints")
            val constraints = realm.findSecurityConstraints(request, context)
            if (constraints == null) {
                log.trace("Realm has no constraints, calling next in chain")
                // Unconstrained
                invokeNext(request, response)
                return
            }
            // Need security, set cache control
            response.setHeader("Cache-Control", "private")
            if (!realm.hasUserDataPermission(request, response, constraints)) {
                when (authresult.value) {
                    AuthResult.AUTHENTICATED -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                    AuthResult.ERROR -> response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error")
                    else -> requestLogin(request, response)
                }
                return
            }

            var authRequired: Boolean
            if (constraints == null) {
                authRequired = false
            } else {
                authRequired = true
                for(constraint in constraints) {
                    if (!constraint.authConstraint) {
                        authRequired = false
                        break;
                    } else if (!constraint.allRoles) {
                        val roles = constraint.findAuthRoles()
                        if (roles == null || roles.size == 0) {
                            authRequired = false
                            break;
                        }
                    }
                }
            }

            if (authRequired) {
                when (authresult.value) {
                    AuthResult.AUTHENTICATED -> if (realm.hasResourcePermission(request, response, constraints, context)) {
                            invokeNext(request, response)
                        } else {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User " + request.userPrincipal + " does not have permission for " + realm.info + " class:" + realm.javaClass.name)
                        }
                    AuthResult.LOGIN_NEEDED, AuthResult.EXPIRED -> requestLogin(request, response)
                    else -> {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication required, but encountered an error")
                    }
                }
            } else {
                invokeNext(request, response)
            }

        } else {
            // No realm, no authentication required.
            invokeNext(request, response)
        }
    }

    private fun requestLogin(request: Request, response: Response) {
        val loginPage = context.loginConfig.loginPage ?: this.loginPage
        val decodedRequestURI: String? = request.decodedRequestURI
        if (loginPage != null && decodedRequestURI!=null) {
            response.sendRedirect("${loginPage}?redirect=${URLEncoder.encode(decodedRequestURI, "utf-8")}")
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You need to log in for this page, but no login page is configured")
        }
    }

    override fun getAuthMethod() = AUTHTYPE

    /**
     * Overridden version of login that does not use register. We don't need tomcat session cookies, and we certainly
     * don't carry passwords around.
     */
    override fun login(userName: String, password: String, request: Request) {
        val principal = doLogin(request, userName, password)
        //        val principal = realm.authenticate(userName, password) ?: throw ServletException("Invalid credentials")

        accountDb(dataSource) {
            val authtoken = this.createAuthtoken(userName, request.remoteAddr)

            val response = request.getNote("response") as? Response
            if (response!=null) {
                response.addCookie(createAuthCookie(authtoken))
            } else {
                request.setNote(DARWINCOOKIENAME, authtoken)
            }
        }

        if (log.isDebugEnabled) {
            val name = if (principal == null) "none" else principal.name
            log.debug("Authenticated '$name' with type '$authMethod'")
        }

        // Set the user into the request
        request.userPrincipal = principal
    }

    @Throws(ServletException::class)
    override fun logout(request: Request) {
        request.cookies.find({ it.name == DARWINCOOKIENAME })?.let {
            accountDb(dataSource) {
                logout(it.value!!)
            }
            it.value = ""
            it.maxAge = 0
            val response = request.getNote("response") as? Response
            if (response!=null) {
                response.addCookie(Cookie(DARWINCOOKIENAME,"").apply { maxAge=0 })
            }
        }
        request.userPrincipal = null
    }

    @Throws(IOException::class)
    override fun authenticate(request: Request, response: HttpServletResponse, config: LoginConfig): Boolean {
        val authResult = authenticateHelper(dataSource, request, response)
        when (authResult) {
            AuthResult.AUTHENTICATED -> return true
            else -> return false
        }
    }

    companion object {

        private val AUTHTYPE = "DARWIN"

        val DEFAULTDBRESOURCE = "webauthAdm"

        private val log = LogFactory.getLog(DarwinAuthenticator::class.java)

        fun Principal.asDarwinPrincipal(dataSource: DataSource): DarwinPrincipal {
            try {
                return toDarwinPrincipal(dataSource, this)
            } catch (e: NamingException) {
                log.warn("Failure to connect to database", e)
                throw e
            }

        }

        private fun toDarwinPrincipal(dataSource: DataSource, principal: Principal): DarwinUserPrincipal {
            if (principal is DarwinUserPrincipal) {
                return principal
            }
            return DarwinUserPrincipalImpl(dataSource, principal.name)
        }

        private fun createAuthCookie(authtoken: String) = Cookie(DARWINCOOKIENAME, authtoken).apply { maxAge = MAXTOKENLIFETIME; path="/" }

        private fun authenticateHelper(dataSource: DataSource, request: Request, response: HttpServletResponse): AuthResult {
            val origPrincipal: Principal? = request.userPrincipal

            val authToken: String = request.getHeader(DARWINCOOKIENAME) ?:
                    request.cookies?.find ({ it.name == DARWINCOOKIENAME })?.let { it.value } ?:
                    return AuthResult.LOGIN_NEEDED

            if (origPrincipal != null) {
                if (origPrincipal !is DarwinUserPrincipal) {
                    log.trace("Found preexisting principal, converted to darwinprincipal: ${origPrincipal.name}")
                    request.authType = AUTHTYPE
                    request.userPrincipal = origPrincipal.asDarwinPrincipal(dataSource)
                }
                if (authToken!= null) response.addCookie(createAuthCookie(authToken))

                return AuthResult.AUTHENTICATED
            }

            try {
                accountDb(dataSource) {
                    cleanAuthTokens() // First get rid of no longer needed tokens
                    val user = userFromToken(authToken, request.remoteAddr)
                    if (user != null) {
                        request.userPrincipal = DarwinUserPrincipalImpl(dataSource, user, getUserRoles(user))
                        response.addCookie(createAuthCookie(authToken))

                        return AuthResult.AUTHENTICATED
                    }
                }
                return AuthResult.LOGIN_NEEDED

            } catch (e: Exception) {
                log.warn("Failure in verifying user", e)
                return AuthResult.ERROR
            }

        }

    }

}
