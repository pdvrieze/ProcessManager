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

class WCP2: ModelSpek(run{
  val model = object : Model("WCP2") {
    val start by startNode
    val split by split(start) { min = 2; max = 2 }
    val ac1   by activity(split)
    val ac2   by activity(split)
    val end1  by endNode(ac1)
    val end2  by endNode(ac2)
  }
  val validTraces = with(model) { trace {
    start .. (
      (ac1 .. end1 .. ac2 ..(split % end2)) or
        (ac2 .. end2 .. ac1 .. (split % end1)))
  } }

  val invalidTraces = with(model) { trace {
    ac1 or ac2 or end1 or end2 or split or
      (start ..(split or
        end1 or
        end2 or
        ((ac1 or
          (ac1..end1)) .. (split or
          end2)) or
        ((ac2 or
          (ac2..end1)) .. (split or
          end1))
               )
      )
  } }
  ModelData(model, validTraces, invalidTraces)
})