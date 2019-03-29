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
import org.junit.jupiter.api.Assertions.assertEquals

class WCP1 : ModelSpek(run {
    val m = object : ConfigurableModel("WCP1") {
        val start by startNode
        val ac1 by activity(start)
        val ac2 by activity(ac1)
        val end by endNode(ac2)
    }
    with(m) {
        val valid = trace { start..ac1..ac2..end }
        val invalid = trace {
            (start.opt * (ac2 or end)) or
                (start..ac1..end)
        }
        ModelData(m, valid, invalid)
    }
}, {val m = model
                           context("model verification") {
                               it("should be correctly named") {
                                   assertEquals("WCP1", m.name)
                               }
                           }
                       })
