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

package nl.adaptivity.process.engine

import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.spek.describe
import org.jetbrains.spek.api.Spek

/**
 * Created by pdvrieze on 02/01/17.
 */

object TestEngineAspects: Spek({
  givenEngine {
    describe("Subprocess") {
      val model = ExecutableProcessModel.build {
        owner = EngineTestData.principal
        val start1 = startNode { id="start1" }
        val ac1 = activity { id="ac1"; predecessor = start1.identifier }
/*
        val comp = composite {
          id="comp"
          predecessor = ac1
          val start2 = startNode { id="start2" }
          val ac2 = activity { id="ac2"; predecessor=start2 }
          val end2 = endNode { id="end2"; predecessor=ac2 }
        }
*/
      }
      givenProcess(model, "A simple process with a subprocess") {

      }
    }
  }
})
