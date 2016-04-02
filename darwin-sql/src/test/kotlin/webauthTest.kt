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
 * Created by pdvrieze on 01/04/16.
 */

package uk.ac.bournemouth.ac.db.darwin.webauth

import org.testng.Assert.*
import org.testng.annotations.Test
import uk.ac.bournemouth.kotlinsql.*
import uk.ac.bournemouth.kotlinsql.ColumnType.*

class WebauthTest {

  @Test
  fun verifyTableCount() {
    assertEquals(WebAuthDB._tables.size, 3)
  }

  @Test(dependsOnMethods = arrayOf("verifyTableCount"))
  fun verifyTablesRecorded() {
    assertTrue(WebAuthDB._tables.contains<Table>(WebAuthDB.roles))
    assertTrue(WebAuthDB._tables.contains<Table>(WebAuthDB.users))
    assertTrue(WebAuthDB._tables.contains<Table>(WebAuthDB.user_roles))
  }

  @Test(dependsOnMethods = arrayOf("verifyTableCount"))
  fun verifyTableNames() {
    assertEquals(WebAuthDB["roles"], WebAuthDB.roles)
    assertEquals(WebAuthDB["users"], WebAuthDB.users)
    assertEquals(WebAuthDB["user_roles"], WebAuthDB.user_roles)
  }

  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
  fun verifyUserRolesRows() {
    assertEquals(WebAuthDB.user_roles._cols.map { it.name }, listOf("user", "role"))
  }


//
//
//  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
//  fun verifyUserTaskPrimaryKey() {
//    assertEquals(WebAuthDB.usertasks._primaryKey?.map { it.name }, listOf("taskhandle"))
//  }
//
//  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
//  fun verifyUserTaskForeignKeys() {
//    assertEquals(WebAuthDB.usertasks._foreignKeys, emptyList<ForeignKey>())
//  }
//
//  @Test(dependsOnMethods = arrayOf("verifyUserTaskRows"))
//  fun verifyUserTaskTaskHandle() {
//    val taskHandle = WebAuthDB.usertasks.taskhandle
//    assertEquals(taskHandle.name, "taskhandle")
//    assertEquals(taskHandle.notnull, true)
//    assertEquals(taskHandle.autoincrement, true)
//    assertNull(taskHandle.columnFormat)
//    assertNull(taskHandle.comment)
//    assertNull(taskHandle.default)
//    assertNull(taskHandle.references)
//    assertNull(taskHandle.storageFormat)
//    assertFalse(taskHandle.unique)
//    assertEquals(taskHandle.table, WebAuthDB.usertasks)
//    val actual: TableRef = taskHandle.table
//    val expected = WebAuthDB["usertasks"]
//    assertEquals(actual, expected)
//    assertEquals(taskHandle.type, BIGINT_T)
//  }
//
//  @Test(dependsOnMethods = arrayOf("verifyUserTaskRows"))
//  fun verifyUserTaskColNames() {
//    assertEquals(WebAuthDB.usertasks["remotehandle"], WebAuthDB.usertasks.remotehandle)
//    assertEquals(WebAuthDB.usertasks["taskhandle"], WebAuthDB.usertasks.taskhandle)
//  }

//  @Test(dependsOnMethods = arrayOf("verifyUserTaskTaskHandle"))
//  fun testUserTasksDDL() {
//    val expected ="""
//      CREATE TABLE `users` (
//        `taskhandle` BIGINT NOT NULL AUTO_INCREMENT,
//        `remotehandle` BIGINT NOT NULL,
//        PRIMARY KEY (`taskhandle`)
//      ) ENGINE=InnoDB CHARSET=utf8;
//    """.trimIndent()
//
//    assertEquals(StringBuilder().apply{WebAuthDB.users.appendDDL(this)}.toString(), expected)
//  }

}