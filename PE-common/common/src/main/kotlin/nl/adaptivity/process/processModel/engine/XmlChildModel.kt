/*
 * Copyright (c) 2018.
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

import kotlinx.serialization.Serializable
import nl.adaptivity.process.processModel.*

class XmlChildModel(builder: ChildProcessModel.Builder<*, *>,
                    buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>) :
    ChildProcessModelBase<XmlProcessNode, XmlModelCommon>(builder,
                                                          buildHelper), ChildProcessModel<XmlProcessNode, XmlModelCommon>, XmlModelCommon {

    @Serializable
    open class Builder : ChildProcessModelBase.Builder<XmlProcessNode, XmlModelCommon>, XmlModelCommon.Builder {
        constructor(rootBuilder: XmlProcessModel.Builder,
                    childId: String? = null,
                    nodes: Collection<XmlProcessNode.Builder> = emptyList(),
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(rootBuilder, childId, nodes, imports,
                                                                               exports)

        constructor(rootBuilder: XmlProcessModel.Builder, base: ChildProcessModel<*, *>) : this(rootBuilder, base.id,
                                                                                                base.getModelNodes().map {
                                                                                                    it.visit(
                                                                                                        XML_BUILDER_VISITOR)
                                                                                                }, base.imports,
                                                                                                base.exports)

        override val rootBuilder: XmlProcessModel.Builder get() = super.rootBuilder as XmlProcessModel.Builder

        override fun buildModel(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): ChildProcessModel<XmlProcessNode, XmlModelCommon> {
            return XmlChildModel(this, buildHelper)
        }
    }

    override val rootModel: XmlProcessModel get() = super.rootModel as XmlProcessModel

    override fun builder(rootBuilder: RootProcessModel.Builder<XmlProcessNode, XmlModelCommon>): XmlChildModel.Builder {
        return Builder(rootBuilder as XmlProcessModel.Builder, this)
    }

}