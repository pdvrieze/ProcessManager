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

import kotlinx.serialization.*
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlSerializable


expect interface IXmlResultType : XmlSerializable {

  val content: CharArray?

  /**
   * The value of the name property.
   */
  fun getName(): String

  fun setName(value:String)

  /**
   * Gets the value of the path property.
   *
   * @return possible object is [String]
   */
  fun getPath(): String?

  /**
   * Sets the value of the path property.
   *
   * @param namespaceContext
   *
   * @param value allowed object is [String]
   */
  fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

    /**
     * A reader for the underlying body stream.
     */
  val bodyStreamReader: XmlReader

//  fun applyData(payload: Node?): ProcessData

  /**
   * Get the namespace context for evaluating the xpath expression.
   * @return the context
   */
  val originalNSContext: Iterable<Namespace>

  companion object serializer: KSerializer<IXmlResultType>
}

val IXmlResultType.path:String?
  inline get() = getPath()

var IXmlResultType.name:String
  inline get(): String = getName()
  inline set(value) { setName(value) }

fun IXmlResultType.getOriginalNSContext(): Iterable<Namespace> = originalNSContext

object IXmlResultTypeListSerializer: KSerializer<List<IXmlResultType>> {
    val delegate = XmlResultType.list

    override val serialClassDesc: KSerialClassDesc = delegate.serialClassDesc

    override fun load(input: KInput): List<IXmlResultType> {
        return delegate.load(input)
    }

    override fun update(input: KInput, old: List<IXmlResultType>): List<IXmlResultType> {
        @Suppress("UNCHECKED_CAST") // Don't check as it's not needed. Update merely appends
        return delegate.update(input, old as List<XmlResultType>)
    }

    override fun save(output: KOutput, obj: List<IXmlResultType>) {
        delegate.save(output, obj.map(::XmlResultType))
    }
}