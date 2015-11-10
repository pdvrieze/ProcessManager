package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.ComparableHandle;
import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.Handles;
import net.devrieze.util.Transaction;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XmlSerializable;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.security.Principal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProcessInstance implements Serializable, HandleAware<ProcessInstance>, SecureObject, XmlSerializable {

  public enum State {
    NEW,
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED;
  }

  @XmlRootElement(name = "processInstance", namespace = Constants.PROCESS_ENGINE_NS)
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    private long aHandle;

    private long aProcessModel;

    private String aName;

    private String aUUID;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(final ProcessInstance processInstance) {
      setHandle(processInstance.getHandle());
      setProcessModel(processInstance.aProcessModel.getHandle());
      aName = processInstance.getName();
      if ((aName == null) || (aName.trim().length() == 0)) {
        aName = processInstance.aProcessModel.getName() + " instance " + aHandle;
      }
      aUUID = processInstance.getUUID()==null ? null : processInstance.getUUID().toString();
    }

    public void setHandle(final long handle) {
      aHandle = handle;
    }

    @Override
    @XmlAttribute(name = "handle")
    public long getHandle() {
      return aHandle;
    }

    public void setProcessModel(final long processModel) {
      aProcessModel = processModel;
    }

    @XmlAttribute(name = "processModel")
    public long getProcessModel() {
      return aProcessModel;
    }

    @XmlAttribute(name = "name")
    public String getName() {
      return aName;
    }

    public void setName(final String name) {
      aName = name;
    }

    @XmlAttribute(name="uuid")
    public String getUUID() {
      return aUUID;
    }

    public void setUUID(String uUID) {
      aUUID = uUID;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModelImpl aProcessModel;

  private final Collection<Handle<? extends ProcessNodeInstance>> aThreads;

  private final Collection<Handle<? extends ProcessNodeInstance>> aFinishedNodes;

  private final Collection<Handle<? extends ProcessNodeInstance>> aEndResults;

  private HashMap<JoinImpl, JoinInstance> aJoins;

  private long aHandle;

  private final ProcessEngine aEngine;

  private List<ProcessData> aInputs = new ArrayList<>();

  private List<ProcessData> aOutputs = new ArrayList<>();

  private final String aName;

  private final Principal aOwner;

  private State aState;

  private final UUID aUUid;

  ProcessInstance(final long handle, final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    aHandle = handle;
    aProcessModel = processModel;
    aOwner = owner;
    aUUid = uUid;
    aEngine = engine;
    aName =name;
    aState = state==null ? State.NEW : state;
    aThreads = new LinkedList<>();
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
    aFinishedNodes = new ArrayList<>();
  }

  public ProcessInstance(final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    aProcessModel = processModel;
    aName = name;
    aUUid = uUid;
    aEngine = engine;
    aThreads = new LinkedList<>();
    aOwner = owner;
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
    aFinishedNodes = new ArrayList<>();
    aState = state == null ? State.NEW : state;
  }

  void setChildren(Transaction transaction, final Collection<? extends Handle<? extends ProcessNodeInstance>> children) throws SQLException {
    aThreads.clear();
    aFinishedNodes.clear();
    aEndResults.clear();

    List<ProcessNodeInstance> nodes = new ArrayList<>();
    TreeMap<ComparableHandle<? extends ProcessNodeInstance>,ProcessNodeInstance> threads = new TreeMap<>();

    for(Handle<? extends ProcessNodeInstance> handle: children) {
      final ProcessNodeInstance inst = aEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      nodes.add(inst);
      threads.put(Handles.handle(handle), inst);
    }

    for(ProcessNodeInstance instance: nodes) {

      if (instance.getNode() instanceof EndNode<?>) {
        aEndResults.add(instance);
        threads.remove(Handles.handle(instance));
      }

      final Collection<Handle<? extends ProcessNodeInstance>> preds = instance.getDirectPredecessors();
      if (preds!=null) {
        for(Handle<? extends ProcessNodeInstance> pred:preds) {
          ComparableHandle<? extends ProcessNodeInstance> handle = Handles.handle(pred);
          if (threads.containsKey(handle)) {
            aFinishedNodes.add(threads.get(handle));
            threads.remove(handle);
          }
        }
      }

    }
    aThreads.addAll(threads.values());
  }

  void setThreads(Transaction transaction, final Collection<? extends Handle<? extends ProcessNodeInstance>> threads) throws SQLException {
    for(Handle<? extends ProcessNodeInstance> threadHeadHandle:threads) {
      aThreads.add(aEngine.getNodeInstance(transaction, threadHeadHandle, SecurityProvider.SYSTEMPRINCIPAL));
    }
  }

  public void initialize(Transaction transaction) throws SQLException {
    if (aState!=State.NEW || aThreads.size()>0) {
      throw new IllegalStateException("The instance already appears to be initialised");
    }
    for (final StartNodeImpl node : aProcessModel.getStartNodes()) {
      final ProcessNodeInstance instance = new ProcessNodeInstance(node, null, this);
      aEngine.registerNodeInstance(transaction, instance);
      aThreads.add(instance);
    }
    aState = State.INITIALIZED;
    aEngine.updateStorage(transaction, this);
  }

  public synchronized void finish(Transaction transaction) throws SQLException {
    int aFinished = getFinishedCount();
    if (aFinished >= aProcessModel.getEndNodeCount()) {
      // TODO mark and store results
      aState=State.FINISHED;
      aEngine.updateStorage(transaction, this);
      transaction.commit();
      aEngine.finishInstance(transaction, this);
    }
  }

  private int getFinishedCount() {
    return aEndResults.size();
  }

  public synchronized JoinInstance getJoinInstance(Transaction transaction, final JoinImpl join, final ProcessNodeInstance predecessor) throws SQLException {
    JoinInstance result = aJoins.get(join);
    if (result == null) {
      final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(join.getPredecessors().size());
      predecessors.add(predecessor);
      result = new JoinInstance(transaction, join, predecessors, this);
      aJoins.put(join, result);
    } else {
      result.addPredecessor(transaction, predecessor);
    }
    return result;
  }

  public synchronized void removeJoin(final JoinInstance j) {
    aJoins.remove(j.getNode());
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(final long handle) {

    aHandle = handle;
  }

  public String getName() {
    return aName;
  }

  public Principal getOwner() {
    return aOwner;
  }

  public UUID getUUID() {
    return aUUid;
  }

  public ProcessEngine getEngine() {
    return aEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public ProcessModelImpl getProcessModel() {
    return aProcessModel;
  }

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  public List<ProcessData> getInputs() {
    return aInputs;
  }

  public synchronized State getState() {
    return aState;
  }

  public synchronized void start(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final Node payload) throws SQLException {
    if (aState==null) {
      initialize(transaction);
    }
    if (aThreads.size() == 0) {
      throw new IllegalStateException("No starting nodes in process");
    }
    aInputs = aProcessModel.toInputs(payload);
    for (final Handle<? extends ProcessNodeInstance> hnode : aThreads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      provideTask(transaction, messageService, node);
    }
    aState = State.STARTED;
    aEngine.updateStorage(transaction, this);
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance. */
  public void reinitialize(Transaction transaction) {
    // TODO Auto-generated method stub

  }

  public synchronized void provideTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.provideTask(transaction, messageService)) {
      takeTask(transaction, messageService, node);
    }
  }

  public synchronized void takeTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.takeTask(transaction, messageService)) {
      startTask(transaction, messageService, node);
    }
  }

  public synchronized void startTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.startTask(transaction, messageService)) {
      finishTask(transaction, messageService, node, null);
    }
  }

  public synchronized void finishTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node, final Node resultPayload) throws SQLException {
    if (node.getState()==TaskState.Complete) {
      throw new IllegalStateException("Task was already complete");
    }
    node.finishTask(transaction, resultPayload);
    // Make sure the finish is recorded.
    transaction.commit();

    try {
      if (node.getNode() instanceof EndNode) {
        aEndResults.add(node);
        aThreads.remove(node);
        finish(transaction);
      } else {
        startSuccessors(transaction, messageService, node);
      }
    } catch (RuntimeException|SQLException e) {
      transaction.rollback();
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e);
    }

  }

  private void startSuccessors(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance predecessor) throws SQLException {
    if (! aFinishedNodes.contains(predecessor)) {
      aFinishedNodes.add(predecessor);
    }
    aThreads.remove(predecessor);

    final List<ProcessNodeInstance> startedTasks = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    for (final ProcessNodeImpl successorNode : predecessor.getNode().getSuccessors()) {
      final ProcessNodeInstance instance = getProcessNodeInstance(transaction, predecessor, successorNode);
      if (instance instanceof JoinInstance && aThreads.contains(instance)) {
        continue;
      } else {
        aThreads.add(instance);
        startedTasks.add(instance);
        aEngine.registerNodeInstance(transaction, instance);
      }
    }
    // Commit the registration of the follow up nodes before starting them.
    transaction.commit();
    for (final ProcessNodeInstance task : startedTasks) {
      provideTask(transaction, messageService, task);
    }
  }

  private ProcessNodeInstance getProcessNodeInstance(Transaction transaction, final ProcessNodeInstance predecessor, final ProcessNodeImpl node) throws SQLException {
    if (node instanceof JoinImpl) {
      final JoinImpl join = (JoinImpl) node;
      if (aJoins == null) {
        aJoins = new HashMap<>();
      }
      JoinInstance instance = aJoins.get(join);
      if (instance == null) {

        final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(node.getPredecessors().size());
        predecessors.add(predecessor);
        instance = new JoinInstance(transaction, join, predecessors, this);
        aJoins.put(join, instance);
      } else {
        instance.addPredecessor(transaction, predecessor);
      }
      return instance;

    } else {
      return new ProcessNodeInstance(node, predecessor, this);
    }
  }

  public synchronized void failTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node, final Throwable cause) throws SQLException {
    node.failTask(transaction, cause);
  }

  public synchronized void cancelTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    node.cancelTask(transaction);
  }

  public synchronized Collection<ProcessNodeInstance> getActivePredecessorsFor(Transaction transaction, final JoinImpl join) throws
          SQLException {
    final ArrayList<ProcessNodeInstance> activePredecesors = new ArrayList<>(Math.min(join.getPredecessors().size(), aThreads.size()));
    for (final Handle<? extends ProcessNodeInstance> hnode : aThreads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      if (node.getNode().isPredecessorOf(join)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance>> getDirectSuccessors(Transaction transaction, final ProcessNodeInstance predecessor) throws SQLException {
    final ArrayList<Handle<? extends ProcessNodeInstance>> result = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    for (final Handle<? extends ProcessNodeInstance> hcandidate : aThreads) {
      ProcessNodeInstance candidate = getEngine().getNodeInstance(transaction, hcandidate, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, candidate, predecessor);
    }
    return result;
  }

  private void addDirectSuccessor(Transaction transaction, final ArrayList<Handle<? extends ProcessNodeInstance>> result, ProcessNodeInstance candidate, final Handle<? extends ProcessNodeInstance> predecessor) throws SQLException {
    // First look for this node, before diving into it's children
    for (final Handle<? extends ProcessNodeInstance> node : candidate.getDirectPredecessors()) {
      if (node.getHandle() == predecessor.getHandle()) {
        result.add(candidate);
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final Handle<? extends ProcessNodeInstance> hnode : candidate.getDirectPredecessors()) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      ProcessNodeInstance node = candidate.getProcessInstance().getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, node, predecessor);
    }
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getActive() {
    return aThreads;
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getFinished() {
    return aFinishedNodes;
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getResults() {
    return aEndResults;
  }

  @Override
  public void serialize(XMLStreamWriter out) throws XMLStreamException {
    //
    if(out.getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      out.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    }
    out.writeStartElement(Constants.PROCESS_ENGINE_NS, "processInstance");
    try {
      out.writeAttribute("handle", Long.toString(aHandle));
      out.writeAttribute("name", aName);
      out.writeAttribute("processModel", Long.toString(getProcessModel().getHandle()));
      out.writeAttribute("owner", aOwner.getName());
      out.writeAttribute("state", aState.name());

      out.writeStartElement(Constants.PROCESS_ENGINE_NS, "inputs");
      try {
        for(ProcessData input:aInputs) {
          input.serialize(out);
        }
      } finally {
        out.writeEndElement();
      }

      out.writeStartElement(Constants.PROCESS_ENGINE_NS, "outputs");
      try {
        for(ProcessData output:aOutputs) {
          output.serialize(out);
        }
      } finally {
        out.writeEndElement();
      }

      try(Transaction transaction = getEngine().startTransaction()) {

        if (aThreads.size() > 0) {
          try {
            out.writeStartElement(Constants.PROCESS_ENGINE_NS, "active");
            for (Handle<? extends ProcessNodeInstance> active : aThreads) {
              writeActiveNodeRef(transaction, out, active);
            }
          } finally {
            out.writeEndElement();
          }
        }
        if (aFinishedNodes.size() > 0) {
          try {
            out.writeStartElement(Constants.PROCESS_ENGINE_NS, "finished");
            for (Handle<? extends ProcessNodeInstance> finished : aFinishedNodes) {
              writeActiveNodeRef(transaction, out, finished);
            }
          } finally {
            out.writeEndElement();
          }
        }
        if (aEndResults.size() > 0) {
          try {
            out.writeStartElement(Constants.PROCESS_ENGINE_NS, "endresults");
            for (Handle<? extends ProcessNodeInstance> result : aEndResults) {
              writeResultNodeRef(transaction, out, result);
            }
          } finally {
            out.writeEndElement();
          }
        }
        transaction.commit();
      } catch (SQLException e) {
        throw new XMLStreamException(e);
      }
    } finally {
      out.writeEndElement();
    }

  }

  private void writeActiveNodeRef(Transaction transaction, XMLStreamWriter out, Handle<? extends ProcessNodeInstance> handleNodeInstance) throws
          XMLStreamException, SQLException {
    ProcessNodeInstance nodeInstance = getEngine().getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL);
    out.writeStartElement(Constants.PROCESS_ENGINE_NS, "nodeinstance");
    try {
      writeNodeRefCommon(out, nodeInstance);
    } finally{
      out.writeEndElement();
    }
  }

  private void writeResultNodeRef(Transaction transaction, XMLStreamWriter out, Handle<? extends ProcessNodeInstance> handleNodeInstance) throws
          XMLStreamException, SQLException {
    ProcessNodeInstance nodeInstance = getEngine().getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL);
    out.writeStartElement(Constants.PROCESS_ENGINE_NS, "nodeinstance");
    try {
      writeNodeRefCommon(out, nodeInstance);
      out.writeStartElement(Constants.PROCESS_ENGINE_NS, "results");
      try {
        List<ProcessData> results = nodeInstance.getResults();
        for(ProcessData result:results) {
          result.serialize(out);
        }
      } finally {
        out.writeEndElement();
      }
    } finally{
      out.writeEndElement();
    }
  }

  private static void writeNodeRefCommon(XMLStreamWriter out, ProcessNodeInstance nodeInstance) throws XMLStreamException {
    out.writeAttribute("nodeid", nodeInstance.getNode().getId());
    out.writeAttribute("handle", Long.toString(nodeInstance.getHandle()));
    out.writeAttribute("state", nodeInstance.getState().toString());
    if (nodeInstance.getState()==TaskState.Failed) {
      final Throwable failureCause = nodeInstance.getFailureCause();
      out.writeAttribute("failureCause", failureCause==null? "<unknown>" : failureCause.getClass().getName()+": "+failureCause.getMessage());
    }

  }

  void setInputs(List<ProcessData> inputs) {
    aInputs.clear();
    aInputs.addAll(inputs);
  }

  public void setOutputs(List<ProcessData> outputs) {
    aOutputs.clear();
    aOutputs.addAll(outputs);
  }

  /**
   * Trigger the instance to reactivate pending tasks.
   * @param transaction The database transaction to use
   * @param messageService The message service to use for messenging.
   */
  public void tickle(Transaction transaction, IMessageService<?, ProcessNodeInstance> messageService) {
    for(Handle<? extends ProcessNodeInstance> handle: aThreads) {
      try {
        getEngine().tickleNode(transaction, handle);
      } catch (SQLException e) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when tickling process instance", e);
      }
    }
    if (aThreads.isEmpty()) {
      try {
        finish(transaction);
      } catch (SQLException e) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when trying to finish a process instance as result of tickling", e);
      }
    }
  }

}
