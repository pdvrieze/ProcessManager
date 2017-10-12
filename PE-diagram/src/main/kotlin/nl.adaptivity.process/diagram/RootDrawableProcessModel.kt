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
import net.devrieze.util.Handles
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.diagram.*
import nl.adaptivity.process.clientProcessModel.RootClientProcessModel
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessNode.Visitor
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import java.security.Principal
import java.util.*


class RootDrawableProcessModel @JvmOverloads constructor(builder: RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?> = Builder()) : RootClientProcessModel(
  builder, DRAWABLE_NODE_FACTORY), DrawableProcessModel {
  override fun getModelNodes(): List<DrawableProcessNode> {
    return super.getModelNodes()
  }

  class Builder : RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessModel.Builder {

    override var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

    constructor(): this(name=null)

    constructor(nodes: Collection<ProcessNode.IBuilder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
                childModels: Collection<ChildProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
                name: String? = null,
                handle: Long = -1L,
                owner: Principal = SYSTEMPRINCIPAL,
                roles: MutableList<String> = mutableListOf<String>(),
                uuid: UUID? = null,
                imports: MutableList<IXmlResultType> = mutableListOf<IXmlResultType>(),
                exports: MutableList<IXmlDefineType> = mutableListOf<IXmlDefineType>(),
                layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode> = LayoutAlgorithm()) : super(nodes, childModels, name, handle, owner, roles, uuid, imports, exports) {
      this. layoutAlgorithm = layoutAlgorithm
    }

    constructor(base: RootProcessModel<*, *>) : super(base) {
      this.layoutAlgorithm = (base as? DrawableProcessModel)?.layoutAlgorithm ?: LayoutAlgorithm()
    }

    override fun childModelBuilder(): ChildProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?> {
      TODO("DrawableChildModels still need to be implemented")
    }

    override fun childModelBuilder(base: ChildProcessModel<*, *>): ChildProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?> {
      TODO("DrawableChildModels still need to be implemented")
    }

    override fun startNodeBuilder() = DrawableStartNode.Builder()

    override fun startNodeBuilder(startNode: StartNode<*, *>) = DrawableStartNode.Builder(startNode)

    override fun splitBuilder() = DrawableSplit.Builder()

    override fun splitBuilder(split: Split<*, *>) = DrawableSplit.Builder(split)

    override fun joinBuilder() = DrawableJoin.Builder(state = Drawable.STATE_DEFAULT)

    override fun joinBuilder(join: Join<*, *>) = DrawableJoin.Builder(join)

    override fun activityBuilder() = DrawableActivity.Builder()

    override fun activityBuilder(activity: Activity<*, *>) = DrawableActivity.Builder(activity)

    override fun endNodeBuilder() = DrawableEndNode.Builder()

    override fun endNodeBuilder(endNode: EndNode<*, *>) = DrawableEndNode.Builder(endNode)

    override fun build(pedantic: Boolean) = RootDrawableProcessModel(this)
  }

  class Factory : XmlDeserializerFactory<RootDrawableProcessModel> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader) = RootDrawableProcessModel.deserialize(reader)

  }

  override var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode> = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()

  override val rootModel: RootDrawableProcessModel get() = this

  private val mItems = ItemCache()
  private var _bounds: Rectangle = Rectangle()

  override val bounds: Rectangle get() {
    if (_bounds.hasUndefined && getModelNodes().isNotEmpty()) {
      updateBounds()
    }
    return _bounds
  }

  override val leftExtent: Double
    get() = TODO("not implemented")
  override val rightExtent: Double
    get() = TODO("not implemented")
  override val topExtent: Double
    get() = TODO("not implemented")
  override val bottomExtent: Double
    get() = TODO("not implemented")

  override var state = Drawable.STATE_DEFAULT

  private var idSeq = 0
  var isFavourite: Boolean = false
  override var isInvalid: Boolean = false
    private set

  constructor(original: RootProcessModel<*, *>) : this(Builder(original))

  @JvmOverloads
  constructor(uuid: UUID, name: String, nodes: Collection<DrawableProcessNode>, layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>? = null) :
    this(Builder(name=name, uuid=uuid, nodes=nodes.map { it.visit(DRAWABLE_BUILDER_VISITOR) }, layoutAlgorithm = layoutAlgorithm?: LayoutAlgorithm())) {

    with(this.layoutAlgorithm) {
      defaultNodeWidth = Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS),
                                                  Math.max(ACTIVITYWIDTH, JOINWIDTH))
      defaultNodeHeight = Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS),
                                                   Math.max(ACTIVITYHEIGHT, JOINHEIGHT))
      horizSeparation = DEFAULT_HORIZ_SEPARATION
      vertSeparation = DEFAULT_VERT_SEPARATION
    }
    ensureIds()
    layout()
  }

  override fun builder() = Builder(this)

  override fun getRef(): IProcessModelRef<DrawableProcessNode, DrawableProcessModel?, RootDrawableProcessModel> = ProcessModelRef(getName(), this.getHandle(), getUuid())

  override fun getHandle(): Handle<RootDrawableProcessModel> = Handles.handle<RootDrawableProcessModel>(handleValue)

  override fun clone(): RootDrawableProcessModel {
    return RootDrawableProcessModel(this)
  }

  override fun translate(dX: Double, dY: Double) {
    childElements.forEach {
      translate(dX, dY)
    }
  }

  /**
   * Setting the position of the diagram will be the same as translating the  items
   */
  @Deprecated("Use translate as that is the implementation used", ReplaceWith("translage(left, top)"))
  override fun setPos(left: Double, top: Double) {
    translate(left, top)
  }

  @Deprecated("This should already be done by builders")
  internal fun ensureIds() {
    for (node in getModelNodes()) {
      ensureId(node)
    }
  }

  @Deprecated("This is already done by builders")
  private fun <T : DrawableProcessNode> ensureId(node: T): T {
    if (node.id == null) {
      val idBase = node.idBase
      var newId = idBase + idSeq++
      while (getNode(Identifier(newId)) != null) {
        newId = idBase + idSeq++
      }
      node.setId(newId)
    }
    return node
  }

  override fun getItemAt(x: Double, y: Double): Drawable? {
    childElements.asSequence().mapNotNull { it.getItemAt(x,y) }.firstOrNull()?.let { return it }

    return if (isWithinBounds(x,y)) this else null
  }

  override fun setNodes(nodes: Collection<DrawableProcessNode>) {
    // Null check here as setNodes is called during construction of the parent
    _bounds.clear()
    super.setNodes(nodes)
  }

  public override fun setNode(pos: Int, newValue: DrawableProcessNode): DrawableProcessNode {
    return super.setNode(pos, newValue)
  }

  override fun layout() {
    super.layout()
    updateBounds()
    mItems.clearPath(0)
  }

  override fun notifyNodeChanged(node: DrawableProcessNode) {
    invalidateConnectors()
    // TODO this is not correct as it will only expand the bounds.
    val nodeBounds = node.bounds
    if (_bounds.hasUndefined) {
      _bounds = nodeBounds.clone()
      return
    }
    val right = Math.max(nodeBounds.right, _bounds.right)
    val bottom = Math.max(nodeBounds.bottom, _bounds.bottom)
    if (nodeBounds.left < _bounds.left) {
      _bounds.left = nodeBounds.left
    }
    if (nodeBounds.top < _bounds.top) {
      _bounds.top = nodeBounds.top
    }
    _bounds.width = right - _bounds.left
    _bounds.height = bottom - _bounds.top
  }

  private fun updateBounds() {
    val modelNodes = getModelNodes()
    if (modelNodes.isEmpty()) {
      _bounds.set(0.0, 0.0, 0.0, 0.0)
      return
    }
    val firstNode = modelNodes.iterator().next()
    _bounds.set(firstNode.bounds)
    for (node in getModelNodes()) {
      _bounds.extendBounds(node.bounds)
    }
  }

  override fun invalidate() {
    isInvalid = true
    invalidateConnectors()
    _bounds.clear()
  }

  private fun invalidateConnectors() {
    mItems.clearPath(0)
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?) {
    //    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    val childCanvas = canvas.childCanvas(0.0, 0.0, 1.0)
    val strategy = canvas.strategy

    val arcPen = canvas.theme.getPen(ProcessThemeItems.LINE, state)

    val con = mItems.getPathList<S,PEN_T, PATH_T>(strategy, 0) {
      modelNodes.asSequence()
          .filter { it.x.isFinite() && it.y.isFinite() }
          .flatMap { start ->
            start.successors.asSequence().map { getNode(it) }.filterNotNull().filter {
              it.x.isFinite() && it.y.isFinite()
            }.map { end ->
              val x1 = start.bounds.right/*-STROKEWIDTH*/
              val y1 = start.y
              val x2 = end.bounds.left/*+STROKEWIDTH*/
              val y2 = end.y
              Connectors.getArrow(strategy, x1, y1, 0.0, x2, y2, Math.PI, arcPen)
            }
          }.toList()
    }

    for (path in con) {
      childCanvas.drawPath(path, arcPen)
    }

    for (node in getModelNodes()) {
      val b = node.bounds
      node.draw(childCanvas.childCanvas(b.left, b.top, 1.0), null)
    }

    for (node in getModelNodes()) {
      // TODO do something better with the left and top coordinates
      val b = node.bounds
      node.drawLabel(childCanvas.childCanvas(b.left, b.top, 1.0), null, node.x, node.y)
    }
  }

  override val childElements: Collection<Drawable>
    get() = getModelNodes()

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  fun normalize(): DrawableProcessModel? {
    return builder().apply { normalize(false) }.build().asM
  }

  companion object {

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

    private val NULLRECTANGLE = Rectangle(0.0, 0.0, java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): RootDrawableProcessModel {
      return RootProcessModelBase.Builder.deserialize(Builder(), reader).build(false)
    }

    @JvmStatic
    fun get(src: RootProcessModel<*, *>?): RootDrawableProcessModel? {
      if (src is RootDrawableProcessModel) {
        return src
      }
      return if (src == null) null else RootDrawableProcessModel(src)
    }

    @JvmStatic
    fun get(src: ProcessModel<*, *>?): DrawableProcessModel? {
      return when (src) {
        null -> null
        is DrawableProcessModel -> src
        is ChildProcessModel -> TODO("Support child models")
        is RootDrawableProcessModel -> RootDrawableProcessModel(src)
        else -> throw UnsupportedOperationException("Unknown process model subtype")
      }
    }

  }

  init {
    layoutAlgorithm = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()
  }
}

