/*
 * Copyright (c) 2017.
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

import net.devrieze.util.CollectionUtil
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.processModel.*
import org.w3c.dom.Node
import java.util.*

/**
 * Shared interface for both root and child models that are executable.
 */
interface ExecutableModelCommon : ProcessModel<ExecutableProcessNode, ExecutableModelCommon> {

    override val rootModel: ExecutableProcessModel
    /**
     * Get the startnodes for this model.

     * @return The start nodes.
     */
    val startNodes: Collection<ExecutableStartNode>
        get() = Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(ArrayList<ExecutableStartNode>(),
                                                                                 modelNodes,
                                                                                 ExecutableStartNode::class.java))
    val endNodeCount: Int

    fun toInputs(payload: Node?): List<ProcessData> {
        // TODO make this work properly

        return imports.map { XmlResultType(it).applyData(payload) }
    }

    fun getNode(nodeId: String): ExecutableProcessNode?

    interface Builder : ProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon> {

        override val rootBuilder: ExecutableProcessModel.Builder

        override val defaultPedantic get() = true

        override fun startNodeBuilder() = ExecutableStartNode.Builder()

        override fun startNodeBuilder(startNode: StartNode<*, *>) = ExecutableStartNode.Builder(startNode)

        override fun splitBuilder() = ExecutableSplit.Builder()

        override fun splitBuilder(split: Split<*, *>) = ExecutableSplit.Builder(split)

        override fun joinBuilder() = ExecutableJoin.Builder()

        override fun joinBuilder(join: Join<*, *>) = ExecutableJoin.Builder(join)

        override fun activityBuilder() = ExecutableActivity.Builder()

        override fun activityBuilder(activity: Activity<*, *>) = ExecutableActivity.Builder(activity)

        override fun compositeActivityBuilder() = ExecutableActivity.ChildModelBuilder(this.rootBuilder)

        override fun endNodeBuilder() = ExecutableEndNode.Builder()

        override fun endNodeBuilder(endNode: EndNode<*, *>) = ExecutableEndNode.Builder(endNode)

    }

}