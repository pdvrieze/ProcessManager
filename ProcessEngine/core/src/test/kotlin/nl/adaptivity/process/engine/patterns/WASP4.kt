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

import nl.adaptivity.process.engine.Model
import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.trace

class WASP4: ModelSpek(run {
  val model = object : Model("WASP4") {
    val start1 by startNode
    val ac1    by activity(start1)

    val comp1 by object : CompositeActivity(ac1) {
      val start2 by startNode
      val ac2    by activity(start2)
      val end2   by endNode(ac2)
    }
    val ac3    by activity(comp1)
    val end    by endNode(ac3)
  }
  val start2 = model.comp1.start2
  val ac2 = model.comp1.ac2
  val end2 = model.comp1.end2

  val validTraces = with(model) { trace{
    start1 .. ac1 .. start2 .. ac2 .. (end2 % comp1) .. ac3 ..end
  }}
  val invalidTraces = with(model) { trace {
    ac1 or comp1 or start2 or ac2 or end2 or ac3 or end or
      (start1 .. (comp1 or start2 or ac2 or end2 or ac3 or end or
        (ac1.. start2.opt .. (comp1 or end2 or ac3 or end or
          ( ac2 .. (comp1.opt % end2.opt) .. end )))))
  }}
  ModelData(model, validTraces, invalidTraces)
})