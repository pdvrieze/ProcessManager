package nl.adaptivity.process.diagram;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableProcessModel extends ClientProcessModel<DrawableProcessNode> implements Diagram {

  public static final double STARTNODERADIUS=8d;
  public static final double ENDNODEOUTERRADIUS=12d;
  public static final double ENDNODEINNERRRADIUS=6d;
  public static final double ACTIVITYWIDTH=32d;
  public static final double ACTIVITYHEIGHT=ACTIVITYWIDTH;
  public static final double ACTIVITYROUNDX=ACTIVITYWIDTH/4d;
  public static final double ACTIVITYROUNDY=ACTIVITYHEIGHT/4d;
  public static final double JOINWIDTH=24d;
  public static final double JOINHEIGHT=JOINWIDTH;

  private double aScale = 1d;

  public DrawableProcessModel(ProcessModel<?> pOriginal) {
    super(pOriginal.getName(), getDrawableNodes(pOriginal.getStartNodes()));
    layout();
  }

  public DrawableProcessModel(String pName, Collection<? extends DrawableProcessNode> pNodes) {
    super(pName, pNodes);
    layout();
  }

  private static Collection<? extends DrawableProcessNode> getDrawableNodes(Collection<? extends StartNode<?>> pStartNodes) {
    Set<EndNode<?>> origEndNodes = getDrawableNodes(new HashSet<EndNode<?>>(), pStartNodes);
    ArrayList<DrawableProcessNode> result = new ArrayList<DrawableProcessNode>(pStartNodes.size());
    for(EndNode<?> n: origEndNodes) {
      result.add(toDrawableEndNode(n));
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

  private static DrawableEndNode toDrawableEndNode(EndNode<?> pN) {
    DrawableEndNode result = DrawableEndNode.from(pN);
    result.setPredecessors(toDrawableNodes(pN.getPredecessors()));
    return result;
  }

  private static Collection<? extends DrawableProcessNode> toDrawableNodes(Collection<? extends ProcessNode<?>> pPredecessors) {
    if (pPredecessors.size()==0) { return Collections.emptyList(); }
    if (pPredecessors.size()==1) { return Collections.singleton(toDrawableNode(pPredecessors.iterator().next())); }

    List<DrawableProcessNode> result = new ArrayList<DrawableProcessNode>(pPredecessors.size());
    for(ProcessNode<?> elem: pPredecessors) {
      result.add(toDrawableNode(elem));
    }
    return result;
  }

  private static DrawableProcessNode toDrawableNode(ProcessNode<?> pElem) {
    if (pElem instanceof StartNode) {
      return DrawableStartNode.from((StartNode<?>) pElem);
    } else if (pElem instanceof EndNode) {
      throw new IllegalArgumentException("EndNodes should not see this function");
    } else if (pElem instanceof Join) {
      return DrawableJoin.from((Join<?>) pElem);
    } else if (pElem instanceof Activity) {
      return DrawableActivity.from((Activity<?>) pElem);
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
    return new Rectangle(0, 0, 200, 200);
  }


  public double getScale() {
    return aScale;
  }


  public void setScale(double pScale) {
    aScale = pScale;
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    Canvas canvas = pCanvas.childCanvas(getBounds(), aScale);
    Color red = canvas.newColor(255, 0, 0, 255);
    for(DrawableProcessNode node:getModelNodes()) {
      System.err.println("Drawing "+ node.getClass().getSimpleName()+" "+node.getId()+ "("+node.getX()+", "+node.getY()+")");
      // TODO actually support clipbounds

      node.draw(canvas.childCanvas(node.getBounds(), 1 ), null);
      canvas.drawFilledCircle(node.getX(), node.getY(), 1.5d, red);
    }
  }

  static void copyProcessNodeAttrs(ProcessNode<?> pFrom, DrawableProcessNode pTo) {
    pTo.setId(pFrom.getId());
    pTo.setX(pFrom.getX());
    pTo.setY(pFrom.getY());
  }

}
