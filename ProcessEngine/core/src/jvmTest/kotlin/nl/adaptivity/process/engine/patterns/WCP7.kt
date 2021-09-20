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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@DisplayName("WCP7: structured synchronized merge")
class WCP7 {

    @Nested
    @DisplayName("ac1.condition=true, ac2.condition=false")
    inner class WCP7Ac1 : TraceTest(Config(true, false))

    @Nested
    @DisplayName("ac1.condition=false, ac2.condition=true")
    inner class WCP7Ac2 : TraceTest(Config(false, true))

    @Nested
    @DisplayName("ac1.condition=true, ac2.condition=true")
    inner class WCP7Ac1Ac2 : TraceTest(Config(true, true))

    class Config(ac1Condition: Boolean, ac2Condition: Boolean) : TraceTest.ConfigBase() {
        override val modelData: ModelData = getModelData(ac1Condition, ac2Condition)
    }
    companion object {
        fun getModelData(ac1Condition: Boolean, ac2Condition: Boolean): ModelData {
            val model = object : TestConfigurableModel("WCP7") {
                val start by startNode
                val split by split(start) { min = 1; max = 2 }
                val ac1 by activity(split) { condition = ac1Condition.toCondition() }
                val ac2 by activity(split) { condition = ac2Condition.toCondition() }
                val join by join(ac1, ac2) { min = 1; max = 2 }
                val end by endNode(join)
            }

            val invalidTraces = mutableListOf<Trace>()
            val validTraces = with(model) {
                when {
                    ac1Condition && ac2Condition  -> {
                        invalidTraces.addAll(trace {
                            (start..(ac1 or ac2)) * (end or join or split)
                        })

                        trace {
                            (start..(ac1 % ac2)) * (split % join % end)
                        }
                    }
                    ac1Condition && !ac2Condition -> {
                        invalidTraces.addAll(trace {
                            start..ac1.opt..ac2
                        })

                        trace {
                            start..ac1..end..join..split
                 //        start .. ac1 .. (split % join % end)
                        }

                    }
                    !ac1Condition && ac2Condition -> {
                        invalidTraces.addAll(trace {
                            start..ac2.opt..ac1
                        })


                        trace {
                            (start..ac2) * (split % join % end)
                        }
                    }
                    else                          -> kfail("All cases need valid traces")
                }
            }

            val baseInvalid = with(model) {
                trace {
                    ac1 or ac2 or (start.opt * (end or join or split))
                }
            }

            return ModelData(model, validTraces, baseInvalid + invalidTraces)
        }

    }
}
