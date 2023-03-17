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

    override var message: IXmlMessage?
        get() = field
        private set(value) {
            field = XmlMessage.from(value)
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
        message = builder.message
        accessRestrictions = builder.accessRestrictions
    }

    constructor(
        builder: CompositeActivity.ModelBuilder,
        buildHelper: BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder.ensureExportable(), buildHelper.newOwner, otherNodes) {
        childModel = buildHelper.childModel(builder)
        childId = builder.childId
        message = null
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
        message = null
        accessRestrictions = null
    }

    override fun builder(): Activity.Builder = when {
        childId == null -> MessageActivityBase.Builder(this)
        else            -> ReferenceActivityBuilder(this)
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = when {
        childModel == null -> visitor.visitActivity(messageActivity = this)
        else               -> visitor.visitCompositeActivity(compositeActivity = this)
    }

}

