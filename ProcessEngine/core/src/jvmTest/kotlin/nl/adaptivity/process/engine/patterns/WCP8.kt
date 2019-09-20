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
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.join
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.invoke
import nl.adaptivity.spek.lenientFactory
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Tag
import org.spekframework.spek2.CreateWith

@CreateWith(lenientFactory::class)
@Tag("slow")
class WCP8(maxValidTraces:Int, maxInvalidTraces: Int = maxValidTraces): ModelSpek(run {
    val model = object : TestConfigurableModel("WCP8") {
        val start1 by startNode
        val start2 by startNode
        val ac1    by activity(start1)
        val ac2    by activity(ac1)

        val ac3 by activity(start2)
        val ac4 by activity(ac3)
        val join   by join(ac2, ac4) { min = 1; max = 1; isMultiMerge=true }
        val ac5 by activity(join) { isMultiInstance=true }
        val end    by endNode(ac5) { isMultiInstance=true }
    }

    val validTraces = with(model) { trace {
        (start1 % start2) * (
            ((((ac1 .. ac2) / (+ac3)) .. join[1]) * ((ac5[1] .. end[1]) / (ac4 .. join[2] .. ac5[2] .. end[2]))) or
                ((((+ac1) / (ac3 .. ac4)) .. join[1]) * ((ac5[1] .. end[1]) / (ac2 .. join[2] .. ac5[2] .. end[2])))
                            )
    }}

    val invalidTraces = with(model) { trace{
        ac2 or ac4 or ac5 or end or join or
            (((start1 % start2) or start1 or start2) * (ac2 or ac4 or join or ac5 or end))
    }}

    ModelData(model, validTraces, invalidTraces)
}, {
    val m = model
    val c = this
    context("When join is not multiInstance") {
        val modifiedModel = m.update { join("join")!! { isMultiMerge = false } }
        it("should not have a multiInstance join") {
            assertFalse((modifiedModel.getNode("join") as? Join)?.isMultiMerge ?: true)
        }
        for (trace in c.valid) {
            testInvalidTrace(modifiedModel, m.owner, trace)
        }
    }
    context("When ac3 is not multiInstance") {
        val modifiedModel = m.update { activity("ac5")!! { isMultiInstance = false } }
        for (trace in c.valid) {
            testInvalidTrace(modifiedModel, m.owner, trace)
        }
    }
    context("When end is not multiInstance") {
        val modifiedModel = m.update { endNode("end")!! { isMultiInstance = false } }
        for (trace in c.valid) {
            testInvalidTrace(modifiedModel, m.owner, trace)
        }
    }
}, maxValidTraces, maxInvalidTraces) {
    // Used when instantiating as an independent test
    @Suppress("unused")
    constructor() : this(Int.MAX_VALUE)
}
