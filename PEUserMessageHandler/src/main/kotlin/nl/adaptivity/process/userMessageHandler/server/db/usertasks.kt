/*
 * Copyright (c) 2017.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.ac.db.darwin.usertasks


import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.ddl.MutableTable
import io.github.pdvrieze.kotlinsql.ddl.SqlTypesMixin
import io.github.pdvrieze.kotlinsql.ddl.columns.CustomColumnType
import io.github.pdvrieze.kotlinsql.ddl.columns.NumberColumnConfiguration
import io.github.pdvrieze.kotlinsql.ddl.columns.NumericColumnType
import net.devrieze.util.Handle
import nl.adaptivity.process.userMessageHandler.server.XmlTask

/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF = "ENGINE=InnoDB CHARSET=utf8"

val X_TASKHANDLE =
    CustomColumnType(fun SqlTypesMixin.(): NumberColumnConfiguration<Long, NumericColumnType.BIGINT_T> {
        return BIGINT
    }, Handle<XmlTask>::handleValue, { Handle<XmlTask>(it) })
@Suppress("USELESS_CAST") // The cast is not useless as it changes the result type
val X_REMOTEHANDLE = CustomColumnType({ BIGINT }, Handle<*>::handleValue, { Handle<Any>(it) as Handle<*> })

object UserTaskDB1 : Database(1) {


    object usertasks : MutableTable("usertasks", EXTRACONF) {
        val taskhandle by X_TASKHANDLE { NOT_NULL; AUTO_INCREMENT }
        val remotehandle by X_REMOTEHANDLE { NOT_NULL }

        override fun init() {
            PRIMARY_KEY(taskhandle)
            UserTaskDB.usertasks.isSealed = true
        }
    }


    object nodedata : MutableTable("nodedata", EXTRACONF) {
        val name by VARCHAR(30) { NOT_NULL }
        val taskhandle by reference(usertasks.taskhandle) { NOT_NULL }
        val data by TEXT("data")

        override fun init() {
            PRIMARY_KEY(name, taskhandle)
            FOREIGN_KEY(taskhandle).REFERENCES(usertasks.taskhandle)
            UserTaskDB.usertasks.isSealed = true
        }
    }

}

object UserTaskDB : Database(2) {

    object usertasks : MutableTable("usertasks", EXTRACONF) {
        val taskhandle by X_TASKHANDLE { NOT_NULL; AUTO_INCREMENT }
        val remotehandle by X_REMOTEHANDLE { NOT_NULL }
        val version by INT { NOT_NULL }

        override fun init() {
            PRIMARY_KEY(taskhandle)
            isSealed = true
        }
    }


    val nodedata = UserTaskDB1.nodedata
}
