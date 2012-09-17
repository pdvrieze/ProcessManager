package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.exec.Task;

/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that they
 * either invoke another process (and the process engine can treat this specially 
 * in later versions), or set interaction with the user. Services can use the ActivityResponse
 * soap header to indicate support for processes and what the actual state of the task
 * after return should be (instead of complete). 
 * @author pdvrieze
 *
 */
@XmlRootElement(name = Activity.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Activity.ELEMENTNAME+"Type", propOrder = { XmlImportType.ELEMENTNAME, XmlImportType.ELEMENTNAME, XmlMessage.ELEMENTNAME, "condition" })
public class Activity extends ProcessNode{

  private static final long serialVersionUID = 282944120294737322L;
  
  /** The name of the XML element. */
  public static final String ELEMENTNAME = "activity";
  public static final String ELEM_CONDITION = "condition";
  public static final String ATTR_PREDECESSOR = "predecessor";

  private String aName;
  private String aCondition;
  private List<XmlImportType> aImports;
  private List<XmlExportType> aExports;

  private XmlMessage aMessage;

  /**
   * Create a new Activity. Note that activities can only have a a single predecessor.
   * @param pPredecessor The process node that starts immediately precedes this activity.
   */
  public Activity(ProcessNode pPredecessor) {
    super(pPredecessor);
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB to work.
   */
  public Activity() {
  }

  @Override
  public boolean condition() {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");

  }

  @XmlAttribute
  public String getName() {
    return aName;
  }

  public void setName(String pName) {
    aName = pName;
  }

  public void setCondition(String pCondition) {
    aCondition = pCondition;
  }

  public void setImports(List<XmlImportType> pImports) {
    aImports = new ArrayList<XmlImportType>(pImports.size());
    aImports.addAll(pImports);
  }

  public void setExports(List<XmlExportType> pExports) {
    aExports = new ArrayList<XmlExportType>(pExports.size());
    aExports.addAll(pExports);
  }


  @XmlElement(name=ELEM_CONDITION)
  public String getCondition() {
    return aCondition;
  }


  @XmlElement(name=XmlImportType.ELEMENTNAME)
  public List<XmlImportType> getImports() {
    return aImports;
  }


  @XmlElement(name=XmlExportType.ELEMENTNAME)
  public List<XmlExportType> getExports() {
    return aExports;
  }

  @XmlAttribute(name=ATTR_PREDECESSOR, required=true)
  @XmlIDREF
  public ProcessNode getPredecessor() {
    Collection<ProcessNode> ps = getPredecessors();
    if (ps==null || ps.size()!=1) {
      return null;
    }
    return ps.iterator().next();
  }

  public void setPredecessor(ProcessNode predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  public void setMessage(XmlMessage message) {
    aMessage = message;
  }

  @XmlElement(name=XmlMessage.ELEMENTNAME, required=true)
  public XmlMessage getMessage() {
    return aMessage;
  }

  @Override
  public <T, U extends Task<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance) {
    // TODO handle imports
    T message = pMessageService.createMessage(aMessage);
    if (! pMessageService.sendMessage(message, pInstance)) {
      pInstance.failTask(new MyMessagingException("Failure to send message"));
    }

    return false;
  }

  @Override
  public <T, U extends Task<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance) {
    return false;
  }

  @Override
  public <T, U extends Task<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance) {
    return false;
  }



}
