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

import nl.adaptivity.process.engine.TestConfigurableModel
import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.trace
import org.junit.jupiter.api.Assertions.assertEquals
import nl.adaptivity.process.processModel.configurableModel.*

private const val expectedWCP1Json = "{\"name\":\"WCP1\",\"owner\":\"pdvrieze\",\"roles\":null,\"uuid\":null,\"childModel\":[],\"import\":[],\"export\":[],\"nodes\":[" +
                                     "[\"nl.adaptivity.process.processModel.engine.XmlStartNode\"," +
                                     "{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"start\",\"label\":null}]," +
                                     "[\"nl.adaptivity.process.processModel.engine.XmlActivity\"," +
                                     "{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"ac1\",\"label\":null,\"predecessor\":\"start\",\"message\":null,\"childId\":null}]," +
                                     "[\"nl.adaptivity.process.processModel.engine.XmlActivity\"," +
                                     "{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"ac2\",\"label\":null,\"predecessor\":\"ac1\",\"message\":null,\"childId\":null}]," +
                                     "[\"nl.adaptivity.process.processModel.engine.XmlEndNode\"," +
                                     "{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"end\",\"label\":null,\"predecessor\":\"ac2\"}]" +
                                     "]}"

object WCP1 : ModelSpek(run {
    val m = object : TestConfigurableModel("WCP1") {
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
                       }, modelJson = expectedWCP1Json)
