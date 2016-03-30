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

/**
 * Created by pdvrieze on 28/03/16.
 */

package uk.ac.bournemouth.darwin.html

import kotlinx.html.img
import kotlinx.html.stream.appendHTML
import javax.servlet.Servlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MainServlet: HttpServlet(), Servlet {
  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    when (req.pathInfo) {
      "/" -> resp.darwinResponse(req) {
        darwinDialog(title = "loading", id="banner") {
          img(alt="loading...", src="/assets/progress_large.gif") { width="192"; height="192"}
        }
      }
      "/common/menu" -> {
        resp.writer!!.use {
          it.appendHTML().darwinMenu(req)
        }
      }

      else -> resp.darwinError(req, "The resource ${req.pathInfo} was not found", HttpServletResponse.SC_NOT_FOUND, "NOT FOUND")

    }
  }
}