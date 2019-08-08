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

package nl.adaptivity.process.diagram

import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.handle
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.diagram.Drawable
import nl.adaptivity.diagram.ItemCache
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.process.clientProcessModel.RootClientProcessModel
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessNode.Visitor
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.JvmOverloads
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader
import kotlin.math.max


final class RootDrawableProcessModel @JvmOverloads constructor(
    builder: RootProcessModelBase.Builder = Builder(),
    pedantic: Boolean = DEFAULT_PEDANTIC
                                                        )
    : RootClientProcessModel(builder, DRAWABLE_NODE_FACTORY, pedantic), DrawableProcessModel {

    override val layoutAlgorithm: LayoutAlgorithm = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()

    override val rootModel: RootDrawableProcessModel get() = this

    private var _bounds: Rectangle = Rectangle()

    private var idSeq = 0
    var isFavourite: Boolean = false
    override var isInvalid: Boolean = false
        private set

    constructor(original: RootProcessModel<*>) : this(Builder(original))

    @JvmOverloads
    constructor(uuid: UUID,
                name: String,
                nodes: Collection<DrawableProcessNode>,
                layoutAlgorithm: LayoutAlgorithm? = null) :
        this(Builder(name = name, uuid = uuid, nodes = nodes.map { it.visit(DRAWABLE_BUILDER_VISITOR) },
                     layoutAlgorithm = (layoutAlgorithm ?: LayoutAlgorithm()).apply {
                         defaultNodeWidth = max(max(STARTNODERADIUS, ENDNODEOUTERRADIUS),
                                                max(ACTIVITYWIDTH, JOINWIDTH))
                         defaultNodeHeight = max(max(STARTNODERADIUS, ENDNODEOUTERRADIUS),
                                                 max(ACTIVITYHEIGHT, JOINHEIGHT))
                         horizSeparation = DEFAULT_HORIZ_SEPARATION
                         vertSeparation = DEFAULT_VERT_SEPARATION

                     }))

    override fun copy(imports: Collection<IXmlResultType>,
                      exports: Collection<IXmlDefineType>,
                      nodes: Collection<ProcessNode>,
                      name: String?,
                      uuid: UUID?,
                      roles: Set<String>,
                      owner: Principal,
                      childModels: Collection<ChildProcessModel<DrawableProcessNode>>,
                      handle: Long,
                      layoutAlgorithm: LayoutAlgorithm): RootDrawableProcessModel {
        return Builder(nodes.map { it.builder() }, emptyList(), name, handle, owner, roles, uuid, imports, exports,
                       isFavourite, layoutAlgorithm).also { builder ->
            builder.childModels.replaceBy(childModels.map { child -> child.builder(builder) })
        }.let { RootDrawableProcessModel(it) }
    }

    override fun builder() = Builder(this)

    override fun update(body: (RootProcessModel.Builder) -> Unit): RootDrawableProcessModel {
        return RootDrawableProcessModel(Builder(this).apply(body))
    }

    override val ref: IProcessModelRef<DrawableProcessNode, RootDrawableProcessModel>
        get() = ProcessModelRef(
            name, this.getHandle(), uuid)

    override fun getHandle(): Handle<RootDrawableProcessModel> = handle(handle = handleValue)

    override fun getNode(nodeId: String): DrawableProcessNode? = super<RootClientProcessModel>.getNode(
        Identifier(nodeId))

    /**
     * Normalize the process model. By default this may do nothing.
     * @return The model (this).
     */
    fun normalize(): DrawableProcessModel? {
        return builder().apply { normalize(false) }.let { RootDrawableProcessModel(it) }
    }

    companion object {
        private const val DEFAULT_PEDANTIC = false

        const val STARTNODERADIUS = 10.0
        const val ENDNODEOUTERRADIUS = 12.0
        const val ENDNODEINNERRRADIUS = 7.0
        const val STROKEWIDTH = 1.0
        const val ENDNODEOUTERSTROKEWIDTH = 1.7 * STROKEWIDTH
        const val ACTIVITYWIDTH = 32.0
        const val ACTIVITYHEIGHT = ACTIVITYWIDTH
        const val ACTIVITYROUNDX = ACTIVITYWIDTH / 4.0
        const val ACTIVITYROUNDY = ACTIVITYHEIGHT / 4.0
        const val JOINWIDTH = 24.0
        const val JOINHEIGHT = JOINWIDTH
        const val DEFAULT_HORIZ_SEPARATION = 40.0
        const val DEFAULT_VERT_SEPARATION = 30.0
        const val DIAGRAMTEXT_SIZE = JOINHEIGHT / 2.4 // 10dp
        const val DIAGRAMLABEL_SIZE = DIAGRAMTEXT_SIZE * 1.1 // 11dp

        private val NULLRECTANGLE = Rectangle(0.0, 0.0, Double.MAX_VALUE, Double.MAX_VALUE)

        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader): RootDrawableProcessModel {
            return RootDrawableProcessModel(RootProcessModelBase.Builder.deserialize(Builder(), reader))
        }

        @kotlin.jvm.JvmStatic
        fun get(src: RootProcessModel<*>?): RootDrawableProcessModel? {
            if (src is RootDrawableProcessModel) {
                return src
            }
            return if (src == null) null else RootDrawableProcessModel(src)
        }

        @kotlin.jvm.JvmStatic
        fun get(src: ProcessModel<*>?): DrawableProcessModel? {
            return when (src) {
                null                        -> null
                is DrawableProcessModel     -> src
                is ChildProcessModel        -> TODO("Support child models")
                is RootDrawableProcessModel -> RootDrawableProcessModel(src)
                else                        -> throw UnsupportedOperationException("Unknown process model subtype")
            }
        }

    }

    class Builder : RootProcessModelBase.Builder, DrawableProcessModel.Builder {

        override var topPadding = 5.0
            set(topPadding) {
                val offset = topPadding - this.topPadding
                for (n in childElements) {
                    n.y += offset
                }
                field = topPadding
            }

        override var leftPadding = 5.0
            set(leftPadding) {
                val offset = leftPadding - this.leftPadding
                for (n in childElements) {
                    n.x += offset
                }
                field = leftPadding
            }

        override var bottomPadding = 5.0

        override var rightPadding = 5.0

        override val x: Double get() = 0.0
        override val y: Double get() = 0.0
        override val leftExtent: Double get() = 0.0
        override val topExtent: Double get() = 0.0

        override val rightExtent: Double
            get() = childElements.map { it.x + it.rightExtent }.max() ?: 0.0
        override val bottomExtent: Double
            get() = childElements.map { it.y + it.bottomExtent }.max() ?: 0.0

        override var state = Drawable.STATE_DEFAULT

        override val itemCache = ItemCache()

        override var layoutAlgorithm: LayoutAlgorithm

        var isFavourite: Boolean

        override val rootBuilder: Builder get() = this

        constructor() : this(name = null)

        constructor(nodes: Collection<ProcessNode.Builder> = mutableSetOf(),
                    childModels: Collection<ChildProcessModel.Builder> = emptyList(),
                    name: String? = null,
                    handle: Long = -1L,
                    owner: Principal = SYSTEMPRINCIPAL,
                    roles: Collection<String> = emptyList(),
                    uuid: UUID? = null,
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList(),
                    isFavourite: Boolean = false,
                    layoutAlgorithm: LayoutAlgorithm = LayoutAlgorithm()) : super(nodes, childModels, name, handle,
                                                                                  owner, roles, uuid, imports,
                                                                                  exports) {
            this.layoutAlgorithm = layoutAlgorithm
            this.isFavourite = isFavourite
        }

        constructor(base: RootProcessModel<*>) : super(base) {
            this.layoutAlgorithm = (base as? DrawableProcessModel)?.layoutAlgorithm ?: LayoutAlgorithm()
            this.isFavourite = (base as? RootDrawableProcessModel)?.isFavourite ?: false
        }

        override fun copy(): Builder {
            if (this::class != Builder::class) throw UnsupportedOperationException(
                "Copy must be overridden to be valid")
            return Builder(nodes, childModels, name, handle, owner, roles, uuid, imports, exports, isFavourite,
                           layoutAlgorithm)
        }

        override fun getNode(nodeId: String): DrawableProcessNode.Builder<*>? {
            return nodes.firstOrNull { it.id == nodeId }?.let { it as DrawableProcessNode.Builder<*> }
        }

        override fun childModelBuilder(): ChildProcessModelBase.ModelBuilder {
            TODO("DrawableChildModels still need to be implemented")
        }

        override fun childModelBuilder(base: ChildProcessModel<*>): ChildProcessModelBase.ModelBuilder {
            TODO("DrawableChildModels still need to be implemented")
        }

        override fun startNodeBuilder() = DrawableStartNode.Builder()

        override fun startNodeBuilder(startNode: StartNode) = DrawableStartNode.Builder(startNode)

        override fun splitBuilder() = DrawableSplit.Builder()

        override fun splitBuilder(split: Split) = DrawableSplit.Builder(split)

        override fun joinBuilder() = DrawableJoin.Builder(state = Drawable.STATE_DEFAULT)

        override fun joinBuilder(join: Join) = DrawableJoin.Builder(join)

        override fun activityBuilder() = DrawableActivity.Builder()

        override fun activityBuilder(activity: MessageActivity) = DrawableActivity.Builder(activity)

        override fun endNodeBuilder() = DrawableEndNode.Builder()

        override fun endNodeBuilder(endNode: EndNode) = DrawableEndNode.Builder(endNode)

        override val childElements: List<DrawableProcessNode.Builder<*>> get() = nodes as List<DrawableProcessNode.Builder<*>> // We know they are drawable

        override fun layout(layoutStepper: LayoutStepper<DrawableProcessNode.Builder<*>>) {
            val leftPadding = this.leftPadding
            val topPadding = this.topPadding
            val diagramNodes = toDiagramNodes(nodes)
            if (layoutAlgorithm.layout(diagramNodes, layoutStepper)) {
                var maxX = Double.MIN_VALUE
                var maxY = Double.MIN_VALUE
                for (n in diagramNodes) {
                    n.target.x = n.x + leftPadding
                    n.target.y = n.y + topPadding
                    maxX = max(n.right, maxX)
                    maxY = max(n.bottom, maxY)
                }
            }
        }

/*
        override fun notifyNodeChanged(node: DrawableProcessNode.Builder<*>) {
            invalidateConnectors()
            // TODO this is not correct as it will only expand the bounds.
            val nodeBounds = node.bounds
            if (_bounds.hasUndefined) {
                _bounds = nodeBounds.copy()
                return
            }
            val right = max(nodeBounds.right, _bounds.right)
            val bottom = max(nodeBounds.bottom, _bounds.bottom)
            if (nodeBounds.left < _bounds.left) {
                _bounds.left = nodeBounds.left
            }
            if (nodeBounds.top < _bounds.top) {
                _bounds.top = nodeBounds.top
            }
            _bounds.width = right - _bounds.left
            _bounds.height = bottom - _bounds.top
        }
*/
/*
    override fun notifyNodeChanged(node: DrawableProcessNode.Builder<*>) {
        invalidateConnectors()
        // TODO this is not correct as it will only expand the bounds.
        val nodeBounds = node.bounds
        if (_bounds.hasUndefined) {
            _bounds = nodeBounds.copy()
            return
        }
        val right = max(nodeBounds.right, _bounds.right)
        val bottom = max(nodeBounds.bottom, _bounds.bottom)
        if (nodeBounds.left < _bounds.left) {
            _bounds.left = nodeBounds.left
        }
        if (nodeBounds.top < _bounds.top) {
            _bounds.top = nodeBounds.top
        }
        _bounds.width = right - _bounds.left
        _bounds.height = bottom - _bounds.top
    }
*/

/*
    private fun updateBounds() {
        val modelNodes = modelNodes
        if (modelNodes.isEmpty()) {
            _bounds.set(0.0, 0.0, 0.0, 0.0)
            return
        }
        val firstNode = modelNodes.iterator().next()
        _bounds.set(firstNode.bounds)
        for (node in modelNodes) {
            _bounds.extendBounds(node.bounds)
        }
    }
*/

/*
    override fun invalidate() {
        isInvalid = true
        invalidateConnectors()
        _bounds.clear()
    }
*/


        companion object {
            @kotlin.jvm.JvmStatic
            fun deserialize(reader: XmlReader) = RootProcessModelBase.Builder.deserialize(Builder(), reader)


            private fun toDiagramNodes(modelNodes: Collection<ProcessNode.Builder>): List<DiagramNode<DrawableProcessNode.Builder<*>>> {
                val nodeMap = HashMap<String, DiagramNode<DrawableProcessNode.Builder<*>>>()
                val result = modelNodes.map { node ->
                    DiagramNode(node as DrawableProcessNode.Builder<*>).apply {
                        node.id?.let { nodeMap[it] = this } ?: Unit
                    }
                }

                for (diagramNode in result) {
                    val modelNode = diagramNode.target
                    modelNode.successors.asSequence()
                        .map { nodeMap[it.id] }
                        .filterNotNullTo(diagramNode.rightNodes)

                    modelNode.predecessors.asSequence()
                        .map { nodeMap[it.id] }
                        .filterNotNullTo(diagramNode.leftNodes)
                }
                return result
            }

        }
    }

    class Factory : XmlDeserializerFactory<RootDrawableProcessModel> {

        override fun deserialize(reader: XmlReader) = RootDrawableProcessModel.deserialize(reader)

    }

}

