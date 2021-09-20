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
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicContainer.dynamicContainer

/**
 * Created by pdvrieze on 15/01/17.
 */
@DisplayName("Test workflow patterns aggregate test")
class TestWorkflowPatterns2 {

    @DisplayName("Basic control-flow patterns")
    @Nested
    inner class BasicPatterns {
        @TestFactory
        @DisplayName("WCP1: A sequential process")
        fun testWCP1() = testPattern(WCP1())

        @TestFactory
        @DisplayName("WCP2: Parallel split")
        fun testWCP2() = testPattern(WCP2())

        @TestFactory
        @DisplayName("WCP3: Synchronization / And join")
        fun testWCP3() = testPattern(WCP3())

        @TestFactory
        @DisplayName("WCP3: Synchronization / And join")
        fun testWCP4() = testPattern(WCP4())

        @TestFactory
        @DisplayName("WCP4: XOR split")
        fun testWCP5() = testPattern(WCP5())

    }

    @Nested
    @DisplayName("Advanced branching and synchronization patterns")
    inner class AdvancedBranchingAndSynchronization {
        @Nested
        @DisplayName("WCP6: multi-choice / or-split")
        inner class WCP6Group {
            @TestFactory
            @DisplayName("ac1.condition = true, ac2.condition=false")
            fun testAc1() = testPattern(WCP6().WCP6Ac1())

            @TestFactory
            @DisplayName("ac1.condition = false, ac2.condition=true")
            fun testAc2() = testPattern(WCP6().WCP6Ac2())

            @TestFactory
            @DisplayName("ac1.condition = true, ac2.condition=true")
            fun testAc1Ac2() = testPattern(WCP6().WCP6Ac1Ac2())

        }
        @Nested
        @DisplayName("WCP7: structured synchronized merge")
        inner class WCP7Group {
            @TestFactory
            @DisplayName("ac1.condition = true, ac2.condition=false")
            fun testAc1() = testPattern(WCP7().WCP7Ac1())

            @TestFactory
            @DisplayName("ac1.condition = false, ac2.condition=true")
            fun testAc2() = testPattern(WCP7().WCP7Ac2())

            @TestFactory
            @DisplayName("ac1.condition = true, ac2.condition=true")
            fun testAc1Ac2() = testPattern(WCP7().WCP7Ac1Ac2())

        }

        @TestFactory
        @DisplayName("WCP8: Multi-merge")
        fun testWCP8() = testPattern(WCP8())

        @TestFactory
        @DisplayName("WCP9: Structured Discriminator")
        fun testWCP9() = testPattern(WCP9())
    }

    @Nested
    @DisplayName("Structural patterns")
    inner class StructuralPatterns {
        @TestFactory
        @DisplayName("WCP10: arbitrary cycles")
        fun testWCP10() = testPattern(WCP10())

        @TestFactory
        @DisplayName("WCP11: Implicit termination")
        fun testWCP11() = testPattern(WCP11())

    }

    @Nested
    @DisplayName("Abstract syntax patterns")
    inner class AbstractSyntaxPatterns {
        @TestFactory
        @DisplayName("WASP4: Vertical modularisation (subprocesses)")
        fun testWASP4() = testPattern(WASP4())
    }

    companion object {
        fun testPattern(testClass: TraceTest): List<DynamicNode> {
            return listOf(
                dynamicContainer(
                    "Valid traces",
                    testClass.validTraces.mapIndexed { idx, trace ->
                        createInvalidTraceTest(testClass.config, trace, idx, false)
                    }
                ),
                dynamicContainer("Invalid traces", testClass.testInvalidTraces())
            )
        }
    }

}
