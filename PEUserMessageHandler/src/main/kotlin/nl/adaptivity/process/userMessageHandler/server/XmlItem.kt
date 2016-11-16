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

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.StringUtil
import nl.adaptivity.process.userMessageHandler.server.UserTask.TaskItem
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xml.*
import nl.adaptivity.util.xml.*

import javax.xml.namespace.QName

import java.util.ArrayList
import java.util.Collections


class XmlItem : TaskItem, XmlSerializable, SimpleXmlDeserializable {

  inner class Factory : XmlDeserializerFactory<XmlItem> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlItem {
      return XmlItem.deserialize(reader)
    }
  }

  override var name: String? = null // TODO make this non-null
  override var label: String? = null

  override var type: String? = null
  override var value: String? = null
  override var params: String? = null

  private val _options = lazy { mutableListOf<String>() }
  override val options:List<String> get() = _options.value

  override val elementName: QName get() = ELEMENTNAME

  @Throws(XmlException::class)
  override fun deserializeChild(reader: XmlReader): Boolean {
    if (reader.isElement(OPTION_ELEMENTNAME)) {
      _options.value.add(reader.readSimpleElement().toString())
      return true
    }
    return false
  }

  override fun deserializeChildText(elementText: CharSequence): Boolean {
    return false
  }

  override fun deserializeAttribute(attributeNamespace: CharSequence,
                                    attributeLocalName: CharSequence,
                                    attributeValue: CharSequence): Boolean {
    when (attributeLocalName.toString()) {
      "name"   -> {
        name = attributeValue.toString()
        return true
      }
      "label"  -> {
        label = attributeValue.toString()
        return true
      }
      "params" -> {
        params = attributeValue.toString()
        return true
      }
      "type"   -> {
        type = attributeValue.toString()
        return true
      }
      "value"  -> {
        value = attributeValue.toString()
        return true
      }
    }
    return false
  }

  @Throws(XmlException::class)
  override fun onBeforeDeserializeChildren(`in`: XmlReader) {
    // do nothing
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(ELEMENTNAME)
    out.writeAttribute("name", name)
    out.writeAttribute("label", label)
    out.writeAttribute("params", params)
    out.writeAttribute("type", type)
    out.writeAttribute("value", value)

    for (option in options) {
      out.writeSimpleElement(OPTION_ELEMENTNAME, option)
    }

    out.endTag(ELEMENTNAME)
  }

  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    result = prime * result + if (name == null) 0 else name!!.hashCode()
    result = prime * result + if (options.isEmpty()) 0 else options.hashCode()
    result = prime * result + if (type == null) 0 else type!!.hashCode()
    result = prime * result + if (value == null) 0 else value!!.hashCode()
    return result
  }

  override fun equals(obj: Any?): Boolean {
    if (this === obj)
      return true
    if (obj == null)
      return false
    if (javaClass != obj.javaClass)
      return false
    val other = obj as XmlItem?
    if (name == null) {
      if (other!!.name != null)
        return false
    } else if (name != other!!.name)
      return false
    if (options == null || options!!.isEmpty()) {
      if (other.options != null && !options!!.isEmpty())
        return false
    } else if (options != other.options)
      return false
    if (type == null) {
      if (other.type != null)
        return false
    } else if (type != other.type)
      return false
    if (value == null) {
      if (other.value != null)
        return false
    } else if (value != other.value)
      return false
    return true
  }

  companion object {

    val ELEMENTLOCALNAME = "item"
    val ELEMENTNAME = QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh")
    private val OPTION_ELEMENTNAME = QName(Constants.USER_MESSAGE_HANDLER_NS, "option", "umh")

    @Throws(XmlException::class)
    fun deserialize(`in`: XmlReader): XmlItem {
      return XmlItem().deserializeHelper(`in`)
    }

    fun get(source: Sequence<TaskItem>) = source.map { get(it) }

    fun get(source: Collection<TaskItem>) = get(source.asSequence()).toList()

    operator fun get(orig: TaskItem): XmlItem {
      if (orig is XmlItem) {
        return orig
      }

      return XmlItem().apply {
        name = orig.name
        label = orig.label
        type = orig.type
        value = orig.value
        params = orig.params

        if (orig.options.isNotEmpty()) {
          _options.value.addAll(orig.options)
        }
      }
    }
  }
}