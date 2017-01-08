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
 * Created by pdvrieze on 04/01/17.
 */
class ExecutableChildModel : ChildProcessModelBase<ExecutableProcessNode, ExecutableModelCommon>, ExecutableModelCommon {

  open class Builder(
      override val rootBuilder: ExecutableProcessModel.Builder,
      childId:String?=null,
      nodes: Collection<ExecutableProcessNode.Builder> = emptyList(),
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) : ChildProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>(rootBuilder, childId, nodes, imports, exports), ExecutableModelCommon.Builder {

    constructor(rootBuilder: ExecutableProcessModel.Builder, base: ChildProcessModel<*,*>): this(rootBuilder, base.id, base.getModelNodes().map { it.visit(EXEC_BUILDER_VISITOR) }, base.imports, base.exports)


    override fun buildModel(ownerModel: RootProcessModel<ExecutableProcessNode, ExecutableModelCommon>, pedantic: Boolean)
      = ExecutableChildModel(this, ownerModel as ExecutableProcessModel, pedantic)
  }

  override val rootModel get() = super.rootModel as ExecutableProcessModel

  constructor(builder: ChildProcessModel.Builder<*, *>, ownerModel: ExecutableProcessModel, pedantic: Boolean) : super(builder, ownerModel, EXEC_NODEFACTORY, pedantic)

  override fun builder(rootBuilder: RootProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>)
      = ExecutableChildModel.Builder(rootBuilder as ExecutableProcessModel.Builder, id, modelNodes.map(ExecutableProcessNode::builder), imports, exports)
}
