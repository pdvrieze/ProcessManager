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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.server

import kotlinx.serialization.Serializable
import nl.adaptivity.process.userMessageHandler.server.UserTask.TaskItem
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import javax.xml.namespace.QName

@Serializable
@XmlSerialName(XmlItem.ELEMENTLOCALNAME, Constants.USER_MESSAGE_HANDLER_NS, Constants.USER_MESSAGE_HANDLER_NS_PREFIX)
class XmlItem : TaskItem {

    override var name: String? = null // TODO make this non-null
    override var label: String? = null

    override var type: String? = null
    override var value: String? = null
    override var params: String? = null

    @XmlSerialName(OPTION_LOCALNAME, Constants.USER_MESSAGE_HANDLER_NS, Constants.USER_MESSAGE_HANDLER_NS_PREFIX)
    override var options: List<String> = listOf<String>()

    companion object {

        const val ELEMENTLOCALNAME = "item"
        val ELEMENTNAME = QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh")
        private const val OPTION_LOCALNAME = "option"

        @Throws(XmlException::class)
        fun deserialize(reader: XmlReader): XmlItem {
            return XML.decodeFromReader(reader)
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

                options = orig.options.toList()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XmlItem

        if (name != other.name) return false
        if (label != other.label) return false
        if (type != other.type) return false
        if (value != other.value) return false
        if (params != other.params) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (params?.hashCode() ?: 0)
        result = 31 * result + options.hashCode()
        return result
    }
}
