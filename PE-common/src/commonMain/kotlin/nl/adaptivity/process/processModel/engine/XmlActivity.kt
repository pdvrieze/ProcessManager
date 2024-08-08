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

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of
 *
 * @author Paul de Vrieze
 */
class XmlActivity : ActivityBase, XmlProcessNode, CompositeActivity, MessageActivity {

    private var _message: XmlMessage?

    final override var message: IXmlMessage?
        get() = _message
        private set(value) {
            _message = XmlMessage.from(value)
        }

    override val childModel: ChildProcessModel<ProcessNode>?

    val childId: String?

    private var xmlCondition: XmlCondition? = null

    override val condition: Condition?
        get() = xmlCondition

    override val accessRestrictions: AccessRestriction?

    constructor(
        builder: MessageActivity.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder.ensureExportable(), newOwner, otherNodes) {
        childModel = null
        childId = null
        _message = XmlMessage.from(builder.message)
        accessRestrictions = builder.accessRestrictions
    }

    constructor(
        builder: Builder,
        buildHelper: BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder.base.ensureExportable(), buildHelper.newOwner, otherNodes) {
        val base = builder.base
        val m: IXmlMessage?
        when (base) {
            is ReferenceActivityBuilder -> {
                val id = base.childId
                childModel = id?.let { buildHelper.childModel(it) }
                childId = id
                _message = null
                accessRestrictions = null
            }


            else -> {
                childModel = null
                childId = null
                _message = XmlMessage.from(builder.message)
                accessRestrictions = builder.accessRestrictions
            }
        }
    }

    constructor(
        builder: CompositeActivity.ModelBuilder,
        buildHelper: BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder.ensureExportable(), buildHelper.newOwner, otherNodes) {
        childModel = buildHelper.childModel(builder)
        childId = builder.childId
        _message = null
        accessRestrictions = null
    }

    constructor(
        builder: CompositeActivity.ReferenceBuilder,
        buildHelper: BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder.ensureExportable(), buildHelper.newOwner, otherNodes) {
        val id = builder.childId
        childModel = id?.let { buildHelper.childModel(it) }
        childId = id
        _message = null
        accessRestrictions = null
    }

    override fun builder(): Builder = when {
        childId == null -> Builder(MessageActivityBase.Builder(this))
        else -> Builder(ReferenceActivityBuilder(this))
    }


    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = when {
        childModel == null -> visitor.visitActivity(messageActivity = this)
        else -> visitor.visitCompositeActivity(compositeActivity = this)
    }

    /**
     * Wrapper builder needed because XmlActivity wraps both composite and message
     */
    class Builder private constructor(val base: Activity.Builder) : CompositeActivity.Builder, MessageActivity.Builder,
        Activity.Builder by base {
        constructor(base: MessageActivity.Builder) : this(base as Activity.Builder)
        constructor(base: ReferenceActivityBuilder) : this(base as Activity.Builder)

        override var message: IXmlMessage?
            get() = (base as? MessageActivity.Builder)?.message
            set(value) {
                (base as? MessageActivity.Builder)?.run { message = value }
            }

        override val accessRestrictions: AccessRestriction?
            get() = (base as? MessageActivity.Builder)?.accessRestrictions

    }

}

