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
import nl.adaptivity.process.processModel.RootProcessModelBase.ChildModelProvider

class XmlChildModel(
  builder: ChildProcessModel.Builder<*, *>,
  ownerModel: XmlProcessModel,
  childModelProvider: ChildModelProvider<XmlProcessNode, XmlModelCommon>,
  pedantic: Boolean) :
    ChildProcessModelBase<XmlProcessNode, XmlModelCommon>(
        builder,
        ownerModel,
        childModelProvider,
        pedantic), ChildProcessModel<XmlProcessNode, XmlModelCommon>, XmlModelCommon {

  open class Builder(rootBuilder: XmlProcessModel.Builder,
                     childId:String?=null,
                     nodes: Collection<XmlProcessNode.Builder> = emptyList(),
                     imports: Collection<IXmlResultType> = emptyList(),
                     exports: Collection<IXmlDefineType> = emptyList()) : ChildProcessModelBase.Builder<XmlProcessNode, XmlModelCommon>(rootBuilder, childId, nodes, imports, exports), XmlModelCommon.Builder {
    constructor(rootBuilder: XmlProcessModel.Builder, base: ChildProcessModel<*,*>): this(rootBuilder, base.id, base.getModelNodes().map { it.visit(XML_BUILDER_VISITOR) }, base.imports, base.exports)

    override val rootBuilder: XmlProcessModel.Builder get() = super.rootBuilder as XmlProcessModel.Builder

    override fun buildModel(ownerModel: RootProcessModel<XmlProcessNode, XmlModelCommon>,
                            childModelProvider: ChildModelProvider<XmlProcessNode, XmlModelCommon>,
                            pedantic: Boolean): XmlChildModel {
      return XmlChildModel(this, ownerModel.asM.rootModel, childModelProvider, pedantic)
    }
  }

  override val rootModel: XmlProcessModel get() = super.rootModel as XmlProcessModel

  override fun builder(rootBuilder: RootProcessModel.Builder<XmlProcessNode, XmlModelCommon>): XmlChildModel.Builder {
    return Builder(rootBuilder as XmlProcessModel.Builder, this)
  }

}