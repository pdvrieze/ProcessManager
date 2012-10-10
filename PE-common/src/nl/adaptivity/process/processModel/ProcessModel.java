package nl.adaptivity.process.processModel;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal;

import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.processModel.ProcessModel.PMXmlAdapter;


/**
 * A class representing a process model. This is too complex to directly support
 * JAXB serialization, so the {@link ProcessModelXmlAdapter} does that.
 *
 * @author Paul de Vrieze
 */
@XmlJavaTypeAdapter(PMXmlAdapter.class)
public class ProcessModel implements HandleAware<ProcessModel>, Serializable, SecureObject {

  static class PMXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel> {

    @Override
    public ProcessModel unmarshal(final XmlProcessModel pV) throws Exception {
      return pV.toProcessModel();
    }

    @Override
    public XmlProcessModel marshal(final ProcessModel pV) throws Exception {
      return new XmlProcessModel(pV);
    }

  }

  public enum Permissions implements SecurityProvider.Permission {
    INSTANTIATE;
  }

  private static final long serialVersionUID = -4199223546188994559L;

  private Collection<StartNode> aStartNodes;

  private int aEndNodeCount;

  private String aName;

  private long aHandle;

  private Principal aOwner;

  private Set<String> aRoles;

  /**
   * Create a new processModel based on the given endnodes. These endnodes must
   * have proper predecessors set, and end with {@link StartNode StartNodes}
   *
   * @param pEndNodes The endnodes
   */
  public ProcessModel(final Collection<EndNode> pEndNodes) {
    aStartNodes = reverseGraph(pEndNodes);
    aEndNodeCount = pEndNodes.size();
  }

  /**
   * Create a new processModel based on the given endnodes. This is a
   * convenience constructor. These endnodes must have proper predecessors set,
   * and end with {@link StartNode StartNodes}
   *
   * @param pEndNodes The endnodes
   */
  public ProcessModel(final EndNode... pEndNodes) {
    this(Arrays.asList(pEndNodes));
  }

  /**
   * Create a processModel out of the given xml representation.
   *
   * @param pXmlModel The xml representation to base the model on.
   */
  public ProcessModel(final XmlProcessModel pXmlModel) {
    final Collection<EndNode> endNodes = new ArrayList<EndNode>();

    for (final ProcessNode node : pXmlModel.getNodes()) {
      if (node instanceof EndNode) {
        endNodes.add((EndNode) node);
      }
    }

    aEndNodeCount = endNodes.size();

    aStartNodes = reverseGraph(endNodes);
    setName(pXmlModel.getName());
    final String owner = pXmlModel.getOwner();
    aOwner = owner == null ? null : new SimplePrincipal(pXmlModel.getOwner());
  }

  /**
   * Helper method that reverses the process model based on the set of endnodes.
   *
   * @param pEndNodes The endnodes to base the model on.
   * @return A collection of startNodes.
   */
  private static Collection<StartNode> reverseGraph(final Collection<EndNode> pEndNodes) {
    final Collection<StartNode> resultList = new ArrayList<StartNode>();
    for (final EndNode endNode : pEndNodes) {
      reverseGraph(resultList, endNode);
    }
    return resultList;
  }

  /**
   * Helper method for {@link #reverseGraph(Collection)} That does the actual
   * reversing. Note that predecessors will also be updated to add the node they
   * are predecessors to.
   *
   * @param pResultList The collection in which start nodes need to be stored.
   * @param pNode The node to do the reversion from.
   */
  private static void reverseGraph(final Collection<StartNode> pResultList, final ProcessNode pNode) {
    final Collection<ProcessNode> previous = pNode.getPredecessors();
    for (final ProcessNode prev : previous) {
      if (prev instanceof StartNode) {
        if (prev.getSuccessors() == null) {
          prev.addSuccessor(pNode);
        }
        pResultList.add((StartNode) prev);
      } else {
        if ((prev.getSuccessors() == null) || (prev.getSuccessors().size() == 0)) {
          prev.addSuccessor(pNode);
          reverseGraph(pResultList, prev);
        } else {
          prev.addSuccessor(pNode);
        }
      }
    }
  }

  /**
   * Get an array of all process nodes in the model. Used by XmlProcessModel
   *
   * @return An array of all nodes.
   */
  ProcessNode[] getModelNodes() {
    final Collection<ProcessNode> list = new ArrayList<ProcessNode>();
    final HashSet<String> seen = new HashSet<String>();
    if (aStartNodes != null) {
      for (final StartNode node : aStartNodes) {
        extractElements(list, seen, node);
      }
    }
    return list.toArray(new ProcessNode[list.size()]);
  }

