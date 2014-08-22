package nl.adaptivity.process.processModel.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import net.devrieze.util.db.DBTransaction;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.processModel.XmlMessage;


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of
 *
 * @author Paul de Vrieze
 */
@XmlRootElement(name = ActivityImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ActivityImpl.ELEMENTNAME + "Type", propOrder = { "defines", "results", XmlMessage.ELEMENTNAME, "condition" })
public class ActivityImpl extends ProcessNodeImpl implements Activity<ProcessNodeImpl> {

  private static final long serialVersionUID = 282944120294737322L;

  /** The name of the XML element. */
  public static final String ELEMENTNAME = "activity";

  public static final String ELEM_CONDITION = "condition";

  public static final String ATTR_PREDECESSOR = "predecessor";

  private String aName;

  private ConditionImpl aCondition;

  private List<XmlResultType> aResults;

  private List<XmlDefineType> aDefines;

  private XmlMessage aMessage;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param pPredecessor The process node that starts immediately precedes this
   *          activity.
   */
  public ActivityImpl(final ProcessNodeImpl pPredecessor) {
    super(Collections.singletonList(pPredecessor));
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public ActivityImpl() {}

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getName()
   */
  @Override
  @XmlAttribute
  public String getName() {
    return aName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setName(java.lang.String)
   */
  @Override
  public void setName(final String pName) {
    aName = pName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getCondition()
   */
  @Override
  @XmlElement(name = ELEM_CONDITION)
  public String getCondition() {
    return aCondition==null ? null : aCondition.toString();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setCondition(java.lang.String)
   */
  @Override
  public void setCondition(final String pCondition) {
    aCondition = pCondition==null ? null : new ConditionImpl(pCondition);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getImports()
   */
  @Override
  @XmlElement(name = XmlResultType.ELEMENTNAME)
  public List<? extends XmlResultType> getResults() {
    if (aResults==null) {
      aResults = new ArrayList<>();
    }
    return aResults;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setImports(java.util.Collection)
   */
  @Override
  public void setResults(final Collection<? extends IXmlResultType> pImports) {
    aResults = toExportableResults(pImports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getExports()
   */
  @Override
  @XmlElement(name = XmlDefineType.ELEMENTNAME)
  public List<? extends XmlDefineType> getDefines() {
    if (aDefines==null) {
      aDefines = new ArrayList<>();
    }
    return aDefines;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setExports(java.util.Collection)
   */
  @Override
  public void setDefines(final Collection<? extends IXmlDefineType> pExports) {
    aDefines = toExportableDefines(pExports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getPredecessor()
   */
  @Override
  @XmlAttribute(name = ATTR_PREDECESSOR, required = true)
  @XmlIDREF
  public ProcessNodeImpl getPredecessor() {
    final Collection<? extends ProcessNodeImpl> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setPredecessor(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public void setPredecessor(final ProcessNodeImpl predecessor) {
    setPredecessors(Collections.singleton(predecessor));
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getMessage()
   */
  @Override
  @XmlElement(name = XmlMessage.ELEMENTNAME, required = true)
  public XmlMessage getMessage() {
    return aMessage;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setMessage(nl.adaptivity.process.processModel.XmlMessage)
   */
  @Override
  public void setMessage(final IXmlMessage message) {
    aMessage = XmlMessage.get(message);
  }

  public void setMessage(final XmlMessage message) {
    aMessage = XmlMessage.get(message);
  }

  /**
   * Determine whether the process can start.
   */
  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    if (aCondition == null) {
      return true;
    }
    return aCondition.eval(pInstance);
  }

  /**
   * This will actually take the message element, and send it through the
   * message service.
   *
   * @param pMessageService The message service to use to send the message.
   * @param pInstance The processInstance that represents the actual activity
   *          instance that the message responds to.
   * @throws SQLException
   * @todo handle imports.
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(DBTransaction pTransaction, final IMessageService<T, U> pMessageService, final U pInstance) throws SQLException {
    // TODO handle imports
    final T message = pMessageService.createMessage(aMessage);
    try {
      if (!pMessageService.sendMessage(pTransaction, message, pInstance)) {
        pInstance.failTaskCreation(pTransaction, new MessagingException("Failure to send message"));
      }
    } catch (RuntimeException e) {
      pInstance.failTask(pTransaction, e);
      throw e;
    }

    return false;
  }

  /**
   * Take the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically taken.
   *
   * @return <code>false</code>
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return false;
  }

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.
   *
   * @return <code>false</code>
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return false;
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitActivity(this);
  }


}
