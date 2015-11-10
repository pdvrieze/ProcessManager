package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.ProcessNode.Visitor;
import nl.adaptivity.process.util.Identifiable;

import java.util.*;


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
  public static final double DIAGRAMTEXT_SIZE = JOINHEIGHT/2.4d; // 10dp
  public static final double DIAGRAMLABEL_SIZE = DIAGRAMTEXT_SIZE*1.1; // 11dp

  private ItemCache aItems = new ItemCache();
  private Rectangle aBounds = new Rectangle(0, 0, 0, 0);
  private int aState = STATE_DEFAULT;
  private int idSeq=0;

  public DrawableProcessModel(ProcessModel<?> original) {
    this(original, null);
  }

  public DrawableProcessModel(ProcessModel<?> original, LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm) {
    super(original.getUuid(), original.getName(), cloneNodes(original), layoutAlgorithm);
    setDefaultNodeWidth(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYWIDTH, JOINWIDTH)));
    setDefaultNodeHeight(Math.max(Math.max(STARTNODERADIUS, ENDNODEOUTERRADIUS), Math.max(ACTIVITYHEIGHT, JOINHEIGHT)));
    setHorizSeparation(DEFAULT_HORIZ_SEPARATION);
    setVertSeparation(DEFAULT_VERT_SEPARATION);
    ensureIds();
    layout();
  }

  private static Collection<? extends DrawableProcessNode> cloneNodes(ProcessModel<? extends ProcessNode<?>> original) {
    Map<String,DrawableProcessNode> cache = new HashMap<>(original.getModelNodes().size());
    return cloneNodes(original, cache, original.getModelNodes());
  }

  private static Collection<? extends DrawableProcessNode> cloneNodes(ProcessModel<?> source, Map<String, DrawableProcessNode> cache, Collection<? extends Identifiable> nodes) {
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

  public static DrawableProcessModel get(ProcessModel<?> src) {
    if (src instanceof DrawableProcessModel) { return (DrawableProcessModel) src; }
    return src==null ? null : new DrawableProcessModel(src);
  }

  @Override
  public DrawableProcessModel clone() {
	return new DrawableProcessModel(this);
  }

  private static DrawableProcessNode toDrawableNode(ProcessNode<?> elem) {
    return elem.visit(new Visitor<DrawableProcessNode>() {

      @Override
      public DrawableProcessNode visitStartNode(StartNode<?> startNode) {
        return DrawableStartNode.from(startNode);
      }

      @Override
      public DrawableProcessNode visitActivity(Activity<?> activity) {
        return DrawableActivity.from(activity);
      }

      @Override
      public DrawableProcessNode visitSplit(Split<?> split) {
        return DrawableSplit.from(split);
      }

      @Override
      public DrawableProcessNode visitJoin(Join<?> join) {
        return DrawableJoin.from(join);
      }

      @Override
      public DrawableProcessNode visitEndNode(EndNode<?> endNode) {
        return DrawableEndNode.from(endNode);
      }

    });
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
  public void move(double x, double y) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  @Override
  public void setPos(double left, double top) {
    // TODO instead implement this through moving all elements.
    throw new UnsupportedOperationException("Diagrams can not be moved");
  }

  @Override
  public void addNode(DrawableProcessNode node) {
    ensureId(node);
    super.addNode(ensureId(node));
  }

  private void ensureIds() {
    for (DrawableProcessNode node: getModelNodes()) {
      ensureId(node);
    }
  }

  private <T extends DrawableProcessNode> T ensureId(T node) {
    if (node.getId()==null) {
      String newId = "id"+idSeq++;
      while (getNode(newId)!=null) {
        newId = "id"+idSeq++;
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
    return aState ;
  }

  @Override
  public void setState(int state) {
    aState = state;
  }

  @Override
  public void setNodes(Collection<? extends DrawableProcessNode> nodes) {
    // Null check here as setNodes is called during construction of the parent
    if (aBounds!=null) { aBounds.left = Double.NaN; }
    super.setNodes(nodes);
  }

  @Override
  public void layout() {
    super.layout();
    updateBounds();
    aItems.clearPath(0);
  }

  @Override
  public void nodeChanged(DrawableProcessNode node) {
    invalidateConnectors();
    // TODO this is not correct as it will only expand the bounds.
    Rectangle nodeBounds = node.getBounds();
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
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
//    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    Canvas<S, PEN_T, PATH_T> childCanvas = canvas.childCanvas(NULLRECTANGLE, 1d);
    final S strategy = canvas.getStrategy();

    PEN_T arcPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, aState);

    List<PATH_T> connectors  = aItems.getPathList(strategy, 0);
    if (connectors == null) {
      connectors = new ArrayList<>();
      for(DrawableProcessNode start:getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (DrawableProcessNode end: start.getSuccessors()) {
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
      aItems.setPathList(strategy, 0, connectors);
    }

    for(PATH_T path: connectors) {
      childCanvas.drawPath(path, arcPen, null);
    }

    for(DrawableProcessNode node:getModelNodes()) {
      node.draw(childCanvas.childCanvas(node.getBounds(), 1 ), null);
    }
  }

  @Override
  public Collection<? extends Drawable> getChildElements() {
    return getModelNodes();
  }

  static void copyProcessNodeAttrs(ProcessNode<?> from, DrawableProcessNode to) {
    to.setId(from.getId());
    to.setX(from.getX());
    to.setY(from.getY());
  }

}
