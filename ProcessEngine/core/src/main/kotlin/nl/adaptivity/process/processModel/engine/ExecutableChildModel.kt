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

  class Builder(nodes: Collection<ProcessNode.Builder<ExecutableProcessNode, ExecutableModelCommon>>, imports: Collection<IXmlResultType>, exports: Collection<IXmlDefineType>) : ChildProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>(null, nodes, imports, exports), ExecutableModelCommon.Builder {

    override fun buildModel(ownerModel: ExecutableModelCommon, pedantic: Boolean): ExecutableChildModel {
      return ExecutableChildModel(this, ownerModel, pedantic)
    }
  }

  constructor(builder: Builder, ownerModel: ExecutableModelCommon, pedantic: Boolean) : super(builder, ownerModel, pedantic)

  override fun builder() = ExecutableChildModel.Builder(modelNodes.map(ExecutableProcessNode::builder), imports, exports)
}