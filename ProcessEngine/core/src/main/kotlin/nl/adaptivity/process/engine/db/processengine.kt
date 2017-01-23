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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import uk.ac.bournemouth.kotlinsql.MutableTable
import uk.ac.bournemouth.kotlinsql.customType
import java.util.*


/**
 * Defines the database used by the process engine
 */

const val EXTRACONF = "ENGINE=InnoDB CHARSET=utf8"

object ProcessEngineDB : uk.ac.bournemouth.kotlinsql.Database(1) {

  val X_UUID = customType({ VARCHAR(36) { UNIQUE } }, UUID::toString, UUID::fromString)
  val X_PMHANDLE = customType({ BIGINT }, Handle<SecureObject<ExecutableProcessModel>>::handleValue, { Handle<SecureObject<ExecutableProcessModel>>(it) })
  val X_PIHANDLE = customType({ BIGINT }, Handle<SecureObject<ProcessInstance>>::handleValue, {Handle<SecureObject<ProcessInstance>>(it)})
  val X_PNIHANDLE = customType({ BIGINT }, Handle<SecureObject<ProcessNodeInstance>>::handleValue, {Handle<SecureObject<ProcessNodeInstance>>(it)})
  val X_INSTANCESTATE = customType({ VARCHAR(20) }, ProcessInstance.State::toString, ProcessInstance.State::valueOf )
  val X_NODESTATE = customType({ VARCHAR(20) { DEFAULT("Sent") } }, NodeInstanceState::toString,
                               { val lcname=it.toLowerCase(Locale.ENGLISH); NodeInstanceState.values().first { it.lcname == lcname } })

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
    val parentActivity by reference(processNodeInstances.pnihandle) { NULL }
    override fun init() {
      INDEX(owner)
      PRIMARY_KEY(pihandle)
      FOREIGN_KEY(pmhandle).REFERENCES(processModels.pmhandle)
    }
  }


  object processNodeInstances : MutableTable("processnodeinstances", EXTRACONF) {
    val pnihandle by X_PNIHANDLE
    val pihandle by reference(processInstances.pihandle) { NOT_NULL }
    val nodeid by VARCHAR(30) { NOT_NULL }
    val entryno by INT
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
    val data by TEXT() { NOT_NULL }
    override fun init() {
      PRIMARY_KEY(name, pnihandle)
      FOREIGN_KEY(pnihandle).REFERENCES(processNodeInstances.pnihandle)
    }
  }

}
