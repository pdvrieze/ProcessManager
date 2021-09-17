/*
 * Copyright (c) 2018.
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
import nl.adaptivity.process.processModel.engine.ExecutableXSLTCondition

/**
 * Test based on
 * https://bpmai.org/foswiki/pub/BPMAcademicInitiative/AnalyzeProcessModels/ex1_execution_traces.pdf
 */
class WebProcess1 : TraceTest(Companion) {
    companion object: TraceTest.CompanionBase() {
        override val modelData: ModelData = run {
            val m = object : TestConfigurableModel("Signavio-insurance-emergency") {
                val start by startNode { label = "Insurance emergency" }

                val split1 by split(start) {
                    min = 2
                    max = 2
                }

                val ac1 by activity(split1) {
                    label = "Analyze insurance agreement"
                    result { name="result"; path="/" }
                }

                val split2 by split(ac1) {
                    min = 1
                    max = 1
                }

                val ac2 by activity(split1) {
                    label = "Offer immediate help"
                    result { name="result"; path="/" }
                }

                val split3 by split(ac2) {
                    min = 1
                    max = 1
                }

                val join1 by this.let { tcm ->
                    join(split2, split3) {
                        conditions[tcm.split2] = ExecutableXSLTCondition("pe:node('ac1')/coverage_exists", "There is coverage")
                        conditions[tcm.split3] = ExecutableXSLTCondition("pe:node('ac2')/accepted", "Offer accepted")
                        min = 2
                        max = 2
                    }
                }

                val ac3 by activity(split2) {
                    condition = ExecutableXSLTCondition("otherwise", "No coverage")
                    label = "Send out offer for emergency help"
                }

                val ac4 by activity(join1) {
                    label = "Do internal accounting"
                }

                val ac5 by activity(split3) {
                    label = "Ask for rejection notification"
                    condition = ExecutableXSLTCondition("otherwise", "Offer rejected")
                }

                val join2 by join(ac4, ac5) {
                    min = 1
                    max = 1
                }

                val join3 by join(ac3, join2) {
                    min = 1
                    max = 1
                }

                val end by endNode(join3)
            }
            with(m) {
                val valid = trace {
//                    (start..ac1..ac2..split1..(
//                        ((ac3 .. split2) % (ac5..split3..join2)) or
//                            (join1..(split2 % split3)..ac4..join2)
//                        )..join3..end
//                        ) or (
                        start..(ac1("<coverage_exists/>") .. ac2("<accepted/>")..split1.. split2..split3..join1..ac4..join2..join3..end)
//                        )
                }
                val invalid = trace {
                    (start.opt * (split2 or split3 or ac3 or ac4 or join1 or join2 or join3 or end)) or
                        (start..ac1..end)
                }
                ModelData(m, valid, /*invalid*/ emptyList())
            }
        }
    }
}