// Casting as we cannot express that it will create the correct child.
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T : DrawableProcessNode> DrawableProcessNode.Builder<T>.build(): T = build(STUB_DRAWABLE_BUILD_HELPER, emptyList()) as T

object STUB_OWNER: DrawableProcessModel {
    override fun builder(): DrawableProcessModel.Builder {
        return RootDrawableProcessModel.Builder()
    }

    override val layoutAlgorithm: LayoutAlgorithm get() = LayoutAlgorithm()

    override val modelNodes: List<DrawableProcessNode> get() = emptyList()

    override val rootModel: RootDrawableProcessModel = RootDrawableProcessModel(RootDrawableProcessModel.Builder())

    override val imports: Collection<IXmlResultType> get() = emptyList()
    override val exports: Collection<IXmlDefineType> get() = emptyList()

    override fun getNode(nodeId: Identifiable): DrawableProcessNode? = null
}

object STUB_DRAWABLE_BUILD_HELPER : ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel, RootDrawableProcessModel, ChildProcessModelBase<DrawableProcessNode>> {
    override val newOwner: DrawableProcessModel
        get() = STUB_OWNER

    override fun childModel(childId: String): ChildProcessModelBase<DrawableProcessNode> {
        TODO("Drawables don't support child models yet")
    }

    override fun childModel(builder: ChildProcessModel.Builder): ChildProcessModelBase<DrawableProcessNode> {
        TODO("Drawables don't support child models yet")
    }

