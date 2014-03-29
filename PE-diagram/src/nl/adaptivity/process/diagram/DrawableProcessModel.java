package nl.adaptivity.process.diagram;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableProcessModel extends ClientProcessModel<DrawableProcessNode> implements Diagram {

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

  private ItemCache aItems = new ItemCache();
  private Rectangle aBounds = new Rectangle(0, 0, 0, 0);
  private int aState = STATE_DEFAULT;
  private int idSeq=0;

  public DrawableProcessModel(ProcessModel<?> pOriginal) {
    super(pOriginal.getName(), getDrawableNodes((DrawableProcessModel) null, pOriginal.getStartNodes()));
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    layout();
  }

  public DrawableProcessModel(ProcessModel<?> pOriginal, LayoutAlgorithm<DrawableProcessNode> pLayoutAlgorithm) {
    super(pOriginal.getName(), getDrawableNodes((DrawableProcessModel) null, pOriginal.getStartNodes()), pLayoutAlgorithm);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    layout();
  }

  public DrawableProcessModel(String pName, Collection<? extends DrawableProcessNode> pNodes) {
    super(pName, pNodes);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    layout();
  }

  public DrawableProcessModel(String pName, Collection<? extends DrawableProcessNode> pNodes, LayoutAlgorithm<DrawableProcessNode> pLayoutAlgorithm) {
    super(pName, pNodes, pLayoutAlgorithm);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    layout();
  }

  @Override
  public DrawableProcessModel clone() {
	return new DrawableProcessModel(this);
  }

  private static Collection<? extends DrawableProcessNode> getDrawableNodes(DrawableProcessModel pOwner, Collection<? extends StartNode<?>> pStartNodes) {
    Set<EndNode<?>> origEndNodes = getDrawableNodes(new HashSet<EndNode<?>>(), pStartNodes);
    ArrayList<DrawableProcessNode> result = new ArrayList<>(pStartNodes.size());
    for(EndNode<?> n: origEndNodes) {
      result.add(toDrawableEndNode(pOwner, n));
    }
    return result;
  }

  private static Set<EndNode<?>> getDrawableNodes(Set<EndNode<?>> pSet, Collection<? extends ProcessNode<?>> pNodes) {
    for(ProcessNode<?> node: pNodes) {
      if (node instanceof EndNode<?>) {
        pSet.add((EndNode<?>) node);
      }
      getDrawableNodes(pSet, node.getSuccessors());

    }
    return pSet;
  }

  private static DrawableEndNode toDrawableEndNode(DrawableProcessModel pOwner, EndNode<?> pN) {
    DrawableEndNode result = DrawableEndNode.from(pOwner, pN);
    result.setPredecessors(toDrawableNodes(pOwner, pN.getPredecessors()));
    return result;
  }

  private static Collection<? extends DrawableProcessNode> toDrawableNodes(DrawableProcessModel pOwner, Collection<? extends ProcessNode<?>> pPredecessors) {
    if (pPredecessors.size()==0) { return Collections.emptyList(); }
    if (pPredecessors.size()==1) { return Collections.singleton(toDrawableNode(pOwner, pPredecessors.iterator().next())); }

    List<DrawableProcessNode> result = new ArrayList<>(pPredecessors.size());
    for(ProcessNode<?> elem: pPredecessors) {
      result.add(toDrawableNode(pOwner, elem));
    }
    return result;
  }

  private static DrawableProcessNode toDrawableNode(DrawableProcessModel pOwner, ProcessNode<?> pElem) {
    if (pElem instanceof StartNode) {
      return DrawableStartNode.from(pOwner, (StartNode<?>) pElem);
    } else if (pElem instanceof EndNode) {
      throw new IllegalArgumentException("EndNodes should not see this function");
    } else if (pElem instanceof Join) {
      return DrawableJoin.from(pOwner, (Join<?>) pElem);
    } else if (pElem instanceof Activity) {
      return DrawableActivity.from(pOwner, (Activity<?>) pElem);
    } else {
      throw new UnsupportedOperationException("Unsupported subclass to ProcessNode");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends DrawableStartNode> getStartNodes() {
    return (Collection<? extends DrawableStartNode>) super.getStartNodes();
  }

  @Override
  public Rectangle getBounds() {
    if (Double.isNaN(aBounds.left) && getModelNodes().size()>0) {
      updateBounds();
    }
    return aBounds;
  }

  @Override
  public void move(double pX, double pY) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  @Override
  public void setPos(double pLeft, double pTop) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  @Override
  public void addNode(DrawableProcessNode pNode) {
    if (pNode.getId()==null) {
      String newId = "id"+idSeq;
      while (getNode(newId)!=null) {
        ++idSeq;
      }
      pNode.setId(newId);
    }
    super.addNode(pNode);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    if (getModelNodes().size()==0) {
      return getBounds().contains(pX, pY) ? this : null;
    }
    for(Drawable candidate: getChildElements()) {
      Drawable result = candidate.getItemAt(pX, pY);
      if (result!=null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public int getState() {
    return aState ;
  }

  @Override
  public void setState(int pState) {
    aState = pState;
  }

  @Override
  public void setNodes(Collection<? extends DrawableProcessNode> pNodes) {
    // Null check here as setNodes is called during construction of the parent
    if (aBounds!=null) { aBounds.left = Double.NaN; }
    super.setNodes(pNodes);
  }

  @Override
  public void layout() {
    super.layout();
    updateBounds();
    aItems.clearPath(0);
  }

  @Override
  public void nodeChanged(DrawableProcessNode pNode) {
    invalidateConnectors();
    // TODO this is not correct as it will only expand the bounds.
    Rectangle nodeBounds = pNode.getBounds();
    if (aBounds==null) {
      aBounds = nodeBounds.clone();
      return;
    }
    double right = Math.max(nodeBounds.right(), aBounds.right());
    double bottom = Math.max(nodeBounds.bottom(), aBounds.bottom());
    if (nodeBounds.left<aBounds.left) {
      aBounds.left = nodeBounds.left;
    }
    if (nodeBounds.top<aBounds.top) {
      aBounds.top = nodeBounds.top;
    }
    aBounds.width = right - aBounds.left;
    aBounds.height = bottom - aBounds.top;
  }

  private void updateBounds() {
    Collection<? extends DrawableProcessNode> modelNodes = getModelNodes();
    if (modelNodes.isEmpty()) { aBounds.set(0,0,0,0); return; }
    DrawableProcessNode firstNode = modelNodes.iterator().next();
    aBounds.set(firstNode.getBounds());
    for(DrawableProcessNode node: getModelNodes()) {
      aBounds.extendBounds(node.getBounds());
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    invalidateConnectors();
    if (aBounds!=null) { aBounds.left=Double.NaN; }
  }

  private void invalidateConnectors() {
    if (aItems!=null) { aItems.clearPath(0); }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
//    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    Canvas<S, PEN_T, PATH_T> canvas = pCanvas.childCanvas(NULLRECTANGLE, 1d);
    final S strategy = pCanvas.getStrategy();

    PEN_T arcPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState);

    PATH_T connectors = aItems.getPath(strategy, 0);
    if (connectors == null) {
      connectors = strategy.newPath();
      for(DrawableProcessNode start:getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (DrawableProcessNode end: start.getSuccessors()) {
            if (! (Double.isNaN(end.getX())|| Double.isNaN(end.getY()))) {
              connectors.moveTo(start.getBounds().right()-STROKEWIDTH, start.getY())
                        .lineTo(end.getBounds().left+STROKEWIDTH, end.getY());
            }
          }
        }
      }
      aItems.setPath(strategy, 0, connectors);
    }

    canvas.drawPath(connectors, arcPen, null);

    for(DrawableProcessNode node:getModelNodes()) {
      node.draw(canvas.childCanvas(node.getBounds(), 1 ), null);
    }
  }

  @Override
  public Collection<? extends Drawable> getChildElements() {
    return getModelNodes();
  }

  static void copyProcessNodeAttrs(ProcessNode<?> pFrom, DrawableProcessNode pTo) {
    pTo.setId(pFrom.getId());
    pTo.setX(pFrom.getX());
    pTo.setY(pFrom.getY());
  }

}
