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

package nl.adaptivity.process.engine.db

import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.ddl.MutableTable
import io.github.pdvrieze.kotlinsql.ddl.columns.CustomColumnType
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.engine.PMHandle
import java.util.*


/**
 * Defines the database used by the process engine
 */

const val EXTRACONF = "ENGINE=InnoDB CHARSET=utf8"

object ProcessEngineDB : Database(1) {

  val X_UUID = CustomColumnType({ VARCHAR(36) { UNIQUE } }, UUID::toString, UUID::fromString)
  val X_PMHANDLE = CustomColumnType({ BIGINT }, PMHandle::handleValue, { Handle(it) })
  val X_PIHANDLE = CustomColumnType({ BIGINT }, PIHandle::handleValue, {PIHandle(it)})
  val X_PNIHANDLE = CustomColumnType({ BIGINT }, PNIHandle::handleValue, {PNIHandle(it)})
  val X_INSTANCESTATE = CustomColumnType({ VARCHAR(20) }, ProcessInstance.State::toString, ProcessInstance.State::valueOf)
  val X_NODESTATE = CustomColumnType({ VARCHAR(20) { DEFAULT("Sent") } }, NodeInstanceState::toString,
                               { val lcname= it.lowercase(Locale.ENGLISH); NodeInstanceState.values().first { it.lcname == lcname } })

  object processModels : MutableTable("processmodels", EXTRACONF) {
    val pmhandle by X_PMHANDLE { NOT_NULL; AUTO_INCREMENT }
    val owner by VARCHAR(30) { NOT_NULL }
    val model by MEDIUMTEXT()

    override fun init() {
      INDEX (owner)
      PRIMARY_KEY(pmhandle)
    }
  }

  object pmUsers : MutableTable("pmusers", EXTRACONF) {
    val pmhandle by reference(processModels.pmhandle) { NOT_NULL }
    val user by VARCHAR(30) { BINARY }
    override fun init() {
      PRIMARY_KEY(pmhandle, user)
      FOREIGN_KEY(pmhandle).REFERENCES(processModels.pmhandle)
    }
  }

  object pmroles : MutableTable("pmroles", EXTRACONF) {
    val pmhandle by reference(processModels.pmhandle) { NOT_NULL }
    val role by VARCHAR(30)
    override fun init() {
      PRIMARY_KEY(pmhandle, role)
      FOREIGN_KEY(pmhandle).REFERENCES(processModels.pmhandle)
    }

  }

  object processInstances : MutableTable("processinstances", EXTRACONF) {
    val pihandle by X_PIHANDLE { NOT_NULL; AUTO_INCREMENT }
    val owner by VARCHAR(30) { NOT_NULL }
    val name by VARCHAR(50)
    val pmhandle by reference(processModels.pmhandle) { NOT_NULL }
    val state by X_INSTANCESTATE
    val uuid by X_UUID { UNIQUE; NOT_NULL }
    val parentActivity by reference("parentActivity", processNodeInstances.pnihandle) { NULL }
    override fun init() {
      INDEX(owner)
      PRIMARY_KEY(pihandle)
      FOREIGN_KEY(pmhandle).REFERENCES(processModels.pmhandle)
    }
  }


  object processNodeInstances : MutableTable("processnodeinstances", EXTRACONF) {
    val pnihandle by X_PNIHANDLE { NOT_NULL; AUTO_INCREMENT }
    val pihandle by reference(processInstances.pihandle) { NOT_NULL }
    val nodeid by VARCHAR(30) { NOT_NULL }
    val entryno by INT
    val assigneduser by VARCHAR(200)
    val state by X_NODESTATE
    override fun init() {
      PRIMARY_KEY(pnihandle)
      FOREIGN_KEY(pihandle).REFERENCES(processInstances.pihandle)
      UNIQUE(pihandle, nodeid, entryno)
    }
  }


  object pnipredecessors : MutableTable("pnipredecessors", EXTRACONF) {
    val pnihandle by reference(processNodeInstances.pnihandle) { NOT_NULL }
    val predecessor by reference("predecessor", processNodeInstances.pnihandle) { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(pnihandle, predecessor)
      FOREIGN_KEY(predecessor).REFERENCES(processNodeInstances.pnihandle)
      FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstances.pnihandle)
    }
  }

  object instancedata : MutableTable("instancedata", EXTRACONF) {
    val name by VARCHAR(30) { NOT_NULL }
    val pihandle by reference(processNodeInstances.pihandle) { NOT_NULL }
    val data by TEXT { NOT_NULL }
    val isoutput by BIT { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(name, pihandle, isoutput)
      FOREIGN_KEY(pihandle).REFERENCES(processInstances.pihandle)
    }
  }

  object nodedata : MutableTable("nodedata", EXTRACONF) {
    val name by VARCHAR(30) { NOT_NULL }
    val pnihandle by reference(processNodeInstances.pnihandle) { NOT_NULL }
    val data by TEXT { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(name, pnihandle)
      FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstances.pnihandle)
    }
  }

}
