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

package uk.ac.bournemouth.ac.db.darwin.processengine

import uk.ac.bournemouth.kotlinsql.ColumnType.*
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table


/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF="ENGINE=InnoDB CHARSET=utf8"

object processModelTable: Table("processmodels", EXTRACONF, {
  val handle = BIGINT("pmhandle") { NOT_NULL; AUTO_INCREMENT }
  val owner =  VARCHAR("owner",30) { NOT_NULL }
  MEDIUMTEXT(name="model")
  INDEX (owner)
  PRIMARY_KEY(handle)
}) {
  val pmhandle by type(BIGINT_T)
  val owner by type(VARCHAR_T)
}

object pmUsersTable: Table("pmusers", EXTRACONF, {
  val handle = BIGINT("pmhandle") { NOT_NULL }
  val user =   VARCHAR("user", 30) { BINARY }
  PRIMARY_KEY( handle, user )
  FOREIGN_KEY ( handle ).REFERENCES(processModelTable.pmhandle)
}) {
  val pmhandle by type(BIGINT_T)
  val user by type(VARCHAR_T)
}

object pmrolesTable: Table("pmroles", EXTRACONF, {
  val handle = BIGINT("pmhandle") { NOT_NULL }
  val role =   VARCHAR("role", 30)
  PRIMARY_KEY( handle, role )
  FOREIGN_KEY ( handle ).REFERENCES(processModelTable.pmhandle)

}) {
  val pmhandle by type(BIGINT_T)
  val role by type(VARCHAR_T)
}

object processInstancesTable: Table("processinstances", EXTRACONF, {
  val pihandle = BIGINT("pihandle") { NOT_NULL; AUTO_INCREMENT }
  val owner =    VARCHAR("owner", 30) { NOT_NULL }
                 VARCHAR("name", 50)
  val pmhandle = BIGINT("pmhandle") { NOT_NULL }
                 VARCHAR("state", 15)
                 VARCHAR("uuid", 36) { UNIQUE }
  INDEX(owner)
  PRIMARY_KEY(pihandle)
  FOREIGN_KEY(pmhandle).REFERENCES(processModelTable.pmhandle)
}) {
  val pihandle by type(BIGINT_T)
  val owner by type(VARCHAR_T)
  val name by type(VARCHAR_T)
  val pmhandle by type(BIGINT_T)
  val state by type(VARCHAR_T)
  val uuid by type(VARCHAR_T)
}


object processNodeInstancesTable: Table("processinstances", EXTRACONF, {
  val pnihandle = BIGINT("pnihandle") { NOT_NULL; AUTO_INCREMENT }
  val pihandle =  BIGINT("pihandle") { NOT_NULL }
  val nodeid =    VARCHAR("nodeid", 30) { NOT_NULL }
  val state =     VARCHAR("state", 30) { DEFAULT("Sent") }
  PRIMARY_KEY(pnihandle)
  FOREIGN_KEY(pihandle).REFERENCES(processInstancesTable.pihandle)
}) {
  val pnihandle by type(BIGINT_T)
  val pihandle by type(BIGINT_T)
  val nodeid by type(VARCHAR_T)
  val state by type(VARCHAR_T)
}


object pnipredecessorsTable: Table("pnipredecessors", EXTRACONF, {
  val pnihandle =   BIGINT("pnihandle") { NOT_NULL }
  val predecessor = BIGINT("predecessor") { NOT_NULL }
  PRIMARY_KEY(pnihandle)
  FOREIGN_KEY(predecessor).REFERENCES(processNodeInstancesTable.pnihandle)
  FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstancesTable.pnihandle)
}) {
  val pnihandle by type(BIGINT_T)
  val predecessor by type(BIGINT_T)
}

object instancedataTable: Table("instancedata", EXTRACONF, {
  val name =     VARCHAR("name", 30) { NOT_NULL }
  val pihandle = BIGINT("pihandle") { NOT_NULL }
                 TEXT("data") { NOT_NULL }
  val isoutput = TINYINT("isoutput") { NOT_NULL }
  PRIMARY_KEY(name, pihandle, isoutput)
  FOREIGN_KEY(pihandle).REFERENCES(processInstancesTable.pihandle)
}) {
  val name by type(VARCHAR_T)
  val pihandle by type(BIGINT_T)
  val data by type(TEXT_T)
  val isoutput by type(TINYINT_T)
}

object nodedataTable: Table("nodedata", EXTRACONF, {
  val name =      VARCHAR("name", 30) { NOT_NULL }
  val pnihandle = BIGINT("pnihandle") { NOT_NULL }
  TEXT("data") { NOT_NULL }
  PRIMARY_KEY(name, pnihandle)
  FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstancesTable.pnihandle)
}) {
  val name by type(VARCHAR_T)
  val pnihandle by type(BIGINT_T)
}

object ProcessEngineDB: Database(1, {
  table(processModelTable)
  table(pmUsersTable)
  table(pmrolesTable)
  table(processInstancesTable)
  table(processNodeInstancesTable)
  table(pnipredecessorsTable)
  table(instancedataTable)
  table(nodedataTable)
}) {
  val processmodels by ref(processModelTable)
  val pmusers by ref(pmUsersTable)
  val pmroles by ref(pmrolesTable)
  val processinstances by ref(processInstancesTable)
  val processnodeinstances by ref(processNodeInstancesTable)
  val pnipredecessors by ref(pnipredecessorsTable)
  val instancedata by ref(instancedataTable)
  val nodedata by ref(nodedataTable)
}
