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

class XmlChildModel(builder: Builder, ownerModel: XmlModelCommon, pedantic: Boolean) : ChildProcessModelBase<XmlProcessNode, XmlModelCommon>(builder, ownerModel, pedantic), ChildProcessModel<XmlProcessNode, XmlModelCommon>, XmlModelCommon {

  open class Builder(childId:String?=null, nodes: Collection<XmlProcessNode.Builder>, imports: Collection<IXmlResultType>, exports: Collection<IXmlDefineType>) : ChildProcessModelBase.Builder<XmlProcessNode, XmlModelCommon>(childId, nodes, imports, exports), XmlModelCommon.Builder {
    constructor(base: XmlChildModel): this(base.childId?.id, base.getModelNodes().map { it.builder() }, base.imports, base.exports)

    override fun buildModel(ownerModel: XmlModelCommon, pedantic: Boolean): ChildProcessModelBase<XmlProcessNode, XmlModelCommon> {
      return XmlChildModel(this, ownerModel, pedantic)
    }
  }

  override val rootModel: RootProcessModel<XmlProcessNode, XmlModelCommon>
    get() = super<ChildProcessModelBase>.rootModel!!

  override fun builder(): Builder {
    return Builder(this)
  }

}