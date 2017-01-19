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

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.xdescribe
import org.jetbrains.spek.api.include

/**
 * Created by pdvrieze on 15/01/17.
 */
class TestWorkflowPatterns2 : Spek(
  {
    describe("Control flow patterns") {

      describe("Basic control-flow patterns") {

        describe("WCP1: A sequential process") {
          include(WCP1())
        }

        xdescribe("WCP2: Parallel split") {
//          testWCP2()
        }

        xdescribe("WCP3: Synchronization / And join") {
//          testWCP3()
        }

        xdescribe("WCP4: XOR split") {
//          testWCP4()
        }

        xdescribe("WCP5: simple-merge") {
//          testWCP5()
        }
      }

      xdescribe("Advanced branching and synchronization patterns") {
        describe("WCP6: multi-choice / or-split") {
          given("ac1.condition=true, ac2.condition=false") {
//            testWCP6(true, false)
          }
          given("ac1.condition=false, ac2.condition=true") {
//            testWCP6(false, true)
          }
          given("ac1.condition=true, ac2.condition=true") {
//            testWCP6(true, true)
          }

        }

        describe("WCP7: structured synchronized merge") {
          given("ac1.condition=true, ac2.condition=false") {
//            testWCP7(true, false)
          }
          given("ac1.condition=false, ac2.condition=true") {
//            testWCP7(false, true)
          }
          given("ac1.condition=true, ac2.condition=true") {
//            testWCP7(true, true)
          }

        }

        xdescribe("WCP8: Multi-merge", "Multiple instantiations of a single node are not yet supported") {
//          testWCP8()
        }

        describe("WCP9: Structured Discriminator") {
//          testWCP9()
        }
      }

      xdescribe("Structural patterns") {
        xdescribe("WCP10: arbitrary cycles", "Multiple instantiations of a single node are not yet supported") {

        }

        describe("WCP11: Implicit termination") {
//          testWCP11()
        }
      }
    }

    xdescribe("Abstract syntax patterns") {
      describe("WASP4: Vertical modularisation (subprocesses)") {
//        testWASP4()

      }
    }


  })
