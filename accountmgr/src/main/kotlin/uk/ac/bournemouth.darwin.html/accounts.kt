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
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Created by pdvrieze on 12/03/16.
 */

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
                        input(name="redirect", type = InputType.hidden) { value = redirect }
                    }
                    val requestedUsername= req.getParameter("username")
                    table {
                        style = "border:none"
                        tr {
                            td {
                                label { for_="#username"
                                    +"User name:"
                                }
                            }
                            td {
                                input(name="username", type= InputType.text) {
                                    if (requestedUsername!=null) { value=requestedUsername }
                                }
                            }
                        }
                        tr {
                            td {
                                label { for_="#password"
                                    +"Password:"
                                }
                            }
                            td {
                                input(name="password", type= InputType.password)
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