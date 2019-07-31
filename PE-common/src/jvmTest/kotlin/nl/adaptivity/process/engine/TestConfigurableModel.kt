/*
 * Copyright (c) 2019.
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

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.engine.XmlProcessNode
import nl.adaptivity.process.processModel.configurableModel.startNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class TestConfigurableModel {

    @Test
    fun testCreateModel() {
        assertEquals("inputCustomerMasterData", Model1.inputCustomerMasterData.id)
    }
}

private val myPrincipal = SimplePrincipal("testPrincipal")

private object Model1: ConfigurableProcessModel<XmlProcessNode>("model1", myPrincipal, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")) {
    val start by startNode
    val inputCustomerMasterData by activity(start) {
    }
    val customerIdentification by activity(inputCustomerMasterData)
}
