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

import kotlinx.serialization.*
import nl.adaptivity.process.processModel.*
import nl.adaptivity.util.multiplatform.name

@Serializable
class XmlChildModel : ChildProcessModelBase<XmlProcessNode, XmlModelCommon>,
                      ChildProcessModel<XmlProcessNode, XmlModelCommon>,
                      XmlModelCommon {

    constructor(builder: ChildProcessModel.Builder<*, *>,
                buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>) : super(builder,
                                                                                               buildHelper)

    override val rootModel: XmlProcessModel get() = super.rootModel as XmlProcessModel

    override fun builder(rootBuilder: RootProcessModel.Builder<XmlProcessNode, XmlModelCommon>): XmlChildModel.Builder {
        return Builder(rootBuilder as XmlProcessModel.Builder, this)
    }

    @Serializable
    open class Builder : ChildProcessModelBase.Builder<XmlProcessNode, XmlModelCommon>, XmlModelCommon.Builder {

        protected constructor() : super()

        constructor(rootBuilder: XmlProcessModel.Builder,
                    childId: String? = null,
                    nodes: Collection<XmlProcessNode.Builder> = emptyList(),
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(rootBuilder, childId, nodes, imports,
                                                                               exports)

        constructor(rootBuilder: XmlProcessModel.Builder, base: ChildProcessModel<*, *>) : this(rootBuilder, base.id,
                                                                                                base.modelNodes.map {
                                                                                                    it.visit(
                                                                                                        XML_BUILDER_VISITOR)
                                                                                                }, base.imports,
                                                                                                base.exports)

        override val rootBuilder: XmlProcessModel.Builder get() = super.rootBuilder as XmlProcessModel.Builder

        override fun buildModel(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): ChildProcessModel<XmlProcessNode, XmlModelCommon> {
            return XmlChildModel(this, buildHelper)
        }

        companion object : ChildProcessModelBase.Builder.BaseSerializer<Builder>() {
            override val serialClassDesc: KSerialClassDesc
                get() = ChildProcessModelBase.serialClassDesc(Builder::class.name)

            override fun builder(): Builder {
                return Builder()
            }

            override fun save(output: KOutput, obj: Builder) {
                throw UnsupportedOperationException("Cannot be independently saved")
            }
        }
    }

    @Serializer(forClass = XmlChildModel::class)
    companion object : ChildProcessModelBase.BaseSerializer<XmlChildModel>() {
        override val serialClassDesc: KSerialClassDesc = ChildProcessModelBase.serialClassDesc(
            XmlProcessModel::class.name)

        override fun save(output: KOutput, obj: XmlChildModel) {
            super.save(output, obj)
        }

        override fun load(input: KInput): XmlChildModel {
            throw UnsupportedOperationException("A Child model can not be loaded independently of the parent")
        }
    }
}