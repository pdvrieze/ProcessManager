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

/**
 * Created by pdvrieze on 04/01/17.
 */
//@Serializable
interface XmlModelCommon: ProcessModel<XmlProcessNode> {

  interface Builder: ProcessModel.Builder {

    override fun startNodeBuilder(): StartNode.Builder = StartNodeBase.Builder()

    override fun startNodeBuilder(startNode: StartNode): StartNode.Builder = StartNodeBase.Builder(startNode)

    override fun splitBuilder(): Split.Builder = SplitBase.Builder()

    override fun splitBuilder(split: Split): Split.Builder = SplitBase.Builder(split)

    override fun joinBuilder(): Join.Builder = JoinBase.Builder()

    override fun joinBuilder(join: Join): Join.Builder = JoinBase.Builder(join)

    override fun activityBuilder(): Activity.Builder = ActivityBase.Builder()

    override fun activityBuilder(activity: Activity): Activity.Builder = ActivityBase.Builder(activity)

    override fun compositeActivityBuilder(): Activity.ChildModelBuilder = XmlActivity.ChildModelBuilder(rootBuilder=this.rootBuilder)

    override fun endNodeBuilder(): EndNode.Builder = EndNodeBase.Builder()

    override fun endNodeBuilder(endNode: EndNode): EndNode.Builder = EndNodeBase.Builder(endNode)

  }

  override val rootModel: RootProcessModel<XmlProcessNode>//XmlProcessModel
}