    override fun node(
        builder: ProcessNode.Builder,
        otherNodes: Iterable<ProcessNode.Builder>
                     ): DrawableProcessNode {
        return DRAWABLE_NODE_FACTORY.invoke(builder, this, otherNodes)
    }

    override fun <M : ProcessModel<DrawableProcessNode>> withOwner(newOwner: M): ProcessModel.BuildHelper<DrawableProcessNode, M, RootDrawableProcessModel, ChildProcessModelBase<DrawableProcessNode>> {
        return this as ProcessModel.BuildHelper<DrawableProcessNode, M, RootDrawableProcessModel, ChildProcessModelBase<DrawableProcessNode>>
    }

    override fun condition(condition: Condition) = condition as? XmlCondition ?: XmlCondition(condition.condition)
}

object DRAWABLE_NODE_FACTORY : ProcessModelBase.NodeFactory<DrawableProcessNode, DrawableProcessNode, ChildProcessModelBase<DrawableProcessNode>> {

    private class Visitor(
        val buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, *, *, *>,
        val otherNodes: Iterable<ProcessNode.Builder>
                         ) : ProcessNode.BuilderVisitor<DrawableProcessNode> {
        override fun visitStartNode(startNode: StartNode.Builder) = DrawableStartNode(startNode, buildHelper)

        override fun visitActivity(activity: MessageActivity.Builder) =
            DrawableActivity(activity, buildHelper, otherNodes)

        override fun visitActivity(activity: CompositeActivity.ModelBuilder) = TODO(
            "Child models are not implemented yet for drawables")

        override fun visitActivity(activity: CompositeActivity.ReferenceBuilder) = TODO(
            "Child models are not implemented yet for drawables")
//        DrawableActivity(activity, childModel!!)

        override fun visitSplit(split: Split.Builder) = DrawableSplit(split, buildHelper, otherNodes)

        override fun visitJoin(join: Join.Builder) = DrawableJoin(join, buildHelper, otherNodes)

        override fun visitEndNode(endNode: EndNode.Builder) = DrawableEndNode(endNode, buildHelper.newOwner, otherNodes)

    }

