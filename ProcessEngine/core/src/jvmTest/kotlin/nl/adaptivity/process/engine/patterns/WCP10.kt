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
import nl.adaptivity.process.processModel.configurableModel.*
import nl.adaptivity.process.util.Identifier
import org.junit.jupiter.api.DisplayName

@DisplayName("WCP10: arbitrary cycles")
class WCP10: TraceTest(Companion) {
    companion object: TraceTest.CompanionBase() {
        override val modelData: ModelData = run {
            val model = object : TestConfigurableModel("WCP10") {
                val start1 by startNode
                val join by join(start1, Identifier("ac2")) { min = 1; max = 1; isMultiMerge = true }
                val ac1 by activity(join) { isMultiInstance = true }
                val split by split(ac1) { min = 1; max = 1; isMultiInstance = true }
                val ac2 by activity(split) { isMultiInstance = true }
                val ac3 by activity(split)
                val end by endNode(ac3)/* { isMultiInstance = true }*/
            }
            val validTraces = with(model) { trace {
                (start1 .. join[1] .. ac1[1] .. ac2[1] .. split[1] .. join[2] .. ac1[2] .. ac3 .. split[2] ..end) or
                    (start1 .. join[1] .. ac1[1] .. ac3 .. split[1] ..end)
            }}
            val invalidTraces = with(model) { trace {
                join or ac2 or ac3 or end or
                    ((start1 .. join[1] .. ac1[1] .. ac2[1] .. split[1]) * (ac3[ANYINSTANCE] or split[2] or end[ANYINSTANCE]))
            }}
            ModelData(model, validTraces, invalidTraces)
        }
    }
}
