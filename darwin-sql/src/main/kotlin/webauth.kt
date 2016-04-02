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

object WebAuthDB: Database(1) {

  object users: MutableTable(EXTRACONF) {
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

  object roles: MutableTable(EXTRACONF) {
    val role by VARCHAR("role", 30) { NOT_NULL }
    val description by VARCHAR("description", 120) { NOT_NULL }

    override fun init() {
      PRIMARY_KEY(role)
    }
  }

  object user_roles: MutableTable(EXTRACONF) {
    val user by VARCHAR("user", 30) { NOT_NULL }
    val role by VARCHAR("role", 30) { NOT_NULL }

    override fun init() {
      PRIMARY_KEY(user, role)
      FOREIGN_KEY(user).REFERENCES(users.user)
      FOREIGN_KEY(role).REFERENCES(roles.role)
    }
  }

  object tokens: MutableTable(EXTRACONF) {
    val tokenid by INT("tokenid") { NOT_NULL; AUTO_INCREMENT }
    val user by reference(users.user) { NOT_NULL }

    override fun init() {
      PRIMARY_KEY(tokenid)
      FOREIGN_KEY(user).REFERENCES(users.user)
    }
  }

        /*  `tokenid` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(30) NOT NULL,
  `ip` varchar(24) NOT NULL,
  `keyid` int(11),
  `token` varchar(45) NOT NULL,
  `epoch` bigint NOT NULL,
  PRIMARY KEY (`tokenid`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`),
  FOREIGN KEY (`keyid`) REFERENCES `pubkeys` (`keyid`)
*/

}