internal object STUB_DRAWABLE_BUILD_HELPER: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?> {
  override val newOwner: DrawableProcessModel?
    get() = null

  override fun childModel(childId: String): ChildProcessModel<DrawableProcessNode, DrawableProcessModel?> {
    TODO("Drawables don't support child models yet")
  }

  override fun node(builder: ProcessNode.IBuilder<*, *>): DrawableProcessNode {
    return DRAWABLE_NODE_FACTORY.invoke(builder, this)
  }

  override fun withOwner(newOwner: DrawableProcessModel?) = this
}

object DRAWABLE_NODE_FACTORY : ProcessModelBase.NodeFactory<DrawableProcessNode, DrawableProcessModel?> {

  private fun visitor(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) = object : ProcessNode.BuilderVisitor<DrawableProcessNode> {
    override fun visitStartNode(startNode: StartNode.Builder<*, *>) = DrawableStartNode(startNode, buildHelper)

    override fun visitActivity(activity: Activity.Builder<*, *>) = DrawableActivity(activity, buildHelper)

    override fun visitActivity(activity: Activity.ChildModelBuilder<*, *>) = TODO("Child models are not implemented yet for drawables")
//        DrawableActivity(activity, childModel!!)

    override fun visitSplit(split: Split.Builder<*, *>) = DrawableSplit(split, buildHelper)

    override fun visitJoin(join: Join.Builder<*, *>) = DrawableJoin(join, buildHelper)

    override fun visitEndNode(endNode: EndNode.Builder<*, *>) = DrawableEndNode(endNode, buildHelper)
  }

