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
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.diagram.*
import nl.adaptivity.process.clientProcessModel.RootClientProcessModel
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessNode.Visitor
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import java.security.Principal
import java.util.*


class RootDrawableProcessModel : RootClientProcessModel, DrawableProcessModel {

  class Builder : RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessModel.Builder {

    override var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

    constructor(nodes: MutableSet<ProcessNode.Builder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
                childModels: MutableSet<ChildProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
                name: String? = null,
                handle: Long = -1L,
                owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
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

    override fun startNodeBuilder(): DrawableStartNode.Builder {
      return DrawableStartNode.Builder()
    }

    override fun startNodeBuilder(startNode: StartNode<*, *>): DrawableStartNode.Builder {
      return DrawableStartNode.Builder(startNode)
    }

    override fun splitBuilder(): DrawableSplit.Builder {
      return DrawableSplit.Builder()
    }

    override fun splitBuilder(split: Split<*, *>): DrawableSplit.Builder {
      return DrawableSplit.Builder(split)
    }

    override fun joinBuilder(): DrawableJoin.Builder {
      return DrawableJoin.Builder(state = Drawable.STATE_DEFAULT)
    }

    override fun joinBuilder(join: Join<*, *>): DrawableJoin.Builder {
      return DrawableJoin.Builder(join)
    }

    override fun activityBuilder(): DrawableActivity.Builder {
      return DrawableActivity.Builder()
    }

    override fun activityBuilder(activity: Activity<*, *>): DrawableActivity.Builder {
      return DrawableActivity.Builder(activity)
    }

    override fun endNodeBuilder(): DrawableEndNode.Builder {
      return DrawableEndNode.Builder()
    }

    override fun endNodeBuilder(endNode: EndNode<*, *>): DrawableEndNode.Builder {
      return DrawableEndNode.Builder(endNode)
    }

    override fun build(pedantic: Boolean): RootDrawableProcessModel {
      return RootDrawableProcessModel(this)
    }
  }

