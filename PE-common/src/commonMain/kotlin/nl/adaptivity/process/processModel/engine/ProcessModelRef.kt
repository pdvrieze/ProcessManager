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

import net.devrieze.util.Handle
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class ProcessModelRef<NodeT : ProcessNode, out ObjectT : RootProcessModel<NodeT>>
constructor(
    override var name: String?,
    handle: Handle<RootProcessModel<NodeT>>,
    override var uuid: UUID?
) : IProcessModelRef<NodeT, ObjectT> {


    override val handleValue: Long = handle.handleValue

    constructor() : this(null, Handle.invalid(), null)

    constructor(source: IProcessModelRef<NodeT, ObjectT>) : this(source.name, source.handle, source.uuid)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessModelRef<*, *>) return false

        if (name != other.name) return false
        if (handle != other.handle) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + handle.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        return result
    }

    companion object {

        const val ELEMENTLOCALNAME = "processModel"

        @JvmField
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

        @JvmStatic
        fun <NodeT : ProcessNode, ObjectT : RootProcessModel<NodeT>> get(src: IProcessModelRef<NodeT, ObjectT>): ProcessModelRef<NodeT, ObjectT> {
            return src as? ProcessModelRef ?: ProcessModelRef(src)
        }

        @JvmStatic
        fun <NodeT : ProcessNode, ObjectT : RootProcessModel<NodeT>> deserialize(reader: XmlReader): ProcessModelRef<NodeT, ObjectT> {
            return XML.decodeFromReader(reader)
        }

    }

}
