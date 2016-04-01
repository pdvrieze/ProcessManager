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

package uk.ac.bournemouth.ac.db.darwin.usertasks


import uk.ac.bournemouth.kotlinsql.ColumnType.BIGINT_T
import uk.ac.bournemouth.kotlinsql.ColumnType.VARCHAR_T
import uk.ac.bournemouth.kotlinsql.Database

/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF="ENGINE=InnoDB CHARSET=utf8"

object userTaskTable: uk.ac.bournemouth.kotlinsql.Table("usertasks", EXTRACONF, {
  val handle = BIGINT("taskhandle") { NOT_NULL; AUTO_INCREMENT }
               BIGINT("remotehandle") { NOT_NULL }
  PRIMARY_KEY(handle)
}) {
  val taskhandle by type(BIGINT_T)
  val remotehandle by type(BIGINT_T)
}

object nodedataTable: uk.ac.bournemouth.kotlinsql.Table("nodedata", EXTRACONF, {
  val name = VARCHAR("name", 30) { NOT_NULL }
  val handle = BIGINT("taskhandle") { NOT_NULL }
  TEXT("data")
  PRIMARY_KEY( name, handle )
  FOREIGN_KEY ( handle ).REFERENCES(userTaskTable.taskhandle)
}) {
  val taskhandle by type(BIGINT_T)
  val name by type(VARCHAR_T)
}

object UserTaskDB: Database(1, {
  table(userTaskTable)
  table(nodedataTable)
}) {
  val usertasks by ref(userTaskTable)
  val nodedata by ref(nodedataTable)
}
