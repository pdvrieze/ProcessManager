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

import org.apache.catalina.Authenticator
import org.apache.catalina.Container
import org.apache.catalina.Context
import org.apache.catalina.Lifecycle
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.catalina.deploy.LoginConfig
import org.apache.catalina.valves.ValveBase
import org.apache.naming.ContextBindings
import uk.ac.bournemouth.darwin.accounts.DARWINCOOKIENAME
import uk.ac.bournemouth.darwin.accounts.MAXTOKENLIFETIME
import uk.ac.bournemouth.darwin.accounts.accountDb
import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl
import java.io.IOException
import java.io.PrintStream
import java.net.URI
import java.net.URLEncoder
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import javax.naming.NamingException
import javax.servlet.ServletException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource
import javax.naming.Context as NamingContext

//@WebFilter(description = "Authentication filter for the darwin system", asyncSupported = true)
class DarwinAuthenticator : ValveBase(), Lifecycle, Authenticator {

    private enum class AuthResult {
        AUTHENTICATED,
        ERROR,
        EXPIRED,
        LOGIN_NEEDED
    }

    init {
      asyncSupported = true
    }

    var resourceName:String = DEFAULTDBRESOURCE

    var loginPage: String? = "/accountmgr/login"

    private var container_:Container? = null

    override fun getContainer() = container_
    override fun setContainer(container:Container?) {
        container_ = container
    }

    override fun initInternal() {
        super.initInternal()
        log.info("Initialising DarwinAuthenticatorValve")
    }

    fun NamingContext.print(out:PrintStream, name:String, indent:Int = 0) {
        for( binding in listBindings(name)) {
            out.print(" ".repeat(indent))
            out.println(binding.name)
            val value = binding.`object`
            if (value is NamingContext) {
                value.print(out, binding.name, indent+4)
            }
        }
    }

    val dataSource by lazy {

        val loader = ContextBindings.getClassLoader()
        try {
            val context = loader.lookup(if (container_ is Context) "comp/env/" else "") as NamingContext

            context.lookup(resourceName) as DataSource
        } catch (e:NamingException) {
            System.err.println("Failure to look up name $resourceName in context ${loader}")
            System.err.println("  the context is:")
            loader.print(System.err, "comp", 6)
            throw NamingException("Failure to look up $resourceName").initCause(e)
        }
    }



    private fun invokeNext(request: Request, response: Response) {
        next.invoke(request, response)
        val cookieNote = request.getNote(DARWINCOOKIENAME)
        if (cookieNote != null) { log.finer("Found a cookie note on the request") }
        (cookieNote as? String)?.let { token -> response.addCookie(createAuthCookie(token, request.isSecure))}
    }

