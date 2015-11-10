package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.diagram.DiagramNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.util.Identifiable;

import javax.xml.XMLConstants;

import java.security.Principal;
import java.util.*;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  public static final String NS_JBI = "http://adaptivity.nl/ProcessEngine/activity";

  public static final String NS_UMH = "http://adaptivity.nl/userMessageHandler";

  public static final String NS_PM = "http://adaptivity.nl/ProcessEngine/";

  static final String PROCESSMODEL_NS = NS_PM;

  private String aName;

  private ProcessNodeSet<T> aNodes;

  private double aTopPadding = 5d;
  private double aLeftPadding = 5d;
  private double aBottomPadding = 5d;
  private double aRightPadding = 5d;

  LayoutAlgorithm<T> aLayoutAlgorithm;

  private boolean mNeedsLayout = false;

  private UUID aUuid;

  private Principal aOwner;

  private Set<String> aRoles;

  private Collection<IXmlResultType> aImports;

  private Collection<IXmlDefineType> aExports;

  public ClientProcessModel(UUID uuid, final String name, final Collection<? extends T> nodes) {
    this(uuid, name, nodes, new LayoutAlgorithm<T>());
  }

  public ClientProcessModel(UUID uuid, final String name, final Collection<? extends T> nodes, LayoutAlgorithm<T> layoutAlgorithm) {
    aName = name;
    aLayoutAlgorithm = layoutAlgorithm == null ? new LayoutAlgorithm<T>() : layoutAlgorithm;
    setNodes(nodes);
    aUuid = uuid==null ? UUID.randomUUID() : uuid;
  }

  public void setNodes(final Collection<? extends T> nodes) {
    aNodes = ProcessNodeSet.processNodeSet(nodes);
    for(T node: aNodes) {
      node.setOwner(this);
    }
    invalidate();
  }

  public LayoutAlgorithm<T> getLayoutAlgorithm() {
    return aLayoutAlgorithm;
  }

  public void setLayoutAlgorithm(LayoutAlgorithm<T> layoutAlgorithm) {
    aLayoutAlgorithm = layoutAlgorithm;
  }

  public double getVertSeparation() {
    return aLayoutAlgorithm.getVertSeparation();
  }


  public void setVertSeparation(double vertSeparation) {
    if (aLayoutAlgorithm.getVertSeparation()!=vertSeparation) {
      invalidate();
    }
    aLayoutAlgorithm.setVertSeparation(vertSeparation);
  }


  public double getHorizSeparation() {
    return aLayoutAlgorithm.getHorizSeparation();
  }


  public void setHorizSeparation(double horizSeparation) {
    if (aLayoutAlgorithm.getHorizSeparation()!=horizSeparation) {
      invalidate();
    }
    aLayoutAlgorithm.setHorizSeparation(horizSeparation);
  }

  public double getDefaultNodeWidth() {
    return aLayoutAlgorithm.getDefaultNodeWidth();
  }


  public void setDefaultNodeWidth(double defaultNodeWidth) {
    if (aLayoutAlgorithm.getDefaultNodeWidth()!=defaultNodeWidth) {
      invalidate();
    }
    aLayoutAlgorithm.setDefaultNodeWidth(defaultNodeWidth);
  }


  public double getDefaultNodeHeight() {
    return aLayoutAlgorithm.getDefaultNodeHeight();
  }


  public void setDefaultNodeHeight(double defaultNodeHeight) {
    if (aLayoutAlgorithm.getDefaultNodeHeight()!=defaultNodeHeight) {
      invalidate();
    }
    aLayoutAlgorithm.setDefaultNodeHeight(defaultNodeHeight);
  }


  public double getTopPadding() {
    return aTopPadding;
  }


  public void setTopPadding(double topPadding) {
    double offset = topPadding-aTopPadding;
    for(T n:aNodes) {
      n.setY(n.getY()+offset);
    }
    aTopPadding = topPadding;
  }


  public double getLeftPadding() {
    return aLeftPadding;
  }


  public void setLeftPadding(double leftPadding) {
    double offset = leftPadding-aLeftPadding;
    for(T n:aNodes) {
      n.setX(n.getX()+offset);
    }
    aLeftPadding = leftPadding;
  }


  public double getBottomPadding() {
    return aBottomPadding;
  }


  public void setBottomPadding(double bottomPadding) {
    aBottomPadding = bottomPadding;
  }


  public double getRightPadding() {
    return aRightPadding;
  }


  public void setRightPadding(double rightPadding) {
    aRightPadding = rightPadding;
  }


  public void invalidate() {
    mNeedsLayout  = true;
  }

  public void resetLayout() {
    for (T n:aNodes) {
      n.setX(Double.NaN);
      n.setY(Double.NaN);
    }
    invalidate();
  }

  public boolean isInvalid() {
    return mNeedsLayout;
  }

  /**
   * @param node The node that has changed.
   */
  public void nodeChanged(T node) {
    // no implementation here
  }

  @Override
  public UUID getUuid() {
    return aUuid;
  }

  public void setUuid(UUID uuid) {
    aUuid = uuid;
  }

  @Override
  public int getEndNodeCount() {
    int i=0;
    for(T node: getModelNodes()) {
      if (node instanceof EndNode) { ++i; }
    }
    return i;
  }

  @Override
  public IProcessModelRef<T> getRef() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public T getNode(Identifiable nodeId) {
    for(T n: getModelNodes()) {
      if (nodeId.getId().equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  public T getNode(String nodeId) {
    for(T n: getModelNodes()) {
      if (nodeId.equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  @Override
  public List<? extends T> getModelNodes() {
    if (aNodes == null) {
      aNodes = ProcessNodeSet.processNodeSet();
    }
    return aNodes;
  }

  @Override
  public String getName() {
    return aName;
  }

  public void setName(String name) {
    aName = name;
  }

  public void setOwner(String owner) {
    aOwner = new SimplePrincipal(owner);
  }

  public void setOwner(Principal owner) {
    aOwner = owner;
  }

  @Override
  public Principal getOwner() {
    return aOwner;
  }

  @Override
  public Collection<IXmlResultType> getImports() {
    return aImports;
  }

  @Override
  public Collection<IXmlDefineType> getExports() {
    return aExports;
  }

  @Override
  public Set<String> getRoles() {
    return aRoles;
  }

  @Override
  public Collection<? extends ClientStartNode<? extends T>> getStartNodes() {
    List<ClientStartNode<? extends T>> result = new ArrayList<>();
    for(T n:getModelNodes()) {
      if (n instanceof ClientStartNode) {
        result.add((ClientStartNode<? extends T>) n);
      }
    }
    return result;
  }

  public void addNode(T node) {
    aNodes.add(node);
    node.setOwner(this);
    // Make sure that children can know of the change.
    nodeChanged(node);
  }

  public void removeNode(int nodePos) {
    T node = aNodes.remove(nodePos);
    disconnectNode(node);
  }

  void removeNode(T node) {
    if (node==null) { return; }
    if (aNodes.remove(node)) {
      disconnectNode(node);
    }
  }

  private void disconnectNode(T node) {
    node.disconnect();
    nodeChanged(node);
  }

  public void layout() {
    final List<DiagramNode<T>> diagramNodes = toDiagramNodes(getModelNodes());
    if(aLayoutAlgorithm.layout(diagramNodes)) {
      double maxX = Double.MIN_VALUE;
      double maxY = Double.MIN_VALUE;
      for(DiagramNode<T> n:diagramNodes) {
        n.getTarget().setX(n.getX()+getLeftPadding());
        n.getTarget().setY(n.getY()+getTopPadding());
        maxX = Math.max(n.getRight(), maxX);
        maxY = Math.max(n.getBottom(), maxY);
      }
    }
  }

  public void serialize(SerializerAdapter out) {
    out.addNamespace(XMLConstants.NULL_NS_URI, NS_PM);
    out.addNamespace("umh", NS_UMH);
    out.addNamespace("jbi", NS_JBI);

    out.startTag(NS_PM, "processModel", true);
    if (aName!=null) {
      out.addAttribute(null, "name", aName);
    }
    if (aUuid!=null) {
      out.addAttribute(null, "uuid", aUuid.toString());
    }
    for(T node:aNodes) {
      node.serialize(out);
    }
    out.endTag(NS_PM, "processModel", true);
  }

  private List<DiagramNode<T>> toDiagramNodes(Collection<? extends T> modelNodes) {
    HashMap<T,DiagramNode<T>> map = new HashMap<>();
    List<DiagramNode<T>> result = new ArrayList<>();
    for(T node:modelNodes) {
      final double leftExtend;
      final double rightExtend;
      final double topExtend;
      final double bottomExtend;
      if (node instanceof Bounded) {
        boolean tempCoords = Double.isNaN(node.getX())||Double.isNaN(node.getY());
        double tmpX=0;
        double tmpY=0;
        if (tempCoords) {
          tmpX=node.getX();node.setX(0);
          tmpY=node.getY();node.setY(0);
          mNeedsLayout=true; // we need layout as we have undefined coordinates.
        }
        Rectangle bounds = ((Bounded)node).getBounds();
        leftExtend = node.getX()-bounds.left;
        rightExtend = bounds.right()-node.getX();
        topExtend = node.getY()-bounds.top;
        bottomExtend = bounds.bottom()-node.getY();
        if (tempCoords) {
          node.setX(tmpX);
          node.setY(tmpY);
        }
      } else {
        leftExtend = rightExtend = aLayoutAlgorithm.getDefaultNodeWidth()/2;
        topExtend = bottomExtend = aLayoutAlgorithm.getDefaultNodeHeight()/2;
      }
      DiagramNode<T> dn = new DiagramNode<>(node, leftExtend, rightExtend, topExtend, bottomExtend);
      if (node.getId()!=null) {
        map.put(node, dn);
      }
      result.add(dn);
    }

    for(DiagramNode<T> dn:result) {
      T mn = dn.getTarget();
      for(T successor:mn.getSuccessors()) {
        DiagramNode<T> rightdn = map.get(successor);
        if (rightdn!=null) {
          dn.getRightNodes().add(rightdn);
        }
      }
      for(Identifiable predecessorId:mn.getPredecessors()) {
        T predecessor = getNode(predecessorId);
        DiagramNode<T> leftdn = map.get(predecessor);
        if (leftdn!=null) {
          dn.getLeftNodes().add(leftdn);
        }
      }
    }
    return result;
  }

}
