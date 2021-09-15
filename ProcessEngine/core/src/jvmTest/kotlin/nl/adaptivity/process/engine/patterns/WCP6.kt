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
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.split
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.engine.ExecutableXSLTCondition
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@DisplayName("WCP6: multi-choice / or-split")
class WCP6 {

    @Nested
    @DisplayName("ac1.condition=true, ac2.condition=false")
    inner class WCP6Ac1 : TraceTest(Config(true, false))

    @Nested
    @DisplayName("ac1.condition=false, ac2.condition=true")
    inner class WCP6Ac2 : TraceTest(Config(false, true))

    @Nested
    @DisplayName("ac1.condition=true, ac2.condition=true")
    inner class WCP6Ac1Ac2 : TraceTest(Config(true, true))


    class Config(ac1Condition: Boolean, ac2Condition: Boolean) : TraceTest.CompanionBase() {
        override val modelData: ModelData = getModelData(ac1Condition, ac2Condition)
    }

    companion object {

        fun getModelData(ac1Condition: Boolean, ac2Condition: Boolean): ModelData {
            val model = object : TestConfigurableModel("WCP6") {
                val start by startNode
                val split by split(start) { min = 1; max = 2 }
                val ac1 by activity(split) { condition = ExecutableXSLTCondition(ac1Condition.toXPath()) }
                val ac2 by activity(split) { condition = ExecutableXSLTCondition(ac2Condition.toXPath()) }
                val end1 by endNode(ac1)
                val end2 by endNode(ac2)
            }
            val invalidTraces = with(model) {
                when {
                    ac1Condition && ac2Condition  -> buildTrace {
                        start..((ac1..end1.opt..end2) or
                            (ac2..end2.opt..end1))
                    }
                    ac1Condition && !ac2Condition -> buildTrace {
                        start..(end1 or ((ac1..end1.opt).opt * (ac2 or end2)))
                    }
                    !ac1Condition && ac2Condition -> buildTrace {
                        start..(end2 or ((ac2..end2.opt).opt * (ac1 or end1)))
                    }
                    else                          -> kfail("Unsupported condition combination")
                }
            }

            val validTraces = with(model) {
                when {
                    ac1Condition && ac2Condition  -> {
                        trace {
                            (start..( // these are valid
                                (ac1..end1..ac2) or
                                    (ac2..end2..ac1))) * (split % (end1 or end2))
                        }.removeInvalid()
                    }
                    ac1Condition && !ac2Condition -> buildTrace {
                        (start..ac1) * (split % end1)
                    }
                    !ac1Condition && ac2Condition -> buildTrace {
                        (start..ac2) * (split % end2)
                    }
                    else                          -> kfail("All cases need valid traces")
                }
            }

            val baseInvalid = with(model) {
                buildTrace {
                    ac1 or ac2 or (start.opt * (end1 or end2 or split))
                }
            }

            return ModelData(model, validTraces, baseInvalid + invalidTraces)
        }

        @JvmStatic
        inline fun buildTrace(builder: TraceBuilder.() -> List<BTrace>): List<Trace> {
            return TraceBuilder().run(builder).map { it.elems }
        }

    }
}
