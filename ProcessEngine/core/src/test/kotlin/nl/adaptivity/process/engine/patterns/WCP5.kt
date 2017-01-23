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

import nl.adaptivity.process.engine.ConfigurableModel
import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.trace

class WCP5: ModelSpek(run{
  val model = object: ConfigurableModel("WCP5") {
    val start by startNode
    val split by split(start) { min = 1; max = 1 }
    val ac1 by activity(split)
    val ac2 by activity(split)
    val join by join(ac1, ac2) { min = 1; max = 1 }
    val ac3 by activity(join )
    val end by endNode(ac3 )
  }
  val validTraces = with(model) { trace {
    start .. ((ac1 or ac2) .. (((split % join).. ac3 ..end)))
  } }
  val invalidTraces = with(model) { trace {
    ac1 or ac2 or ac3 or end or join or split or
      (start .. (ac3 or join or split or end or
        //        ((ac1 or ac2) .. ac3) or, this passes as the system verify nonexistence of join/split/end nodes
        (ac1 % ac2)))
  } }
  ModelData(model, validTraces, invalidTraces)
})