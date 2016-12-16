/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel

import net.devrieze.util.StringUtil
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.writeAttribute
import java.util.*


/**
 * Created by pdvrieze on 25/11/15.
 */
abstract class JoinSplitBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> :
    ProcessNodeBase<T, M>, JoinSplit<T, M> {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : ProcessNodeBase.Builder<T,M>, JoinSplit.Builder<T,M>, SimpleXmlDeserializable {

    override var min:Int
    override var max:Int

    constructor(predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String? = null, label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) : super(predecessors, successors, id, label, x, y, defines, results) {
      this.min = min
      this.max = max
    }

    constructor(node: JoinSplit<*, *>) : super(node) {
      min = node.min
      max = node.max
    }

    override abstract fun build(newOwner: M?): ProcessNode<T, M>

    @Throws(XmlException::class)
    override fun deserializeChild(`in`: XmlReader): Boolean {
      return false
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
      return false
    }

    override fun toString(): String {
      return "${super.toString().dropLast(1)}, min=$min, max=$max)"
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      if (StringUtil.isEqual("min", attributeLocalName)) {
        min = Integer.parseInt(attributeValue.toString())
      } else if (StringUtil.isEqual("max", attributeLocalName)) {
        max = Integer.parseInt(attributeValue.toString())
      } else {
        return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
      }
      return true
    }
  }

  constructor(ownerModel: M?,
              predecessors: Collection<Identified> = emptyList(),
              successors: Collection<Identified> = emptyList(),
              id: String?,
              label: String? = null,
              x: Double = java.lang.Double.NaN,
              y: Double = java.lang.Double.NaN,
              defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
              results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
              min: Int = -1,
              max: Int = -1) :
      super(ownerModel, predecessors, successors, id, label, x, y, defines, results) {
    this.min = min
    this.max = max
  }



  override var min: Int
  override var max: Int

  @Deprecated("Use the main constructor")
  constructor(ownerModel: M?, predecessors: Collection<Identified>, max: Int, min: Int) : this(ownerModel, predecessors=predecessors, id=null, max=max, min=min)

  @Deprecated("")
  constructor(ownerModel: M?) : this(ownerModel, id=null) { }

  constructor(orig: JoinSplit<*, *>, newOwner: M?) : super(orig, newOwner) {
    min = orig.min
    max = orig.max
  }

  constructor(builder: JoinSplit.Builder<*, *>, newOwnerModel: M?) : super(builder, newOwnerModel) {
    this.min = builder.min
    this.max = builder.max
  }

  override abstract fun builder(): Builder<T, M>

  @Deprecated("Don't use")
  @Throws(XmlException::class)
  open fun deserializeChild(`in`: XmlReader): Boolean {
    return false
  }

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
