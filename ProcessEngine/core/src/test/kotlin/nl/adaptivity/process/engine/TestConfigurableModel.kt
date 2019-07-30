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

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import java.util.*
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
internal abstract class TestConfigurableModel(
    name: String? = null,
    owner: Principal = EngineTestData.principal,
    uuid: UUID = UUID.randomUUID()
                                             ) :
    ConfigurableProcessModel<ExecutableProcessNode>(
        name,
        owner,
        uuid
                                                   ) {

    override val rootModel: ExecutableProcessModel by lazy {
        buildModel {
            it.uuid = null;ExecutableProcessModel(
            it,
            false
                                                 )
        }
    }

    override fun copy(
        imports: Collection<IXmlResultType>,
        exports: Collection<IXmlDefineType>,
        nodes: Collection<ProcessNode>,
        name: String?,
        uuid: UUID?,
        roles: Set<String>,
        owner: Principal,
        childModels: Collection<ChildProcessModel<ExecutableProcessNode>>
                     ): ExecutableProcessModel {
        return ExecutableProcessModel.Builder(
            nodes.map { it.builder() }, emptySet(), name, -1L, owner, roles,
            uuid
                                             ).also { builder ->
            builder.childModels.replaceBy(childModels.map { it.builder(builder) })
        }.let { ExecutableProcessModel(it, false) }
    }

}
