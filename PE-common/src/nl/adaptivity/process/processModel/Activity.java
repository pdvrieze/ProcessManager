package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


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
@XmlRootElement(name = Activity.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Activity.ELEMENTNAME + "Type", propOrder = { "imports", "exports", XmlMessage.ELEMENTNAME, "condition" })
public class Activity extends ProcessNode {

  private static final long serialVersionUID = 282944120294737322L;

  /** The name of the XML element. */
  public static final String ELEMENTNAME = "activity";

  public static final String ELEM_CONDITION = "condition";

  public static final String ATTR_PREDECESSOR = "predecessor";

  private String aName;

  private Condition aCondition;

  private List<XmlImportType> aImports;

  private List<XmlExportType> aExports;

  private XmlMessage aMessage;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param pPredecessor The process node that starts immediately precedes this
   *          activity.
   */
  public Activity(final ProcessNode pPredecessor) {
    super(pPredecessor);
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public Activity() {}

  /**
   * Get the name of the activity.
   *
   * @return The name
   */
  @XmlAttribute
  public String getName() {
    return aName;
  }

  /**
   * Set the name of this activity. Note that for serialization to XML to work
   * this needs to be unique for the process model at time of serialization, and
   * can not be null or an empty string. While in Java mode other nodes are
   * referred to by reference, not name.
   *
   * @param pName The name of the activity.
   */
  public void setName(final String pName) {
    aName = pName;
  }

  /**
   * Get the condition of the activity.
   *
   * @return The condition.
   */
  @XmlElement(name = ELEM_CONDITION)
  public String getCondition() {
    return aCondition==null ? null : aCondition.toString();
  }

  /**
   * Set the condition that needs to be true to start this activity.
   *
   * @param pCondition The condition.
   */
  public void setCondition(final String pCondition) {
    aCondition = pCondition==null ? null : new Condition(pCondition);
  }

  /**
   * Get the list of imports. The imports are provided to the message for use as
   * data parameters.
   *
   * @return The list of imports.
   */
  @XmlElement(name = XmlImportType.ELEMENTNAME)
  public List<XmlImportType> getImports() {
    return aImports;
  }

  /**
   * Set the import requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param pImports The imports to set.
   */
  public void setImports(final Collection<XmlImportType> pImports) {
    aImports = new ArrayList<XmlImportType>(pImports.size());
    aImports.addAll(pImports);
  }

  /**
   * Get the list of exports. Exports will allow storing the response of an
   * activity.
   *
   * @return The list of exports.
   */
  @XmlElement(name = XmlExportType.ELEMENTNAME)
  public List<XmlExportType> getExports() {
    return aExports;
  }

  /**
   * Set the export requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param pExports The exports to set.
   */
  public void setExports(final Collection<XmlExportType> pExports) {
    aExports = new ArrayList<XmlExportType>(pExports.size());
    aExports.addAll(pExports);
  }

  /**
   * Get the predecessor node for this activity.
   *
   * @return the predecessor
   */
  @XmlAttribute(name = ATTR_PREDECESSOR, required = true)
  @XmlIDREF
  public ProcessNode getPredecessor() {
    final Collection<ProcessNode> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  /**
   * Set the predecessor for this activity.
   *
   * @param predecessor The predecessor
   */
  public void setPredecessor(final ProcessNode predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  /**
   * Get the message of this activity. This provides all the information to be
   * able to actually invoke the service.
   *
   * @return The message.
   */
  @XmlElement(name = XmlMessage.ELEMENTNAME, required = true)
  public XmlMessage getMessage() {
    return aMessage;
  }

  /**
   * Set the message of this activity. This encodes what actually needs to be
   * done when the activity is activated.
   *
   * @param message The message.
   */
  public void setMessage(final XmlMessage message) {
    aMessage = message;
  }

  /**
   * Determine whether the process can start.
   */
  @Override
  public boolean condition(final Task<?> pInstance) {
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
   * @todo handle imports.
   */
  @Override
  public <T, U extends Task<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    // TODO handle imports
    final T message = pMessageService.createMessage(aMessage);
    if (!pMessageService.sendMessage(message, pInstance)) {
      pInstance.failTask(new MessagingException("Failure to send message"));
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
  public <T, U extends Task<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return false;
  }

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.
   *
   * @return <code>false</code>
   */
  @Override
  public <T, U extends Task<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return false;
  }


}
