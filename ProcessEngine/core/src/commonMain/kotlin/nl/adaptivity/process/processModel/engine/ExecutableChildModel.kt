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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.processModel.*

/**
 * Child model extension that has the behaviour needed for execution.
 */
class ExecutableChildModel(
    builder: ChildProcessModel.Builder,
    buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>
) : ChildProcessModelBase<ExecutableProcessNode>(
    builder, buildHelper
), ExecutableModelCommon {

    override val rootModel get() = super.rootModel as ExecutableProcessModel

    override val endNodeCount by lazy { modelNodes.count { it is ExecutableEndNode } }

    override fun builder(rootBuilder: RootProcessModel.Builder): Builder {
        return Builder(
            rootBuilder, id, modelNodes.map(ExecutableProcessNode::builder), imports,
            exports
        )
    }

    open class Builder(
        override val rootBuilder: RootProcessModel.Builder,
        childId: String? = null,
        nodes: Collection<ProcessNode.Builder> = emptyList(),
        imports: Collection<IXmlResultType> = emptyList(),
        exports: Collection<IXmlDefineType> = emptyList()
    ) : ModelBuilder(
        rootBuilder,
        childId,
        nodes,
        imports,
        exports
    ) {

        constructor(rootBuilder: RootProcessModel.Builder, base: ChildProcessModel<*>)
            : this(
            rootBuilder,
            base.id,
            base.modelNodes.map { it.visit(EXEC_BUILDER_VISITOR) },
            base.imports,
            base.exports
        )

    }

}
