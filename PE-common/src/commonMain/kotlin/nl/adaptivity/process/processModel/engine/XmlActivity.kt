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

import kotlinx.serialization.*
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.addField
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlSerialName


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
@Serializable(XmlActivity.Companion::class)
@XmlSerialName(Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlActivity : ActivityBase, XmlProcessNode {

    constructor(builder: Activity.Builder,
                buildHelper: BuildHelper<*, *, *, *>) : super(builder, buildHelper)

    constructor(builder: Activity.CompositeActivityBuilder,
                buildHelper: BuildHelper<*, *, *, *>) : super(builder, buildHelper)

    @Transient
    private var xmlCondition: XmlCondition? = null

    @Throws(XmlException::class)
    override fun serializeCondition(out: XmlWriter) {
        out.writeChild(xmlCondition)
    }

    override val condition: String?
        get() = xmlCondition?.toString()

    @Serializer(forClass = XmlActivity::class)
    companion object:KSerializer<XmlActivity> {

        override fun deserialize(decoder: Decoder): XmlActivity {
            throw UnsupportedOperationException("This can only done in the correct context")
        }
    }

}

