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

package uk.ac.bournemouth.ac.db.darwin.webauth


import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.MutableTable

/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF="ENGINE=InnoDB CHARSET=utf8"

object UserTaskDB: Database(1) {

  object users: MutableTable("users", EXTRACONF) {
    val user by VARCHAR("user", 30) { NOT_NULL }
    val fullname by VARCHAR("fullname", 80)
    val alias by VARCHAR("alias", 80)
    val password by VARCHAR("password", 40)
    val resettoken by VARCHAR("resettoken", 20)
    val resettime by DATETIME("resettime")

    override fun init() {
      PRIMARY_KEY(user)
    }
  }


}
