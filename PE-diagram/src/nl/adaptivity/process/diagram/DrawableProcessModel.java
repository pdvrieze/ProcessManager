/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.ProcessNode.Visitor;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class DrawableProcessModel extends ClientProcessModel<DrawableProcessNode, DrawableProcessModel> implements Diagram {

  public static class Factory implements DeserializationFactory<DrawableProcessNode, DrawableProcessModel>, XmlDeserializerFactory<DrawableProcessModel>, SplitFactory<DrawableProcessNode, DrawableProcessModel> {

    @Override
    public DrawableProcessModel deserialize(final XmlReader in) throws XmlException {
      return DrawableProcessModel.deserialize(this, in);
    }

    @Override
    public DrawableEndNode deserializeEndNode(final DrawableProcessModel ownerModel, final XmlReader in) throws
            XmlException {
      return DrawableEndNode.deserialize(ownerModel, in);
    }

    @Override
    public DrawableActivity deserializeActivity(final DrawableProcessModel ownerModel, final XmlReader in) throws
            XmlException {
      return DrawableActivity.deserialize(ownerModel, in);
    }

    @Override
    public DrawableStartNode deserializeStartNode(final DrawableProcessModel ownerModel, final XmlReader in) throws
            XmlException {
      return DrawableStartNode.deserialize(ownerModel, in);
    }

    @Override
    public DrawableJoin deserializeJoin(final DrawableProcessModel ownerModel, final XmlReader in) throws
            XmlException {
      return DrawableJoin.deserialize(ownerModel, in);
    }

    @Override
    public DrawableSplit deserializeSplit(final DrawableProcessModel ownerModel, final XmlReader in) throws
            XmlException {
      return DrawableSplit.deserialize(ownerModel, in);
    }

    @Override
    public DrawableSplit createSplit(final DrawableProcessModel ownerModel, final Collection<? extends Identifiable> successors) {
      DrawableSplit join = new DrawableSplit(ownerModel);
      join.setId(Identifier.findIdentifier(join.getIdBase(), ownerModel.getModelNodes()));
      ownerModel.addNode(join);
      join.setSuccessors(successors);
      return join;
    }
  }

  public static final double STARTNODERADIUS=10d;
  public static final double ENDNODEOUTERRADIUS=12d;
  public static final double ENDNODEINNERRRADIUS=7d;
  public static final double STROKEWIDTH = 1d;
  public static final double ENDNODEOUTERSTROKEWIDTH = 1.7d * STROKEWIDTH;
  public static final double ACTIVITYWIDTH=32d;
  public static final double ACTIVITYHEIGHT=ACTIVITYWIDTH;
  public static final double ACTIVITYROUNDX=ACTIVITYWIDTH/4d;
  public static final double ACTIVITYROUNDY=ACTIVITYHEIGHT/4d;
  public static final double JOINWIDTH=24d;
  public static final double JOINHEIGHT=JOINWIDTH;
  public static final double DEFAULT_HORIZ_SEPARATION = 40d;
  public static final double DEFAULT_VERT_SEPARATION = 30d;
  private static final Rectangle NULLRECTANGLE = new Rectangle(0, 0, Double.MAX_VALUE, Double.MAX_VALUE);
  public static final double DIAGRAMTEXT_SIZE = JOINHEIGHT/2.4d; // 10dp
  public static final double DIAGRAMLABEL_SIZE = DIAGRAMTEXT_SIZE*1.1; // 11dp

  private ItemCache mItems = new ItemCache();
  private Rectangle mBounds = new Rectangle(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
  private int mState = STATE_DEFAULT;
  private int idSeq=0;
  private boolean mFavourite;

  private DrawableProcessModel() {
    super();
  }

  public DrawableProcessModel(ProcessModel<?, ?> original) {
    this(original, null);
  }

  public DrawableProcessModel(ProcessModel<?, ?> original, LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm) {
    super(original.getUuid(), original.getName(), cloneNodes(original), layoutAlgorithm);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    ensureIds();
    layout();
  }

  public boolean isFavourite() {
    return mFavourite;
  }

  public void setFavourite(final boolean favourite) {
    mFavourite = favourite;
  }

  @NotNull
  public static DrawableProcessModel deserialize(@NotNull final XmlReader in) throws XmlException {
    return deserialize(new Factory(), in);
  }

  @NotNull
  public static DrawableProcessModel deserialize(@NotNull Factory factory, @NotNull final XmlReader in) throws XmlException {
    return (DrawableProcessModel) ProcessModelBase.deserialize(factory, new DrawableProcessModel(), in).normalize(factory);
  }

  private static Collection<? extends DrawableProcessNode> cloneNodes(ProcessModel<? extends ProcessNode<?, ?>, ?> original) {
    Map<String,DrawableProcessNode> cache = new HashMap<>(original.getModelNodes().size());
    return cloneNodes(original, cache, original.getModelNodes());
  }

  private static Collection<? extends DrawableProcessNode> cloneNodes(ProcessModel<?, ?> source, Map<String, DrawableProcessNode> cache, Collection<? extends Identifiable> nodes) {
    List<DrawableProcessNode> result = new ArrayList<>(nodes.size());
    for(Identifiable origId: nodes) {
      DrawableProcessNode val = cache.get(origId.getId());
      if (val==null) {
        ProcessNode orig = source.getNode(origId);
        DrawableProcessNode cpy = toDrawableNode(orig);
        result.add(cpy);
        cache.put(cpy.getId(), cpy);
        cpy.setSuccessors(Collections.<DrawableProcessNode>emptyList());
        cpy.setPredecessors(cloneNodes(source, cache, orig.getPredecessors()));
      } else {
        result.add(val);
      }

    }
    return result;
  }

  public DrawableProcessModel(UUID uuid, String name, Collection<? extends DrawableProcessNode> nodes) {
    this(uuid, name, nodes, null);
  }

  public DrawableProcessModel(UUID uuid, String name, Collection<? extends DrawableProcessNode> nodes, LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm) {
    super(uuid, name, nodes, layoutAlgorithm);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    ensureIds();
    layout();
  }

  public static DrawableProcessModel get(ProcessModel<?, ?> src) {
    if (src instanceof DrawableProcessModel) { return (DrawableProcessModel) src; }
    return src==null ? null : new DrawableProcessModel(src);
  }

  @Override
  public DrawableProcessModel clone() {
	return new DrawableProcessModel(this);
  }

  private static DrawableProcessNode toDrawableNode(ProcessNode<?, ?> elem) {
    return elem.visit(new Visitor<DrawableProcessNode>() {

      @Override
      public DrawableProcessNode visitStartNode(StartNode<?, ?> startNode) {
        return DrawableStartNode.from(startNode, true);
      }

      @Override
      public DrawableProcessNode visitActivity(Activity<?, ?> activity) {
        return DrawableActivity.from(activity, true);
      }

      @Override
      public DrawableProcessNode visitSplit(Split<?, ?> split) {
        return DrawableSplit.from(split);
      }

      @Override
      public DrawableProcessNode visitJoin(Join<?, ?> join) {
        return DrawableJoin.from(join, true);
      }

      @Override
      public DrawableProcessNode visitEndNode(EndNode<?, ?> endNode) {
        return DrawableEndNode.from(endNode);
      }

    });
  }

  @Override
  public Rectangle getBounds() {
    if (Double.isNaN(mBounds.left) && getModelNodes().size()>0) {
      updateBounds();
    }
    return mBounds;
  }

  @Override
  public void translate(double dX, double dY) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  @Override
  public void setPos(double left, double top) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  public void ensureIds() {
    for (ClientProcessNode node: getModelNodes()) {
      ensureId(node);
    }
  }

  private <T extends ClientProcessNode> T ensureId(T node) {
    if (node.getId()==null) {
      String idBase = node.getIdBase();
      String newId = idBase + idSeq++;
      while (getNode(newId)!=null) {
        newId = idBase+idSeq++;
      }
      node.setId(newId);
    }
    return node;
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    if (getModelNodes().size()==0) {
      return getBounds().contains(x, y) ? this : null;
    }
    for(Drawable candidate: getChildElements()) {
      Drawable result = candidate.getItemAt(x, y);
      if (result!=null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public int getState() {
    return mState ;
  }

  @Override
  public void setState(int state) {
    mState = state;
  }

  @Override
  public void setNodes(Collection<? extends DrawableProcessNode> nodes) {
    // Null check here as setNodes is called during construction of the parent
    if (mBounds!=null) { mBounds.left = Double.NaN; }
    super.setNodes(nodes);
  }

  @Override
  public void layout() {
    super.layout();
    updateBounds();
    mItems.clearPath(0);
  }

  @Override
  public void notifyNodeChanged(DrawableProcessNode node) {
    invalidateConnectors();
    // TODO this is not correct as it will only expand the bounds.
    Rectangle nodeBounds = node.getBounds();
    if (mBounds==null) {
      mBounds = nodeBounds.clone();
      return;
    }
    double right = Math.max(nodeBounds.right(), mBounds.right());
    double bottom = Math.max(nodeBounds.bottom(), mBounds.bottom());
    if (nodeBounds.left<mBounds.left) {
      mBounds.left = nodeBounds.left;
    }
    if (nodeBounds.top<mBounds.top) {
      mBounds.top = nodeBounds.top;
    }
    mBounds.width = right - mBounds.left;
    mBounds.height = bottom - mBounds.top;
  }

  private void updateBounds() {
    Collection<? extends DrawableProcessNode> modelNodes = getModelNodes();
    if (modelNodes.isEmpty()) { mBounds.set(0,0,0,0); return; }
    DrawableProcessNode firstNode = modelNodes.iterator().next();
    mBounds.set(firstNode.getBounds());
    for(DrawableProcessNode node: getModelNodes()) {
      mBounds.extendBounds(node.getBounds());
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    invalidateConnectors();
    if (mBounds!=null) { mBounds.left=Double.NaN; }
  }

  private void invalidateConnectors() {
    if (mItems!=null) { mItems.clearPath(0); }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
//    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    Canvas<S, PEN_T, PATH_T> childCanvas = canvas.childCanvas(0d, 0d, 1d);
    final S strategy = canvas.getStrategy();

    PEN_T arcPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, mState);

    List<PATH_T> connectors  = mItems.getPathList(strategy, 0);
    if (connectors == null) {
      connectors = new ArrayList<>();
      for(DrawableProcessNode start:getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (Identifiable endId: start.getSuccessors()) {
            DrawableProcessNode end = asNode(endId);
            if (! (Double.isNaN(end.getX())|| Double.isNaN(end.getY()))) {
              double x1 = start.getBounds().right()/*-STROKEWIDTH*/;
              double y1 = start.getY();
              double x2 = end.getBounds().left/*+STROKEWIDTH*/;
              double y2 = end.getY();
              connectors.add(Connectors.getArrow(strategy, x1, y1, x2, y2, arcPen));
            }
          }
        }
      }
      mItems.setPathList(strategy, 0, connectors);
    }

    for(PATH_T path: connectors) {
      childCanvas.drawPath(path, arcPen, null);
    }

    for(DrawableProcessNode node:getModelNodes()) {
      Rectangle b = node.getBounds();
      node.draw(childCanvas.childCanvas(b.left, b.top, 1 ), null);
    }

    for(DrawableProcessNode node:getModelNodes()) {
      // TODO do something better with the left and top coordinates
      Rectangle b = node.getBounds();
      node.drawLabel(childCanvas.childCanvas(b.left, b.top, 1 ), null, node.getX(), node.getY());
    }
  }

  @Override
  public Collection<? extends Drawable> getChildElements() {
    return getModelNodes();
  }

  static void copyProcessNodeAttrs(ProcessNode<?, ?> from, DrawableProcessNode to) {
    to.setId(from.getId());
    to.setX(from.getX());
    to.setY(from.getY());

    Set<? extends Identifiable> predecessors = from.getPredecessors();
    Set<? extends Identifiable> successors = from.getSuccessors();
    if (predecessors != null) { to.setPredecessors(predecessors); }
    if (successors != null) { to.setSuccessors(successors); }
  }

  @Override
  public DrawableProcessNode asNode(final Identifiable id) {
    if (id instanceof DrawableProcessNode) {
      return (DrawableProcessNode) id;
    }
    return getNode(id);
  }
}
