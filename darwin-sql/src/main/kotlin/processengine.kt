
import uk.ac.bournemouth.kotlinsql.ColumnType
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table

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
 * Created by pdvrieze on 31/03/16.
 */

object processModelTable: Table("processmodels", "ENGINE=InnoDB CHARSET=utf8", {
  val handle = BIGINT("pmhandle") { NOT_NULL; AUTO_INCREMENT }
  val owner = VARCHAR("owner",30) { NOT_NULL }
  MEDIUMTEXT(name="model")
  INDEX (owner)
  PRIMARY_KEY(handle)
}) {
  val pmhandle by type(ColumnType.BIGINT_T)
  val owner by type(ColumnType.VARCHAR_T)
}

object pmUsersTable: Table("pmusers", "ENGINE=InnoDB CHARSET=utf8", {
  val handle = BIGINT("pmhandle") { NOT_NULL }
  val user =VARCHAR("user", 30)
  PRIMARY_KEY( handle, user )
  FOREIGN_KEY ( handle ).REFERENCES(processModelTable.pmhandle)
}) {
  val pmhandle by type(ColumnType.BIGINT_T)
  val user by type(ColumnType.VARCHAR_T)
}

object pmrolesTable: Table("pmroles", "ENGINE=InnoDB CHARSET=utf8",{
  val handle = BIGINT("pmhandle") { NOT_NULL }
  val role =VARCHAR("role", 30)
  PRIMARY_KEY( handle, role )
  FOREIGN_KEY ( handle ).REFERENCES(processModelTable.pmhandle)

}) {
  val pmhandle by type(ColumnType.BIGINT_T)
  val role by type(ColumnType.VARCHAR_T)
}

object ProcessEngineDB: Database(2, {
  table(processModelTable)
  table(pmUsersTable)
}) {
  val processmodels by ref(processModelTable)
  val pmusers by ref(pmUsersTable)
  val pmroles by ref(pmrolesTable)
}
