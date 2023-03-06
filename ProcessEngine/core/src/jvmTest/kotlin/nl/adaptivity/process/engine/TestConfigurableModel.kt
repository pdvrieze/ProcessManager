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

import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID

@Suppress("NOTHING_TO_INLINE")
internal abstract class TestConfigurableModel(
    name: String? = null,
    owner: PrincipalCompat = EngineTestData.principal,
    uuid: UUID = UUID.randomUUID()
) : ConfigurableProcessModel<ExecutableProcessNode>(
        name,
        owner,
        uuid
    ) {

    override val rootModel: ExecutableProcessModel by lazy {
        buildModel {
            it.uuid = null

            ExecutableProcessModel(it, false)
        }
    }

}
