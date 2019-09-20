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

import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.TestConfigurableModel
import nl.adaptivity.process.engine.trace
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.join
import nl.adaptivity.process.processModel.configurableModel.startNode

class WCP9: ModelSpek(run{
  val model = object : TestConfigurableModel("WCP9") {
    val start1 by startNode
    val start2 by startNode
    val ac1 by activity(start1)
    val ac2 by activity(start2)
    val join by join(ac1, ac2){ min = 1; max = 1 }
    val ac3 by activity(join)
    val end by endNode(ac3)
  }
  val validTraces = with(model) { trace {
    (start1 % start2) .. (ac1 or ac2) .. join .. ac3 .. end
  }}
  val invalidTraces = with(model) { trace {
    val starts = (start1.opt % start2.opt).filter { it.elems.isNotEmpty() }
    ac1 or ac2 or (starts..(ac3 or end or join)) or
      (starts .. ac1 % ac2)
  }}
  ModelData(model, validTraces, invalidTraces)
})
