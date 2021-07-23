/*
 * Copyright (c) 2019.
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

import foo.FakeSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.process.util.Identifiable

@FakeSerializable
abstract class MessageActivityBase : ActivityBase, MessageActivity {

    @SerialName("message")
    @FakeSerializable(with = IXmlMessage.Companion::class)
    private var _message: XmlMessage?

    @Transient
    override final var message: IXmlMessage?
        get() = _message
        private set(value) {
            _message = XmlMessage.from(value)
        }

    constructor(
        builder: MessageActivity.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
               ) :
        super(builder, newOwner, otherNodes) {
        _message = XmlMessage.from(builder.message)
    }


    override fun builder(): MessageActivity.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = visitor.visitActivity(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as MessageActivityBase

        if (_message != other._message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (_message?.hashCode() ?: 0)
        return result
    }

    open class Builder : BaseBuilder, MessageActivity.Builder {

        @FakeSerializable(with = IXmlMessage.Companion::class)
        final override var message: IXmlMessage?

        constructor(activity: MessageActivity) : super(activity) {
            message = activity.message
        }

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            message: XmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
                   ) : super(
            id,
            predecessor,
            successor,
            label,
            defines,
            results,
            condition,
            name,
            x,
            y,
            multiInstance
                            ) {
            this.message = message
        }
    }
}
