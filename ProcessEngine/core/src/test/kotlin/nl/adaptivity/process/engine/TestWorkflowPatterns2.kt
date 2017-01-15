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
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Created by pdvrieze on 15/01/17.
 */
class TestWorkflowPatterns2 : Spek(
  {
    given("WCP1 model") {
      val subject = object : Model("WCP1") {
        val start by startNode
        val ac1 by activity(start)
        val ac2 by activity(ac1)
        val end by endNode(ac2)
      }

      it("should have 4 children") {
        assertEquals(4, subject.getModelNodes().size)
      }

    }
    given("the default engine") {
      val testEngine= EngineTestData.defaultEngine()

    }
  }) {}
