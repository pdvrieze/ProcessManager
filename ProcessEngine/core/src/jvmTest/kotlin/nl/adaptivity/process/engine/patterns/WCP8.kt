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
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.join
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.invoke
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("slow")
class WCP8() : TraceTest(Companion) {

    @DisplayName("Model is correct")
    inner class TestModelCorrect() {
        @Test
        @DisplayName("Should have join as multimerge")
        fun hasJoinAsMultimerge() {
            assertTrue((model.getNode("join") as Join).isMultiMerge)
        }

        @Test
        @DisplayName("should have ac5 as multiInstance")
        fun hasAc5AsMultiInstance() {
            assertTrue((model.getNode("ac5") as Activity).isMultiInstance)
        }
    }

    @Nested
    @DisplayName("When join is not multiInstance, valid traces are invalid")
    inner class TestNotMultiInstance : TraceTest(ModifyToInvalid { join("join")!! { isMultiMerge = false } }) {
        @DisplayName("it should not have a multiInstance join")
        @Test
        fun testNoMultiInstanceJoin() {
            val node = model.getNode("join") ?: fail("Node \"join\" not found")
            val join = node as? Join ?: fail("Node \"join\" is not a Join")

            assertFalse(join.isMultiMerge)
        }
    }

    @Nested
    @DisplayName("When ac5 is not multiInstance")
    inner class TestAc5NotMultiInstance: TraceTest(ModifyToInvalid { activity("ac5")!! { isMultiInstance = false } })

    @Nested
    @DisplayName("When end is not multiInstance")
    inner class TestEndNotMultiInstance: TraceTest(ModifyToInvalid { endNode("end")!! { isMultiInstance = false } })

    private fun ModifyToInvalid(
        newValidTraces: List<Trace> = emptyList(),
        newInvalidTraces: List<Trace> = modelData.valid,
        update: RootProcessModel.Builder.() -> Unit
    ) = ModifyToInvalid(model, newValidTraces, newInvalidTraces, update)

    private class ModifyToInvalid(
        baseModel: ExecutableProcessModel,
        newValidTraces: List<Trace>,
        newInvalidTraces: List<Trace>,
        update: RootProcessModel.Builder.() -> Unit
    ) : TraceTest.CompanionBase() {
        override val modelData: ModelData = ModelData(
            baseModel.update(update),
            newValidTraces,
            newInvalidTraces
        )
    }

    companion object : TraceTest.CompanionBase() {
        override val modelData: ModelData = run {
            val model = object : TestConfigurableModel("WCP8") {
                val start1 by startNode
                val start2 by startNode
                val ac1 by activity(start1)
                val ac2 by activity(ac1)

                val ac3 by activity(start2)
                val ac4 by activity(ac3)
                val join by join(ac2, ac4) { min = 1; max = 1; isMultiMerge = true }
                val ac5 by activity(join) { isMultiInstance = true }
                val end by endNode(ac5) { isMultiInstance = true }
            }

            val validTraces = with(model) {
                trace {
                    (start1 % start2) * (
                        ((((ac1..ac2) / (+ac3))..join[1]) * ((ac5[1]..end[1]) / (ac4..join[2]..ac5[2]..end[2]))) or
                            ((((+ac1) / (ac3..ac4))..join[1]) * ((ac5[1]..end[1]) / (ac2..join[2]..ac5[2]..end[2])))
                        )
                }
            }

            val invalidTraces = with(model) {
                trace {
                    ac2 or ac4 or ac5 or end or join or
                        (((start1 % start2) or start1 or start2) * (ac2 or ac4 or join or ac5 or end))
                }
            }

            ModelData(model, validTraces, invalidTraces)
        }
    }
}
