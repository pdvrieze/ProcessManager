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
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.startNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WCP1: A sequential process")
class WCP1: TraceTest(Companion) {

    @Test
    @DisplayName("The model should be correctly named")
    fun testName() {
        assertEquals("WCP1", model.name)
    }

    companion object: TraceTest.ConfigBase() {
        override val modelData: ModelData = run {
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
        }


        override val expectedJson: String
            get() = "{\"imports\":[],\"exports\":[],\"name\":\"WCP1\",\"owner\":\"pdvrieze\",\"roles\":[],\"childModel\":[],\"nodes\":[" +
                "{\"type\":\"start\",\"id\":\"start\",\"label\":null}," +
                "{\"type\":\"activity\",\"id\":\"ac1\",\"label\":null,\"defines\":[],\"results\":[],${DummyMessage.JSON},\"predecessor\":\"start\"}," +
                "{\"type\":\"activity\",\"id\":\"ac2\",\"label\":null,\"defines\":[],\"results\":[],${DummyMessage.JSON},\"predecessor\":\"ac1\"}," +
                "{\"type\":\"end\",\"id\":\"end\",\"label\":null,\"defines\":[],\"results\":[],\"predecessor\":\"ac2\"}]}"

        override val expectedXml: String
            get() = "<pe:processModel xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"WCP1\" owner=\"pdvrieze\">\n" +
                "<pe:start id=\"start\"/>\n" +
                "<pe:activity id=\"ac1\" predecessor=\"start\">" +
                DummyMessage.XML +
                "</pe:activity>\n" +
                "<pe:activity id=\"ac2\" predecessor=\"ac1\">" +
                DummyMessage.XML +
                "</pe:activity>\n" +
                "<pe:end id=\"end\" predecessor=\"ac2\"/>\n" +
                "</pe:processModel>"
    }
}

/*
    <pe:activity id="ac1" predecessor="start">
        <pe:message type="application/x-dummy" url="/dummy" method="POST"/>
    </pe:activity>
<pe:activity id="ac2" predecessor="ac1">
<pe:message type="application/x-dummy" url="/dummy" method="POST"/>
</pe:activity>

 */
