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

import nl.adaptivity.process.processModel.engine.XmlAccessRestriction
import nl.adaptivity.process.util.Identifiable

abstract class MessageActivityBase(
    builder: MessageActivity.Builder,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : ActivityBase(builder, newOwner, otherNodes), MessageActivity {

    private var _message: XmlMessage?

    final override var message: IXmlMessage?
        get() = _message
        private set(value) {
            _message = XmlMessage.from(value)
        }

    init {
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

    open class Builder : BaseBuilder, MessageActivity.RWBuilder {

        final override var message: IXmlMessage?
        final override var accessRestrictions: AccessRestriction?

        constructor(): this(
            null,
        )


        constructor(
            id: String?,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = null,
            results: Collection<IXmlResultType>? = null,
            message: IXmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double= Double.NaN,
            y: Double= Double.NaN,
            isMultiInstance: Boolean= false,
            accessRestrictions: AccessRestriction? = null
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
            isMultiInstance
        ) {
            this.message = message
            this.accessRestrictions = accessRestrictions
        }

//        @Suppress("DEPRECATION")
        constructor(activity: MessageActivity) : this(
            activity.id,
            activity.predecessor,
            activity.successor,
            activity.label,
            activity.defines,
            activity.results,
            XmlMessage.from(activity.message),
            activity.condition,
            activity.name,
            activity.x,
            activity.y,
            activity.isMultiInstance
        )

        constructor(serialDelegate: SerialDelegate) : this(
            id = serialDelegate.id,
            predecessor = serialDelegate.predecessor,
            successor = null,
            label = serialDelegate.label,
            defines = serialDelegate.defines,
            results = serialDelegate.results,
            message = XmlMessage.from(serialDelegate.message),
            condition = serialDelegate.elementCondition,
            name = serialDelegate.name,
            x = serialDelegate.x,
            y = serialDelegate.y,
            isMultiInstance = serialDelegate.isMultiInstance,
            accessRestrictions = serialDelegate.accessRestrictions?.let(::XmlAccessRestriction)
        )
    }
}
