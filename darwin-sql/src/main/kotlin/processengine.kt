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
import uk.ac.bournemouth.kotlinsql.ImmutableTable
import uk.ac.bournemouth.kotlinsql.MutableTable
import uk.ac.bournemouth.kotlinsql.ColumnType.NumericColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.SimpleColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.CharColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.LengthCharColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.LengthColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.DecimalColumnType.*


/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF = "ENGINE=InnoDB CHARSET=utf8"

object ProcessEngineDB : Database(1) {

  object processModelTable : MutableTable("processmodels", EXTRACONF) {
    val pmhandle by BIGINT("pmhandle") { NOT_NULL; AUTO_INCREMENT }
    val owner by VARCHAR("owner", 30) { NOT_NULL }
    val model by MEDIUMTEXT(name = "model")

    override fun init() {
      INDEX (owner)
      PRIMARY_KEY(pmhandle)
    }
  }

  object pmUsersTable : MutableTable("pmusers", EXTRACONF) {
    val pmhandle by reference(processModelTable.pmhandle) { NOT_NULL }
    val user by VARCHAR("user", 30) { BINARY }
    override fun init() {
      PRIMARY_KEY(pmhandle, user)
      FOREIGN_KEY (pmhandle).REFERENCES(processModelTable.pmhandle)
    }
  }

  object pmrolesTable : MutableTable("pmroles", EXTRACONF) {
    val pmhandle by reference(processModelTable.pmhandle) { NOT_NULL }
    val role by VARCHAR("role", 30)
    override fun init() {
      PRIMARY_KEY(pmhandle, role)
      FOREIGN_KEY (pmhandle).REFERENCES(processModelTable.pmhandle)
    }

  }

  object processInstancesTable : MutableTable("processinstances", EXTRACONF) {
    val pihandle by BIGINT("pihandle") { NOT_NULL; AUTO_INCREMENT }
    val owner by VARCHAR("owner", 30) { NOT_NULL }
    val name by VARCHAR("name", 50)
    val pmhandle by reference(processModelTable.pmhandle) { NOT_NULL }
    val state by VARCHAR("state", 15)
    val uuid by VARCHAR("uuid", 36) { UNIQUE }
    override fun init() {
      INDEX(owner)
      PRIMARY_KEY(pihandle)
      FOREIGN_KEY(pmhandle).REFERENCES(processModelTable.pmhandle)
    }
  }


  object processNodeInstancesTable : MutableTable("processinstances", EXTRACONF) {
    val pnihandle by BIGINT("pnihandle") { NOT_NULL; AUTO_INCREMENT }
    val pihandle by reference(processInstancesTable.pihandle) { NOT_NULL }
    val nodeid by VARCHAR("nodeid", 30) { NOT_NULL }
    val state by VARCHAR("state", 30) { DEFAULT("Sent") }
    override fun init() {
      PRIMARY_KEY(pnihandle)
      FOREIGN_KEY(pihandle).REFERENCES(processInstancesTable.pihandle)
    }
  }


  object pnipredecessorsTable : MutableTable("pnipredecessors", EXTRACONF) {
    val pnihandle by reference(processNodeInstancesTable.pnihandle) { NOT_NULL }
    val predecessor by reference("predecessor", processNodeInstancesTable.pnihandle) { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(pnihandle, predecessor)
      FOREIGN_KEY(predecessor).REFERENCES(processNodeInstancesTable.pnihandle)
      FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstancesTable.pnihandle)
    }
  }

  object instancedataTable : MutableTable("instancedata", EXTRACONF) {
    val name by VARCHAR("name", 30) { NOT_NULL }
    val pihandle by reference(processNodeInstancesTable.pihandle) { NOT_NULL }
    val data by TEXT("data") { NOT_NULL }
    val isoutput by TINYINT("isoutput") { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(name, pihandle, isoutput)
      FOREIGN_KEY(pihandle).REFERENCES(processInstancesTable.pihandle)
    }
  }

  object nodedataTable : MutableTable("nodedata", EXTRACONF) {
    val name by VARCHAR("name", 30) { NOT_NULL }
    val pnihandle by reference(processNodeInstancesTable.pnihandle) { NOT_NULL }
    val data by TEXT("data") { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(name, pnihandle)
      FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstancesTable.pnihandle)
    }
  }

}