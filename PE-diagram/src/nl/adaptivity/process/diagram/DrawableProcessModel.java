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
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.engine.ActivityImpl;
import nl.adaptivity.process.processModel.engine.EndNodeImpl;



public class DrawableProcessModel extends ClientProcessModel implements Diagram {

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

  private double aScale = 1;

  public DrawableProcessModel(ProcessModel pOriginal) {
    super(pOriginal.getName(), getDrawableNodes(pOriginal.getStartNodes()));
  }

  private static Collection<DrawableProcessNode> getDrawableNodes(Collection<StartNode> pStartNodes) {
    Set<EndNode> origEndNodes = getDrawableNodes(new HashSet<EndNode>(), pStartNodes);
    ArrayList<DrawableProcessNode> result = new ArrayList<DrawableProcessNode>(pStartNodes.size());
    for(EndNode n: origEndNodes) {
      result.add(toDrawableEndNode(n));
    }
    return result;
  }

  private static Set<EndNode> getDrawableNodes(Set<EndNode> pSet, Collection<? extends ProcessNode> pNodes) {
    for(ProcessNode node: pNodes) {
      if (node instanceof EndNode) {
        pSet.add((EndNodeImpl) node);
      }
      getDrawableNodes(pSet, node.getSuccessors());

    }
    return pSet;
  }

  private static DrawableEndNode toDrawableEndNode(EndNode pN) {
    DrawableEndNode result = DrawableEndNode.from(pN);
    result.setPredecessors(toDrawableNodes(pN.getPredecessors()));
    return result;
  }

  private static Collection<? extends DrawableProcessNode> toDrawableNodes(Collection<? extends ProcessNode> pPredecessors) {
    if (pPredecessors.size()==0) { return Collections.emptyList(); }
    if (pPredecessors.size()==1) { return Collections.singleton(toDrawableNode(pPredecessors.iterator().next())); }

    List<DrawableProcessNode> result = new ArrayList<DrawableProcessNode>(pPredecessors.size());
    for(ProcessNode elem: pPredecessors) {
      result.add(toDrawableNode(elem));
    }
    return result;
  }

  private static DrawableProcessNode toDrawableNode(ProcessNode pElem) {
    if (pElem instanceof StartNode) {
      return DrawableStartNode.from((StartNode) pElem);
    } else if (pElem instanceof EndNode) {
      throw new IllegalArgumentException("EndNodes should not see this function");
    } else if (pElem instanceof Join) {
      return DrawableJoin.from((Join) pElem);
    } else if (pElem instanceof ActivityImpl) {
      return DrawableActivity.from((ActivityImpl) pElem);
    } else {
      throw new UnsupportedOperationException("Unsupported subclass to ProcessNode");
    }
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(0, 0, 200, 200);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    System.err.println("THIS SHOULD BE SEEN");
    Set<ProcessNode> seenNodes = new HashSet<ProcessNode>();
    for(ProcessNode node:getStartNodes()) {
      System.err.println("Adding node: ["+node.getClass().getSimpleName()+"] "+node.getId());
      if(seenNodes.add(node)) {
        addSuccessors(seenNodes,node);
      }
    }
    for(ProcessNode node: seenNodes) {
      System.err.println("Drawing node "+node.getId());
      Drawable n = (Drawable) node;
      // TODO actually support clipbounds
      n.draw(pCanvas.childCanvas(n.getBounds(), aScale ), null);
    }
    Color color = pCanvas.newColor(255, 0, 0, 255);
    pCanvas.drawCircle(200, 200, 150, color);
    pCanvas.drawPath(new double[]{0,0,200,200}, color);
  }

  private void addSuccessors(Set<ProcessNode> pSeenNodes, ProcessNode pNode) {
    for(ProcessNode n:pNode.getSuccessors()) {
      if(pSeenNodes.add(n)) {
        addSuccessors(pSeenNodes, n);
      }
    }
  }

  static void copyProcessNodeAttrs(ProcessNode pFrom, DrawableProcessNode pTo) {
    pTo.setId(pFrom.getId());
    pTo.setX(pFrom.getX());
    pTo.setY(pFrom.getY());
  }

}