  override fun invoke(baseNodeBuilder: ProcessNode.IBuilder<*, *>, buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>): DrawableProcessNode {
    return baseNodeBuilder.visit(visitor(buildHelper))
  }

  override fun invoke(baseChildBuilder: ChildProcessModel.Builder<*, *>,
                      buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>): ChildProcessModelBase<DrawableProcessNode, DrawableProcessModel?> {
    TODO("Child models are not implemented yet for drawables")
//    return DrawableChildModel(baseChildBuilder, ownerModel, pedantic)
  }

}


val DRAWABLE_BUILDER_VISITOR: ProcessNode.Visitor<DrawableProcessNode.Builder> = object : Visitor<DrawableProcessNode.Builder> {
  override fun visitStartNode(startNode: StartNode<*, *>) = DrawableStartNode.Builder(startNode)

  override fun visitActivity(activity: Activity<*, *>) = DrawableActivity.Builder(activity)

  override fun visitSplit(split: Split<*, *>) = DrawableSplit.Builder(split)

  override fun visitJoin(join: Join<*, *>) = DrawableJoin.Builder(join)

  override fun visitEndNode(endNode: EndNode<*, *>) = DrawableEndNode.Builder(endNode)
}


private fun toDrawableNode(elem: ProcessNode<*, *>, buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>): DrawableProcessNode {

  return elem.visit(object : Visitor<DrawableProcessNode> {

    override fun visitStartNode(startNode: StartNode<*, *>): DrawableProcessNode {
      return startNode as? DrawableProcessNode ?: DrawableStartNode.Builder(startNode).build(buildHelper)
    }

    override fun visitActivity(activity: Activity<*, *>): DrawableProcessNode {
      return activity as? DrawableProcessNode ?: DrawableActivity.Builder(activity).build(buildHelper)
    }

    override fun visitSplit(split: Split<*, *>): DrawableProcessNode {
      return split as? DrawableProcessNode ?: DrawableSplit.Builder(split).build(buildHelper)
    }

    override fun visitJoin(join: Join<*, *>): DrawableProcessNode {
      return join as? DrawableProcessNode ?: DrawableJoin.Builder(join).build(buildHelper)
    }

    override fun visitEndNode(endNode: EndNode<*, *>): DrawableProcessNode {
      return endNode as? DrawableProcessNode ?: DrawableEndNode.Builder(endNode).build(buildHelper)
    }

  })
}
