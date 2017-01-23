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

package nl.adaptivity.process.engine.patterns

import nl.adaptivity.process.engine.*
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.invoke
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.dsl.SubjectDsl
import org.junit.jupiter.api.Assertions.assertFalse

class WCP8: ModelSpek(run {
  val model = object : ConfigurableModel("WCP8") {
    val start1 by startNode
    val start2 by startNode
    val ac1    by activity(start1)
    val ac2    by activity(start2)
    val join   by join(ac1, ac2) { min = 1; max = 1; isMultiMerge=true }
    val ac3    by activity(join) { isMultiInstance=true }
    val end    by endNode(ac3) { isMultiInstance=true }
  }

  val validTraces = with(model) { trace {
    val t1 = ac3[1] .. end[1]
    val t2 = ac3[2] .. end[2]
    val h2 = (ac1 or ac2) .. join[2]

    (start1 % start2) .. (ac1 or ac2) .. join[1] ..
      (((t1 % h2).. t2) or (h2 .. t2 .. t1))
  }}.removeInvalid()

  val invalidTraces = with(model) { trace{
    ac3 or end or join or
      (((start1 % start2) or start1 or start2) .. (join or ac3 or end))
  }}

  ModelData(model, validTraces, invalidTraces)
}, {
  val t: CustomDsl = this
  group("When join is not multiInstance") {
    val modifiedModel = model.update { join("join")!! { isMultiMerge = false } }
    it("should not have a multiInstance join") {
      assertFalse((modifiedModel.getNode("join") as? Join<*,*>)?.isMultiMerge ?: true)
    }
    for (trace in valid) {
      t.testInvalidTrace(this, modifiedModel, model.owner, trace)
    }
  }
  group("When ac3 is not multiInstance") {
    val modifiedModel = model.update { activity("ac3")!! { isMultiInstance = false } }
    for (trace in valid) {
      t.testInvalidTrace(this, modifiedModel, model.owner, trace)
    }
  }
  group("When end is not multiInstance") {
    val modifiedModel = model.update { endNode("end")!! { isMultiInstance = false } }
    for (trace in valid) {
      t.testInvalidTrace(this, modifiedModel, model.owner, trace)
    }
  }
})