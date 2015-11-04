package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


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
@XmlDeserializer(ActivityImpl.Factory.class)
@XmlRootElement(name = ActivityImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ActivityImpl.ELEMENTLOCALNAME + "Type", propOrder = { "defines", "results", "condition", XmlMessage.ELEMENTLOCALNAME})
public class ActivityImpl extends ProcessNodeImpl implements Activity<ProcessNodeImpl> {

  public static class Factory implements XmlDeserializerFactory {

    @Override
    public ActivityImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return ActivityImpl.deserialize(null, in);
    }
  }

  private static final long serialVersionUID = 282944120294737322L;

  /** The name of the XML element. */
  public static final String ELEMENTLOCALNAME = "activity";

  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String aName;

  private ConditionImpl aCondition;

  private List<XmlResultType> aResults = new ArrayList<>();

  private List<XmlDefineType> aDefines = new ArrayList<>();

  private XmlMessage aMessage;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param pPredecessor The process node that starts immediately precedes this
   *          activity.
   */
  public ActivityImpl(ProcessModelImpl pOwnerModel, final ProcessNodeImpl pPredecessor) {
    super(pOwnerModel, Collections.singletonList(pPredecessor));
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public ActivityImpl(ProcessModelImpl pOwnerModel) {super(pOwnerModel);}

  public static ActivityImpl deserialize(ProcessModelImpl pOwnerModel, final XMLStreamReader in) throws XMLStreamException {
    XmlUtil.skipPreamble(in);
    assert XmlUtil.isElement(in, ELEMENTNAME);
    ActivityImpl result = new ActivityImpl(pOwnerModel);
    for(int i=in.getAttributeCount()-1; i>=0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    int event = -1;
    loop:while (in.hasNext()&& event!= XMLStreamConstants.END_ELEMENT) {
      switch((event = in.next())) {
        case XMLStreamConstants.START_ELEMENT:
          if (Engine.NAMESPACE.equals(in.getNamespaceURI())) {
            switch (in.getLocalName()) {
              case XmlDefineType.ELEMENTNAME:
                result.aDefines.add(XmlDefineType.deserialize(in));
                continue loop;
              case XmlResultType.ELEMENTLOCALNAME:
                result.aResults.add(XmlResultType.deserialize(in));
                continue loop;
              case ConditionImpl.ELEMENTLOCALNAME:
                result.aCondition = ConditionImpl.deserialize(in);
                continue loop;
              case XmlMessage.ELEMENTLOCALNAME:
                result.aMessage = XmlMessage.deserialize(in);
                continue loop;
            }
          }
          break;
        default:
          XmlUtil.unhandledEvent(in);
      }
    }
    return result;
  }

  @Override
  protected boolean deserializeAttribute(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    switch (pAttributeLocalName) {
      case ATTR_PREDECESSOR: setPredecessor(new Identifier(pAttributeValue)); return true;
      case "name": setName(pAttributeValue);
    }
    return super.deserializeAttribute(pAttributeNamespace, pAttributeLocalName, pAttributeValue);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeAttributes(pOut);
    pOut.writeAttribute(ATTR_PREDECESSOR, getPredecessor().getId());
    XmlUtil.writeAttribute(pOut, "name", getName());
  }

  protected void serializeChildren(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeChildren(pOut);
    XmlUtil.writeChildren(pOut, getDefines());
    XmlUtil.writeChildren(pOut, getResults());
    XmlUtil.writeChild(pOut, aCondition);
    if (aCondition!=null) { aCondition.serialize(pOut); }

    {
      XmlMessage m = getMessage();
      if (m!=null) { m.serialize(pOut); }
    }
  }

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
  @XmlElement(name = ConditionImpl.ELEMENTLOCALNAME)
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
  @XmlElement(name = XmlResultType.ELEMENTLOCALNAME)
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
    aResults = pImports==null ? new ArrayList<XmlResultType>(0) : toExportableResults(pImports);
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
    aDefines = pExports==null ? new ArrayList<XmlDefineType>(0) : toExportableDefines(pExports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getPredecessor()
   */
  @Override
  @XmlAttribute(name = ATTR_PREDECESSOR, required = true)
  @XmlIDREF
  public Identifiable getPredecessor() {
    final Collection<? extends Identifiable> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setPredecessor(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public void setPredecessor(final Identifiable predecessor) {
    setPredecessors(Collections.singleton(predecessor));
  }



  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getMessage()
   */
  @Override
  @XmlElement(name = XmlMessage.ELEMENTLOCALNAME, required = true)
  public XmlMessage getMessage() {
    return aMessage;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setMessage(nl.adaptivity.process.processModel.XmlMessage)
   */
  @Override
  public void setMessage(final IXmlMessage message) {
    try {
      aMessage = XmlMessage.get(message);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
  }

  public void setMessage(final XmlMessage message) {
    try {
      aMessage = XmlMessage.get(message);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
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
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction pTransaction, final IMessageService<T, U> pMessageService, final U pInstance) throws SQLException {
    // TODO handle imports
    final T message = pMessageService.createMessage(aMessage);
    try {
      if (!pMessageService.sendMessage(pTransaction, message, pInstance)) {
        pInstance.failTaskCreation(pTransaction, new MessagingException("Failure to send message"));
      }
    } catch (RuntimeException e) {
      pInstance.failTaskCreation(pTransaction, e);
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