  /**
   * Set the process nodes for the model. This will actually just retrieve the
   * {@link EndNode}s and sets the model accordingly. This does mean that only
   * passing {@link EndNode}s will have the same result, and the other nodes
   * will be pulled in.
   *
   * @param pProcessNodes The process nodes to base the model on.
   */
  public void setModelNodes(final ProcessNode[] pProcessNodes) {
    final ArrayList<EndNode> endNodes = new ArrayList<EndNode>();
    for (final ProcessNode n : pProcessNodes) {
      if (n instanceof EndNode) {
        endNodes.add((EndNode) n);
      }
    }
    aStartNodes = reverseGraph(endNodes);
    aEndNodeCount = endNodes.size();
  }

  /**
   * Helper method that helps enumerating all elements in the model
   *
   * @param pTo The collection that will contain the result.
   * @param pSeen A set of process names that have already been seen (and should
   *          not be added again.
   * @param pNode The node to start extraction from. This will go on to the
   *          successors.
   */
  private static void extractElements(final Collection<ProcessNode> pTo, final HashSet<String> pSeen, final ProcessNode pNode) {
    if (pSeen.contains(pNode.getId())) {
      return;
    }
    pTo.add(pNode);
    pSeen.add(pNode.getId());
    for (final ProcessNode node : pNode.getSuccessors()) {
      extractElements(pTo, pSeen, node);
    }
  }

  /**
   * Get the startnodes for this model.
   *
   * @return The start nodes.
   */
  public Collection<StartNode> getStartNodes() {
    return Collections.unmodifiableCollection(aStartNodes);
  }

  /**
   * Get the amount of end nodes in the model
   *
   * @return The amount of end nodes.
   */
  public int getEndNodeCount() {
    return aEndNodeCount;
  }

  /**
   * Get the name of the model.
   *
   * @return
   */
  public String getName() {
    return aName;
  }

  /**
   * Set the name of the model.
   *
   * @param name The name
   */
  public void setName(final String name) {
    aName = name;
  }

  /**
   * Get the handle recorded for this model.
   */
  @Override
  public long getHandle() {
    return aHandle;
  }

  /**
   * Set the handle for this model.
   */
  @Override
  public void setHandle(final long pHandle) {
    aHandle = pHandle;
  }

  /**
   * Get a reference node for this model.
   *
   * @return A reference node.
   */
  public ProcessModelRef getRef() {
    return new ProcessModelRef(getName(), aHandle);
  }

  public Principal getOwner() {
    return aOwner;
  }

  /**
   * @param pOwner
   * @return
   * @todo add security checks.
   */
  public void setOwner(final Principal pOwner) {
    aOwner = pOwner;
  }

  public Set<String> getRoles() {
    if (aRoles == null) {
      aRoles = new HashSet<String>();
    }
    return aRoles;
  }

  public void cacheStrings(final StringCache pStringCache) {
    if (aOwner instanceof SimplePrincipal) {
      aOwner = new SimplePrincipal(pStringCache.lookup(aOwner.getName()));
    } else if (aOwner instanceof DarwinPrincipal) {
      aOwner = ((DarwinPrincipal) aOwner).cacheStrings(pStringCache);
    }
    aName = pStringCache.lookup(aName);
    if ((aRoles != null) && (aRoles.size() > 0)) {
      final Set<String> roles = aRoles;
      aRoles = new HashSet<String>(aRoles.size() + (aRoles.size() >> 1));
      for (final String role : roles) {
        aRoles.add(pStringCache.lookup(role));
      }
    }
  }

  /**
   * Get the process node with the given id.
   * @param pNodeId The node id to look up.
   * @return The process node with the id.
   */
  public ProcessNode getNode(String pNodeId) {
    for(StartNode startNode: aStartNodes) {
      ProcessNode result = getNode(startNode, pNodeId);
      if (result!=null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Recursive helper for {@link #getNode(String)}
   */
  private static ProcessNode getNode(ProcessNode pBaseNode, String pNodeId) {
    if (pBaseNode.getId().equals(pNodeId)) { return pBaseNode; }
    for(ProcessNode node: pBaseNode.getSuccessors()) {
      ProcessNode result = getNode(node, pNodeId);
      if (result!=null) {
        return result;
      }
    }
    return null;
  }

}
