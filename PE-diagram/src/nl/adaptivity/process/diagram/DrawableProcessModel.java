package nl.adaptivity.process.diagram;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableProcessModel extends ProcessModel implements Diagram {

  private static final long serialVersionUID = 633050227182642699L;

  public static final double STARTNODERADIUS=8d;
  public static final double ENDNODEOUTERRADIUS=12d;
  public static final double ENDNODEINNERRRADIUS=6d;
  public static final double ACTIVITYWIDTH=32d;
  public static final double ACTIVITYHEIGHT=ACTIVITYWIDTH;
  public static final double ACTIVITYROUNDX=ACTIVITYWIDTH/8d;
  public static final double ACTIVITYROUNDY=ACTIVITYHEIGHT/8d;
  public static final double JOINWIDTH=24d;
  public static final double JOINHEIGHT=JOINWIDTH;

  public DrawableProcessModel(ProcessModel pOriginal) {
    super(getDrawableEndNodes(pOriginal.getStartNodes()));
  }

  private static Collection<DrawableEndNode> getDrawableEndNodes(Collection<StartNode> pStartNodes) {
    Set<EndNode> origEndNodes = getEndNodes(new HashSet<EndNode>(), pStartNodes);
    ArrayList<DrawableEndNode> result = new ArrayList<>(pStartNodes.size());
    for(EndNode n: origEndNodes) {
      result.add(toDrawableEndNode(n));
    }
    return result;
  }

  private static Set<EndNode> getEndNodes(Set<EndNode> pSet, Collection<? extends ProcessNode> pNodes) {
    for(ProcessNode node: pNodes) {
      if (node instanceof EndNode) {
        pSet.add((EndNode) node);
      } else {
        getEndNodes(pSet, node.getSuccessors());
      }

    }
    return pSet;
  }

  private static DrawableEndNode toDrawableEndNode(EndNode pN) {
    DrawableEndNode result = DrawableEndNode.from(pN);
    result.setPredecessors(toDrawableNodes(pN.getPredecessors()));
    return result;
  }

  private static Collection<? extends ProcessNode> toDrawableNodes(Collection<? extends ProcessNode> pPredecessors) {
    if (pPredecessors.size()==0) { return Collections.emptyList(); }
    if (pPredecessors.size()==1) { return Collections.singleton(toDrawableNode(pPredecessors.iterator().next())); }

    List<ProcessNode> result = new ArrayList<>(pPredecessors.size());
    for(ProcessNode elem: pPredecessors) {
      result.add(toDrawableNode(elem));
    }
    return result;
  }

  private static ProcessNode toDrawableNode(ProcessNode pElem) {
    if (pElem instanceof StartNode) {
      return DrawableStartNode.from((StartNode) pElem);
    } else if (pElem instanceof EndNode) {
      throw new IllegalArgumentException("EndNodes should not see this function");
    } else if (pElem instanceof Join) {
      return DrawableJoin.from((Join) pElem);
    } else if (pElem instanceof Activity) {
      return DrawableActivity.from((Activity) pElem);
    } else {
      throw new UnsupportedOperationException("Unsupported subclass to ProcessNode");
    }
  }

  @Override
  public Rectangle getBounds() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

  static void copyProcessNodeAttrs(ProcessNode pFrom, ProcessNode pTo) {
    pTo.setId(pFrom.getId());
    pTo.setX(pFrom.getX());
    pTo.setY(pFrom.getY());
  }

}
