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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.ac.db.darwin.processengine

import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.MutableTable


/**
 * Created by pdvrieze on 31/03/16.
 */

const val EXTRACONF = "ENGINE=InnoDB CHARSET=utf8"

object ProcessEngineDB : Database(1) {

  object processModels : MutableTable("processmodels", EXTRACONF) {
    val pmhandle by BIGINT { NOT_NULL; AUTO_INCREMENT }
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
      FOREIGN_KEY (pmhandle).REFERENCES(processModels.pmhandle)
    }
  }

  object pmroles : MutableTable("pmroles", EXTRACONF) {
    val pmhandle by reference(processModels.pmhandle) { NOT_NULL }
    val role by VARCHAR(30)
    override fun init() {
      PRIMARY_KEY(pmhandle, role)
      FOREIGN_KEY (pmhandle).REFERENCES(processModels.pmhandle)
    }

  }

  object processInstances : MutableTable("processinstances", EXTRACONF) {
    val pihandle by BIGINT { NOT_NULL; AUTO_INCREMENT }
    val owner by VARCHAR(30) { NOT_NULL }
    val name by VARCHAR(50)
    val pmhandle by reference(processModels.pmhandle) { NOT_NULL }
    val state by VARCHAR(15)
    val uuid by VARCHAR(36) { UNIQUE; NOT_NULL }
    val parentActivity by reference(processNodeInstances.pnihandle) { NULL }
    override fun init() {
      INDEX(owner)
      PRIMARY_KEY(pihandle)
      FOREIGN_KEY(pmhandle).REFERENCES(processModels.pmhandle)
    }
  }


  object processNodeInstances : MutableTable("processnodeinstances", EXTRACONF) {
    val pnihandle by BIGINT { NOT_NULL; AUTO_INCREMENT }
    val pihandle by reference(processInstances.pihandle) { NOT_NULL }
    val nodeid by VARCHAR(30) { NOT_NULL }
    val state by VARCHAR(30) { DEFAULT("Sent") }
    override fun init() {
      PRIMARY_KEY(pnihandle)
      FOREIGN_KEY(pihandle).REFERENCES(processInstances.pihandle)
      UNIQUE(pihandle, nodeid)
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
    val data by TEXT() { NOT_NULL }
    val isoutput by BIT() { NOT_NULL }
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
