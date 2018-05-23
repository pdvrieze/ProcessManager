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
class ExecutableChildModel(builder: ChildProcessModel.Builder<*, *>,
                           buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) : ChildProcessModelBase<ExecutableProcessNode, ExecutableModelCommon>(
  builder, buildHelper), ExecutableModelCommon {

    override val rootModel get() = super.rootModel as ExecutableProcessModel

    override val endNodeCount by lazy { modelNodes.count { it is ExecutableEndNode } }

    override fun builder(rootBuilder: RootProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>)
        = ExecutableChildModel.Builder(rootBuilder as ExecutableProcessModel.Builder, id, modelNodes.map(ExecutableProcessNode::builder), imports, exports)

  open class Builder(
      override val rootBuilder: ExecutableProcessModel.Builder,
      childId:String?=null,
      nodes: Collection<ExecutableProcessNode.Builder> = emptyList(),
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) : ChildProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>(rootBuilder, childId, nodes, imports, exports), ExecutableModelCommon.Builder {

    constructor(rootBuilder: ExecutableProcessModel.Builder, base: ChildProcessModel<*,*>): this(rootBuilder, base.id, base.modelNodes.map { it.visit(EXEC_BUILDER_VISITOR) }, base.imports, base.exports)


    override fun buildModel(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>)
      = ExecutableChildModel(this, buildHelper)
  }


}
