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

import nl.adaptivity.process.engine.patterns.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe

/**
 * Created by pdvrieze on 15/01/17.
 */
object TestWorkflowPatterns2 : Spek(
    {
        fun Suite.includeLocal(spec: Spek) {
            val includedBody = spec.root
            val inclusionRoot = InclusionRoot(this)
            inclusionRoot.includedBody()
        }

        describe("control flow patterns") {

/*
            describe("basic control-flow patterns") {

                describe("WCP1: A sequential process") {
                    includeLocal(WCP1)
                }

                describe("WCP2: Parallel split") {
                    includeLocal(WCP2())
                }

                describe("WCP3: Synchronization / And join") {
                    includeLocal(WCP3())
                }

                describe("WCP4: XOR split") {
                    includeLocal(WCP4())
                }

                describe("WCP5: simple-merge") {
                    includeLocal(WCP5())
                }
            }
*/

            describe("Advanced branching and synchronization patterns") {
/*
                describe("WCP6: multi-choice / or-split") {
                    context("ac1.condition=true, ac2.condition=false") {
                        includeLocal(WCP6(true, false))
                    }
                    context("ac1.condition=false, ac2.condition=true") {
                        includeLocal(WCP6(false, true))
                    }
                    context("ac1.condition=true, ac2.condition=true") {
                        includeLocal(WCP6(true, true))
                    }

                }

                describe("WCP7: structured synchronized merge") {
                    context("ac1.condition=true, ac2.condition=false") {
                        includeLocal(WCP7(true, false))
                    }
                    context("ac1.condition=false, ac2.condition=true") {
                        includeLocal(WCP7(false, true))
                    }
                    context("ac1.condition=true, ac2.condition=true") {
                        includeLocal(WCP7(true, true))
                    }

                }

                describe("WCP8: Multi-merge") {
                    includeLocal(WCP8(15))
                }

                describe("WCP9: Structured Discriminator") {
                    includeLocal(WCP9())
                }
*/
            }

            describe("Structural patterns") {
//                describe("WCP10: arbitrary cycles"/*, "Multiple instantiations of a single node are not yet supported"*/) {
//                    includeLocal(WCP10())
//                }

                describe("WCP11: Implicit termination") {
                    includeLocal(WCP11())
                }
            }
        }

        describe("Abstract syntax patterns") {
            describe("WASP4: Vertical modularisation (subprocesses)") {
                includeLocal(WASP4())
            }
        }


    })