    override fun invoke(
        baseNodeBuilder: ProcessNode.Builder,
        buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
                       ): DrawableProcessNode {
        return baseNodeBuilder.visit(Visitor(buildHelper, otherNodes))
    }

    override fun invoke(baseChildBuilder: ChildProcessModel.Builder,
                        buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, *, *, *>): ChildProcessModelBase<DrawableProcessNode> {
        TODO("Child models are not implemented yet for drawables")
//    return DrawableChildModel(baseChildBuilder, ownerModel, pedantic)
    }

    override fun condition(condition: Condition) = condition as? XmlCondition ?: XmlCondition(condition.condition)
}


val DRAWABLE_BUILDER_VISITOR: ProcessNode.Visitor<DrawableProcessNode.Builder<*>> = object : Visitor<DrawableProcessNode.Builder<*>> {
    override fun visitStartNode(startNode: StartNode) = DrawableStartNode.Builder(startNode)

    override fun visitGenericActivity(activity: Activity): DrawableProcessNode.Builder<*> =
        DrawableActivity.Builder(activity)

    override fun visitSplit(split: Split) = DrawableSplit.Builder(split)

    override fun visitJoin(join: Join) = DrawableJoin.Builder(join)

    override fun visitEndNode(endNode: EndNode) = DrawableEndNode.Builder(endNode)
}


private fun toDrawableNode(elem: ProcessNode,
                           buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, *,*,*>,
                           otherNodes: Iterable<ProcessNode.Builder>): DrawableProcessNode {

    return elem.visit(object : Visitor<DrawableProcessNode> {

        override fun visitStartNode(startNode: StartNode): DrawableProcessNode {
            return startNode as? DrawableProcessNode ?: DrawableStartNode.Builder(startNode).build(buildHelper,otherNodes)
        }

        override fun visitGenericActivity(activity: Activity): DrawableProcessNode {
            return activity as? DrawableProcessNode ?: DrawableActivity.Builder(activity).build(buildHelper,otherNodes)
        }

        override fun visitSplit(split: Split): DrawableProcessNode {
            return split as? DrawableProcessNode ?: DrawableSplit.Builder(split).build(buildHelper,otherNodes)
        }

        override fun visitJoin(join: Join): DrawableProcessNode {
            return join as? DrawableProcessNode ?: DrawableJoin.Builder(join).build(buildHelper,otherNodes)
        }

        override fun visitEndNode(endNode: EndNode): DrawableProcessNode {
            return endNode as? DrawableProcessNode ?: DrawableEndNode.Builder(endNode).build(buildHelper,otherNodes)
        }

    })
}
