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

import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.writeAttribute


/**
 * Created by pdvrieze on 25/11/15.
 */
abstract class JoinSplitBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
    ProcessNodeBase<NodeT, ModelT>, JoinSplit<NodeT, ModelT> {

    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNodeBase.Builder<NodeT, ModelT>, JoinSplit.Builder<NodeT, ModelT>, SimpleXmlDeserializable {

        override var min: Int
        override var max: Int

        constructor(id: String? = null,
                    predecessors: Collection<Identified> = emptyList(),
                    successors: Collection<Identified> = emptyList(),
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    min: Int = -1,
                    max: Int = -1,
                    multiInstance: Boolean = false) : super(id, predecessors, successors, label, defines, results, x, y,
                                                            multiInstance) {
            this.min = min
            this.max = max
        }

        constructor(node: JoinSplit<*, *>) : super(node) {
            min = node.min
            max = node.max
        }

        @Throws(XmlException::class)
        override fun deserializeChild(reader: XmlReader): Boolean {
            return false
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            return false
        }

        override fun toString(): String {
            return "${super.toString().dropLast(1)}, min=$min, max=$max)"
        }

        override fun deserializeAttribute(attributeNamespace: CharSequence,
                                          attributeLocalName: CharSequence,
                                          attributeValue: CharSequence): Boolean {
            when (attributeLocalName.toString()) {
                "min" -> min = attributeValue.toString().toInt()
                "max" -> max = attributeValue.toString().toInt()
                else  -> return super<ProcessNodeBase.Builder>.deserializeAttribute(attributeNamespace,
                                                                                    attributeLocalName, attributeValue)
            }
            return true
        }
    }

    constructor(ownerModel: ModelT,
                predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String?,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) :
        super(ownerModel, predecessors, successors, id, label, x, y, defines, results) {
        this.min = min
        this.max = max
    }


    override var min: Int
    override var max: Int

    @Deprecated("Use the main constructor")
    constructor(ownerModel: ModelT, predecessors: Collection<Identified>, max: Int, min: Int) : this(ownerModel,
                                                                                                     predecessors = predecessors,
                                                                                                     id = null,
                                                                                                     max = max,
                                                                                                     min = min)

    @Deprecated("Use builders")
    constructor(ownerModel: ModelT) : this(ownerModel, id = null) {
    }

    constructor(builder: JoinSplit.Builder<*, *>,
                buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper) {
        this.min = builder.min
        this.max = builder.max
    }

    override abstract fun builder(): Builder<NodeT, ModelT>

    @Deprecated("Don't use")
    open fun deserializeChildText(elementText: CharSequence): Boolean {
        return false
    }

    @Throws(XmlException::class)
    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        if (min >= 0) {
            out.writeAttribute("min", min.toLong())
        }
        if (max >= 0) {
            out.writeAttribute("max", max.toLong())
        }
    }

}