  class Factory : XmlDeserializerFactory<RootDrawableProcessModel> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): RootDrawableProcessModel {
      return RootDrawableProcessModel.deserialize(reader)
    }

  }

  override var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

  override val rootModel: RootDrawableProcessModel get() = this

  private val mItems = ItemCache()
  private var mBounds: Rectangle = Rectangle(java.lang.Double.NaN, java.lang.Double.NaN, java.lang.Double.NaN, java.lang.Double.NaN)
  private var mState = Drawable.STATE_DEFAULT
  private var mIdSeq = 0
  var isFavourite: Boolean = false
  override var isInvalid: Boolean = false
    private set

  private constructor() : this(Builder()) {}

  @JvmOverloads
  constructor(original: RootProcessModel<*, *>) : this(Builder(original))

  @JvmOverloads
  constructor(builder: RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>) : super(builder, DRAWABLE_NODE_FACTORY) {
    layoutAlgorithm = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()
  }

  @JvmOverloads constructor(uuid: UUID, name: String, nodes: Collection<DrawableProcessNode>, layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>? = null) :
    super(uuid, name, nodes, DRAWABLE_NODE_FACTORY) {
    this.layoutAlgorithm = layoutAlgorithm?: LayoutAlgorithm()

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

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun getRef(): IProcessModelRef<DrawableProcessNode, DrawableProcessModel?, RootDrawableProcessModel> {
    return ProcessModelRef(getName(), this.getHandle(), getUuid())
  }

  override fun getHandle(): Handle<RootDrawableProcessModel> {
    return Handles.handle<RootDrawableProcessModel>(handleValue)
  }

  override fun clone(): RootDrawableProcessModel {
    return RootDrawableProcessModel(this)
  }

  override val bounds: Rectangle get() {
    if (java.lang.Double.isNaN(mBounds.left) && getModelNodes().isNotEmpty()) {
      updateBounds()
    }
    return mBounds
  }

  override fun translate(dX: Double, dY: Double) {
    // TODO instead implement this through moving all elements.
    throw UnsupportedOperationException("Diagrams can not be moved")
  }

  override fun setPos(left: Double, top: Double) {
    // TODO instead implement this through moving all elements.
    throw UnsupportedOperationException("Diagrams can not be moved")
  }

  fun ensureIds() {
    for (node in getModelNodes()) {
      ensureId(node)
    }
  }

  @Deprecated("This is already done by builders")
  private fun <T : DrawableProcessNode> ensureId(node: T): T {
    if (node.id == null) {
      val idBase = node.idBase
      var newId = idBase + mIdSeq++
      while (getNode(Identifier(newId)) != null) {
        newId = idBase + mIdSeq++
      }
      node.setId(newId)
    }
    return node
  }

  override fun getItemAt(x: Double, y: Double): Drawable? {
    childElements.asSequence().mapNotNull { it.getItemAt(x,y) }.firstOrNull()?.let { return it }

    return if (isWithinBounds(x,y)) this else null
  }

  override fun getState(): Int {
    return mState
  }

  override fun setState(state: Int) {
    mState = state
  }

  override fun setNodes(nodes: Collection<DrawableProcessNode>) {
    // Null check here as setNodes is called during construction of the parent
    if (mBounds != null) {
      mBounds!!.left = java.lang.Double.NaN
    }
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
    if (mBounds == null) {
      mBounds = nodeBounds.clone()
      return
    }
    val right = Math.max(nodeBounds.right(), mBounds!!.right())
    val bottom = Math.max(nodeBounds.bottom(), mBounds!!.bottom())
    if (nodeBounds.left < mBounds!!.left) {
      mBounds!!.left = nodeBounds.left
    }
    if (nodeBounds.top < mBounds!!.top) {
      mBounds!!.top = nodeBounds.top
    }
    mBounds!!.width = right - mBounds!!.left
    mBounds!!.height = bottom - mBounds!!.top
  }

  private fun updateBounds() {
    val modelNodes = getModelNodes()
    if (modelNodes.isEmpty()) {
      mBounds!!.set(0.0, 0.0, 0.0, 0.0)
      return
    }
    val firstNode = modelNodes.iterator().next()
    mBounds!!.set(firstNode.bounds)
    for (node in getModelNodes()) {
      mBounds!!.extendBounds(node.bounds)
    }
  }

  override fun invalidate() {
    isInvalid = true
    invalidateConnectors()
    if (mBounds != null) {
      mBounds!!.left = java.lang.Double.NaN
    }
  }

  private fun invalidateConnectors() {
    mItems?.clearPath(0)
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
    //    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    val childCanvas = canvas.childCanvas(0.0, 0.0, 1.0)
    val strategy = canvas.strategy

    val arcPen = canvas.theme.getPen(ProcessThemeItems.LINE, mState)

    val con = mItems.getPathList<S,PEN_T, PATH_T>(strategy, 0) {
      modelNodes.asSequence()
          .filter { it.x.isFinite() && it.y.isFinite() }
          .flatMap { start ->
            start.successors.asSequence().map { getNode(it) }.filterNotNull().filter {
              it.x.isFinite() && it.y.isFinite()
            }.map { end ->
              val x1 = start.bounds.right()/*-STROKEWIDTH*/
              val y1 = start.y
              val x2 = end.bounds.left/*+STROKEWIDTH*/
              val y2 = end.y
              Connectors.getArrow(strategy, x1, y1, 0.0, x2, y2, Math.PI, arcPen)
            }
          }.toList()
    }

    for (path in con) {
      childCanvas.drawPath(path, arcPen, null)
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

  override fun getChildElements(): Collection<Drawable> {
    return getModelNodes()
  }

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

    private fun cloneNodes(original: RootProcessModel<out ProcessNode<*, *>, *>): Collection<DrawableProcessNode> {
      val cache = HashMap<String, DrawableProcessNode>(original.getModelNodes().size)
      return cloneNodes(original, cache, original.getModelNodes())
    }

    private fun cloneNodes(source: RootProcessModel<*, *>, cache: MutableMap<String, DrawableProcessNode>, nodes: Collection<Identifiable>): Collection<DrawableProcessNode> {
      val result = ArrayList<DrawableProcessNode>(nodes.size)
      for (origId in nodes) {
        val value = cache[origId.id]
        if (value == null) {
          source.getNode(origId)?.let { orig->
            val cpy = toDrawableNode(orig)
            result.add(cpy)
            cpy.id?.let{ cache[it] = cpy }
            cpy.setSuccessors(emptyList<Identified>())
            cpy.setPredecessors(cloneNodes(source, cache, orig.predecessors))
          }
        } else {
          result.add(value)
        }

      }
      return result
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

    @JvmStatic
    @JvmName("copyProcessNodeAttrs")
    @Deprecated("Use builders for this instead")
    internal fun copyProcessNodeAttrs(from: ProcessNode<*, *>, to: DrawableProcessNode) {
      from.id?.let { to.setId(it) }
      to.setX(from.x)
      to.setY(from.y)

      val predecessors = from.predecessors
      val successors = from.successors
      to.setPredecessors(predecessors)
      to.setSuccessors(successors)
    }
  }
}

object DRAWABLE_NODE_FACTORY : ProcessModelBase.NodeFactory<DrawableProcessNode, DrawableProcessModel?> {

  private fun visitor(newOwner: DrawableProcessModel?, childModel: ChildProcessModel<DrawableProcessNode, DrawableProcessModel?>?=null) = object : ProcessNode.BuilderVisitor<DrawableProcessNode> {
    override fun visitStartNode(startNode: StartNode.Builder<*, *>) = DrawableStartNode(startNode, newOwner)

    override fun visitActivity(activity: Activity.Builder<*, *>) = DrawableActivity(activity, newOwner)

    override fun visitActivity(activity: Activity.ChildModelBuilder<*, *>) = TODO("Child models are not implemented yet for drawables")
//        DrawableActivity(activity, childModel!!)

    override fun visitSplit(split: Split.Builder<*, *>) = DrawableSplit(split, newOwner)

    override fun visitJoin(join: Join.Builder<*, *>) = DrawableJoin(join, newOwner)

    override fun visitEndNode(endNode: EndNode.Builder<*, *>) = DrawableEndNode(endNode, newOwner)
  }

  override fun invoke(newOwner: ProcessModel<DrawableProcessNode, DrawableProcessModel?>, baseNode: ProcessNode<*, *>): DrawableProcessNode {
    if (baseNode is DrawableProcessNode && baseNode.ownerModel== newOwner) return baseNode
    return toDrawableNode(newOwner.asM, baseNode)
  }

  override fun invoke(newOwner: ProcessModel<DrawableProcessNode, DrawableProcessModel?>, baseNodeBuilder: ProcessNode.Builder<*, *>): DrawableProcessNode {
    return baseNodeBuilder.visit(visitor(newOwner.asM))
  }

  override fun invoke(newOwner: ProcessModel<DrawableProcessNode, DrawableProcessModel?>, baseNodeBuilder: Activity.ChildModelBuilder<*, *>, childModel: ChildProcessModel<DrawableProcessNode, DrawableProcessModel?>): DrawableActivity {
    TODO("Child models are not implemented yet for drawables")
//    return baseNodeBuilder.visit(visitor(newOwner.asM, childModel as DrawableChildModel)) as DrawableActivity
  }

  override fun invoke(ownerModel: RootProcessModel<DrawableProcessNode, DrawableProcessModel?>,
                      baseChildBuilder: ChildProcessModel.Builder<*, *>,
                      childModelProvider: RootProcessModelBase.ChildModelProvider<DrawableProcessNode, DrawableProcessModel?>,
                      pedantic: Boolean): ChildProcessModelBase<DrawableProcessNode, DrawableProcessModel?> {
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


private inline fun toDrawableNode(newOwner: DrawableProcessModel?, node: ProcessNode<*, *>): DrawableProcessNode {
  return node.visit(DRAWABLE_BUILDER_VISITOR).build(newOwner)
}



private fun toDrawableNode(elem: ProcessNode<*, *>): DrawableProcessNode {
  return elem.visit(object : Visitor<DrawableProcessNode> {

    override fun visitStartNode(startNode: StartNode<*, *>): DrawableProcessNode {
      return DrawableStartNode.from(startNode, true)
    }

    override fun visitActivity(activity: Activity<*, *>): DrawableProcessNode {
      return DrawableActivity.from(activity, true)
    }

    override fun visitSplit(split: Split<*, *>): DrawableProcessNode {
      return DrawableSplit.from(split)
    }

    override fun visitJoin(join: Join<*, *>): DrawableProcessNode {
      return DrawableJoin.from(join, true)
    }

    override fun visitEndNode(endNode: EndNode<*, *>): DrawableProcessNode {
      return DrawableEndNode.from(endNode)
    }

  })
}
