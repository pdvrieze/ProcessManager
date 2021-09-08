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

package nl.adaptivity.process.processModel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XML_BUILDER_VISITOR
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.util.IdentifiableSetSerializer
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.jvm.JvmField

/**
 * Base class for submodels
 */
abstract class ChildProcessModelBase<NodeT : ProcessNode> :
    ProcessModelBase<NodeT>, ChildProcessModel<NodeT> {

    @SerialName("nodes")
    @XmlPolyChildren(
        arrayOf(
            "nl.adaptivity.process.processModel.engine.XmlActivity\$Builder=pe:activity",
            "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
            "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
            "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
            "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
            "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
            "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
            "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
            "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
            "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"
        )
    )
    @Serializable(IdentifiableSetSerializer::class)
    override val modelNodes: IdentifyableSet<NodeT>

    @Transient
    private var _rootModel: RootProcessModel<NodeT> = XmlProcessModel(RootProcessModelBase.Builder()) as RootProcessModel<NodeT>

    override val rootModel: RootProcessModel<NodeT>
        get() = _rootModel

    @SerialName("id")
    override val id: String?

    @Suppress("LeakingThis")
    constructor(
        builder: ChildProcessModel.Builder,
        buildHelper: ProcessModel.BuildHelper<NodeT, ProcessModel<NodeT>, *, *>
   ) : super(builder, buildHelper.pedantic) {
        modelNodes = buildNodes(builder, buildHelper.withOwner(this))
        val newOwner: ProcessModel<NodeT> = buildHelper.newOwner
        _rootModel = newOwner.rootModel
        this.id = builder.childId
    }

    abstract override fun builder(rootBuilder: RootProcessModel.Builder): ModelBuilder

    @Serializable
    @XmlSerialName(ChildProcessModelBase.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @SerialName(ChildProcessModelBase.ELEMENTLOCALNAME)
    internal class SerialDelegate: ProcessModelBase.SerialDelegate {
        val id: Identifier?

        constructor(
            imports: List<XmlResultType>,
            exports: List<XmlDefineType>,
            nodes: List<ProcessNodeBase.SerialDelegate>,
            id: Identifier?
        ) : super(imports, exports, nodes) {
            this.id = id
        }

        constructor(base: ChildProcessModel<*>): this(
            base.imports.map { XmlResultType(it) },
            base.exports.map { XmlDefineType(it) },
            base.modelNodes.map { ProcessNodeBase.SerialDelegate(it) },
            base.identifier,
        )

        constructor(base: ChildProcessModel.Builder): this(
            base.imports.map { XmlResultType(it) },
            base.exports.map { XmlDefineType(it) },
            base.nodes.map { ProcessNodeBase.SerialDelegate(it) },
            base.childId?.let { Identifier(it) },
        )
    }

    open class ModelBuilder : ProcessModelBase.Builder, ChildProcessModel.Builder {

        @Transient
        private lateinit var _rootBuilder: RootProcessModel.Builder

        @Transient
        override val rootBuilder: RootProcessModel.Builder
            get() = _rootBuilder

        @SerialName("id")
        final override var childId: String?

        protected constructor() {
            childId = null
        }

        constructor(
            rootBuilder: RootProcessModel.Builder,
            childId: String? = null,
            nodes: Collection<ProcessNode.Builder> = emptyList(),
            imports: Collection<IXmlResultType> = emptyList(),
            exports: Collection<IXmlDefineType> = emptyList()
        ) : super(nodes, imports, exports) {
            this._rootBuilder = rootBuilder
            this.childId = childId
        }

        constructor(rootBuilder: RootProcessModel.Builder, base: ChildProcessModel<*>) : this(
            rootBuilder,
            base.id,
            base.modelNodes.map { it.visit(XML_BUILDER_VISITOR) },
            base.imports,
            base.exports
        )

        internal constructor(rootBuilder: RootProcessModel.Builder, serialDelegate: SerialDelegate) : this(
            rootBuilder,
            serialDelegate.id?.id,
            serialDelegate.nodes.map { ProcessNodeBase.Builder(it) },
            serialDelegate.imports,
            serialDelegate.exports
        )

        /**
         * When this is overridden and it returns a non-`null` value, it will allow childmodels to be nested in eachother.
         * Note that this does not actually introduce a scope. The nesting is not retained.
         */
        open fun nestedBuilder(): ModelBuilder? = null

    }

    companion object {
        const val ATTR_ID = "id"
        const val ELEMENTLOCALNAME = "childModel"
        @JvmField
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)
    }

}
