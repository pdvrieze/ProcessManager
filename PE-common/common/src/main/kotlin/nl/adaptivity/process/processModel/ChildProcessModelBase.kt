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

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import kotlinx.serialization.internal.SerialClassDescImpl
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.addField
import nl.adaptivity.util.addFields
import nl.adaptivity.util.multiplatform.JvmField
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.readNullableString
import nl.adaptivity.xml.serialization.writeNullableStringElementValue

/**
 * Base class for submodels
 */
//@Serializable
abstract class ChildProcessModelBase<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> :
    ProcessModelBase<T, M>, ChildProcessModel<T, M> {

    @Suppress("LeakingThis")
    constructor(builder: ChildProcessModel.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<T, M>) : super(builder,
                                                                                                               buildHelper.pedantic) {
        modelNodes = buildNodes(builder, buildHelper.withOwner(asM))
        rootModel = buildHelper.newOwner?.rootModel
            ?: throw IllegalProcessModelException(
            "Childmodels must have roots")
        this.id = builder.childId
    }

    /* Invalid constructor purely for serialization */
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    protected constructor() : super(emptyList(), emptyList()) {
        modelNodes = IdentifyableSet.processNodeSet()
        rootModel = XmlProcessModel.Builder().build() as RootProcessModel<T, M>
        id = null
        if (id == null) {// stupid if to make the compiler not complain about uninitialised values
            throw UnsupportedOperationException("Actually nvoking this constructor is invalid")
        }
    }

    override val modelNodes: IdentifyableSet<T>

    @Transient
    override val rootModel: RootProcessModel<T, M>

    @SerialName("id")
    override val id: String?

    override abstract fun builder(rootBuilder: RootProcessModel.Builder<T, M>): ChildProcessModelBase.Builder<T, M>

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME) {
            writeAttribute(ATTR_ID, id)

            writeChildren(imports)
            writeChildren(exports)
            writeChildren(modelNodes)
        }
    }

    abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessModelBase.Builder<T, M>, ChildProcessModel.Builder<T, M> {


        @Transient
        private lateinit var _rootBuilder: RootProcessModel.Builder<T, M>

        @Transient
        override val rootBuilder: RootProcessModel.Builder<T, M>
            get() = _rootBuilder

        @SerialName("id")
        final override var childId: String?

        protected constructor() {
            childId = null
        }

        constructor(rootBuilder: RootProcessModel.Builder<T, M>,
                    childId: String? = null,
                    nodes: Collection<ProcessNode.IBuilder<T, M>> = emptyList(),
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(nodes, imports, exports) {
            this._rootBuilder = rootBuilder
            this.childId = childId
        }

        @Transient
        override val elementName: QName
            get() = ELEMENTNAME

        /**
         * When this is overridden and it returns a non-`null` value, it will allow childmodels to be nested in eachother.
         * Note that this does not actually introduce a scope. The nesting is not retained.
         */
        open fun nestedBuilder(): ChildProcessModelBase.Builder<T, M>? = null

        abstract override fun buildModel(buildHelper: ProcessModel.BuildHelper<T, M>): ChildProcessModel<T, M>

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (reader.isElement(ProcessConsts.Engine.NAMESPACE, ChildProcessModel.ELEMENTLOCALNAME)) {
                nestedBuilder()?.let { rootBuilder.childModels.add(deserializeHelper(reader)) }
                ?: reader.unhandledEvent("Child models are not currently allowed to be nested")
                return true
            } else {
                return super.deserializeChild(reader)
            }
        }

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            return when (attributeLocalName) {
                ATTR_ID -> {
                    childId = attributeValue; true
                }
                else    -> super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
            }
        }

        abstract class BaseSerializer<T : ChildProcessModelBase.Builder<*, *>> : ProcessModelBase.Builder.BaseSerializer<T>() {
            override fun readElement(result: T, input: KInput, index: Int) {
                when (serialClassDesc.getElementName(index)) {
                    ATTR_ID -> result.childId = input.readNullableString(serialClassDesc, index)
                    else    -> super.readElement(result, input, index)
                }
            }
        }

    }

    abstract class BaseSerializer<T : ChildProcessModelBase<*, *>> : ProcessModelBase.BaseSerializer<T>() {

        private val idIdx by lazy { serialClassDesc.getElementIndexOrThrow(ATTR_ID) }

        override fun writeValues(output: KOutput, obj: T) {
            output.writeNullableStringElementValue(serialClassDesc, idIdx, obj.id)
            super.writeValues(output, obj)
        }
    }

    companion object {
        const val ATTR_ID = "id"
        const val ELEMENTLOCALNAME = "childModel"
        @JvmField
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME,
                                ProcessConsts.Engine.NSPREFIX)


        fun serialClassDesc(name: String): SerialClassDescImpl {
            return SerialClassDescImpl(name).apply {
                addField(ChildProcessModelBase<*, *>::id)
                addFields(ProcessModelBase.serialClassDesc)
            }
        }

    }

}