    @Throws(IOException::class, ServletException::class)
    override fun invoke(request: Request, response: Response) {
        log.info("Invoking DarwinAuthenticator for ${request.method} request ${request.requestURI}")
        val container = container_!!

        // First do any handling of already present authentication information
        val authresult = lazy { authenticateHelper(dataSource, request, response) }
        request.setNote("response", response)
        run {
            if ((container is Context && container.preemptiveAuthentication) || (request.cookies?.any { c -> c.name==DARWINCOOKIENAME } ?: false)) { authresult.value }
            // If the context wants us to do preemptive authentication, make sure to get the authentication result.
            // That has side-effects that will register the principal.
        }

        loginPage?.let { // If the target is the login page, now just go there.
            val loginPageUrl = URI.create(it)
            if (request.requestURI==loginPageUrl.path) {
                invokeNext(request, response)
                return
            }
        }

        val realm = container.realm
        if (realm != null) {
            log.info("DarwinAuthenticator: this context has an authentication realm, enforce the constraints")
            val constraints = (container as? Context)?.let { realm.findSecurityConstraints(request, it) }
            if (constraints == null) {
                log.fine ("Realm has no constraints, calling next in chain")
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
                    AuthResult.AUTHENTICATED -> if (container is Context && realm.hasResourcePermission(request, response, constraints, container)) {
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
            log.info("No realm set for the request")
            // No realm, no authentication required.
            invokeNext(request, response)
        }
    }

    private fun requestLogin(request: Request, response: HttpServletResponse) {
        val loginPage = this.loginPage
        val decodedRequestURI: String? = request.decodedRequestURI
        if (loginPage != null && decodedRequestURI!=null) {
            response.sendRedirect("${loginPage}?redirect=${URLEncoder.encode(decodedRequestURI, "utf-8")}")
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You need to log in for this page, but no login page is configured")
        }
    }

    val authMethod: String get() = AUTHTYPE

    /**
     * Overridden version of login that does not use register. We don't need tomcat session cookies, and we certainly
     * don't carry passwords around.
     */
    override fun login(userName: String, password: String, request: Request) {
        log.info("Login requested for ${request.requestURI}. Passing to realm")
        val principal = container_!!.realm.authenticate(userName, password) ?: throw ServletException("Invalid credentials")

        accountDb(dataSource) {
            val authtoken = this.createAuthtoken(userName, request.remoteAddr)

            val response = request.getNote("response") as? Response
            if (response!=null) {
                log.log(Level.FINE, "Setting a cookie on the response")
                response.addCookie(createAuthCookie(authtoken, request.isSecure))
            } else {
                log.info("Setting a note for the cookie as there is no recorded response")
                request.setNote(DARWINCOOKIENAME, authtoken)
            }
        }

        if (log.isLoggable(Level.FINER)) {
            val name = if (principal == null) "none" else principal.name
            log.finer("Authenticated '$name' with type '${AUTHTYPE}'")
        }

        // Set the user into the request
        request.userPrincipal = principal
    }

    @Throws(ServletException::class)
    override fun logout(request: Request) {
        log.fine("Logging out")
        (request.getNote("response") as? HttpServletResponse) ?.let { response ->
            clearCookies(request, response)
        }

        request.userPrincipal = null
    }

    override fun authenticate(request: Request, response: HttpServletResponse): Boolean {
        val authResult = authenticateHelper(dataSource, request, response)
        when (authResult) {
            AuthResult.AUTHENTICATED -> return true
            else -> {
                requestLogin(request, response)
                return false
            } // We have sent the correct message
        }
    }

    @Throws(IOException::class)
    override fun authenticate(request: Request, response: HttpServletResponse, config: LoginConfig): Boolean {
        return authenticate(request, response)
    }

    companion object {

        private val AUTHTYPE = "DARWIN"

        val DEFAULTDBRESOURCE = "webauthadm"

        private val log = Logger.getLogger(DarwinAuthenticator::class.java.name)

        fun Principal.asDarwinPrincipal(dataSource: DataSource): DarwinPrincipal {
            try {
                return toDarwinPrincipal(dataSource, this)
            } catch (e: NamingException) {
                log.log(Level.WARNING, "Failure to connect to database", e)
                throw e
            }

        }

        private fun toDarwinPrincipal(dataSource: DataSource, principal: Principal): DarwinUserPrincipal {
            if (principal is DarwinUserPrincipal) {
                return principal
            }
            return DarwinUserPrincipalImpl(dataSource, principal.name)
        }

        private fun createAuthCookie(authtoken: String, secureCookie:Boolean) = Cookie(DARWINCOOKIENAME, authtoken).apply {
            maxAge = MAXTOKENLIFETIME;
            path="/"
            secure = secureCookie
            version=1
        }

        private fun clearCookies(request: Request, response:HttpServletResponse) {
            request.removeNote(DARWINCOOKIENAME)
            request.cookies?.asSequence()?.filter ({ it.name == DARWINCOOKIENAME })?.forEach { staleCookie ->
                staleCookie.maxAge = 0
                staleCookie.value = ""
                staleCookie.path="/"
                staleCookie.secure = request.isSecure
                response.addCookie(staleCookie)
            }

        }

        private fun authenticateHelper(dataSource: DataSource, request: Request, response: HttpServletResponse): AuthResult {
            val origPrincipal: Principal? = request.userPrincipal

            val authTokens: List<String> =
                  ((request.getHeader(DARWINCOOKIENAME)?.let{ listOf(it) }?: emptyList()).asSequence() +
                  (request.cookies?.asSequence()?.filter ({ it.name == DARWINCOOKIENAME })?.map { it.value }?: emptySequence())).toList()

            if (authTokens.isEmpty())
                    return AuthResult.LOGIN_NEEDED.apply { log.info("authenticateHelper: No user found") }

            if (origPrincipal != null) {
                if (origPrincipal !is DarwinUserPrincipal) {
                    log.info("Found preexisting principal, converted to darwinprincipal: ${origPrincipal.name}")
                    request.authType = AUTHTYPE
                    request.userPrincipal = origPrincipal.asDarwinPrincipal(dataSource)
                }

                return AuthResult.AUTHENTICATED.apply { log.info("authenticateHelper: previously authenticated as ${request.userPrincipal.name}") }
            }

            try {

                accountDb(dataSource) {
                    cleanAuthTokens() // First get rid of no longer needed tokens

                    // Try all cookies, not the first
                    val userInfo = authTokens.asSequence().map { authToken ->
                        userFromToken(authToken, request.remoteAddr)?.let { user -> authToken to user }
                    }.filterNotNull().firstOrNull()

                    if (userInfo != null) {
                        val user = userInfo.second
                        val authToken = userInfo.first
                        request.userPrincipal = DarwinUserPrincipalImpl(dataSource, user, getUserRoles(user))
                        // Set the cookie as a note so it can be removed for example in logout
                        request.setNote(DARWINCOOKIENAME, authToken)

                        return AuthResult.AUTHENTICATED.apply { log.info("authenticateHelper: authenticated as ${request.userPrincipal.name}") }
                    }
                }
                // invalidate all old cookies
                clearCookies(request, response)

                return AuthResult.LOGIN_NEEDED.apply { log.info("authenticateHelper: cookie no longer valid (${authTokens.first()})") }

            } catch (e: Exception) {
                log.log(Level.WARNING, "Failure in verifying user", e)
                return AuthResult.ERROR
            }

        }

    }

}
