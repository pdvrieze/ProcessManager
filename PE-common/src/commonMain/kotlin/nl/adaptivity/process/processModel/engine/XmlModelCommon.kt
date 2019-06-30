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

    override val rootBuilder: XmlProcessModel.Builder

    override fun startNodeBuilder(): XmlStartNode.Builder = XmlStartNode.Builder()

    override fun startNodeBuilder(startNode: StartNode): XmlStartNode.Builder = XmlStartNode.Builder(startNode)

    override fun splitBuilder(): XmlSplit.Builder = XmlSplit.Builder()

    override fun splitBuilder(split: Split): XmlSplit.Builder = XmlSplit.Builder(split)

    override fun joinBuilder(): XmlJoin.Builder = XmlJoin.Builder()

    override fun joinBuilder(join: Join): XmlJoin.Builder = XmlJoin.Builder(join)

    override fun activityBuilder(): ActivityBase.Builder = XmlActivity.Builder()

    override fun activityBuilder(activity: Activity): ActivityBase.Builder = XmlActivity.Builder(activity)

    override fun compositeActivityBuilder(): XmlActivity.ChildModelBuilder = XmlActivity.ChildModelBuilder(rootBuilder=this.rootBuilder)

    override fun endNodeBuilder(): XmlEndNode.Builder = XmlEndNode.Builder()

    override fun endNodeBuilder(endNode: EndNode): XmlEndNode.Builder = XmlEndNode.Builder(endNode)

  }

  override val rootModel: RootProcessModel<XmlProcessNode>//XmlProcessModel
}
