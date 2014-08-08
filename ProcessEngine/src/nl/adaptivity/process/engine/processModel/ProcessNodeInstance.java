package nl.adaptivity.process.engine.processModel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecureObject;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.MessagingFormatException;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.exec.XmlProcessNodeInstance.Body;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.util.Constants;


public class ProcessNodeInstance implements IProcessNodeInstance<ProcessNodeInstance>, SecureObject {

  private final ProcessNodeImpl aNode;

  private List<ProcessData> aResult = new ArrayList<>();

  private Collection<Handle<? extends ProcessNodeInstance>> aPredecessors;

  private TaskState aState = TaskState.Pending;

  private long aHandle = -1;

  private final ProcessInstance aProcessInstance;

  private Throwable aFailureCause;

  public ProcessNodeInstance(final ProcessNodeImpl pNode, final Handle<? extends ProcessNodeInstance> pPredecessor, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    if (pPredecessor==null) {
      if (pNode instanceof StartNode) {
        aPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      aPredecessors = Collections.<Handle<? extends ProcessNodeInstance>>singletonList(pPredecessor);
    }
    aProcessInstance = pProcessInstance;
    if ((pPredecessor == null) && !(pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ProcessNodeImpl pNode, final Collection<? extends Handle<? extends ProcessNodeInstance>> pPredecessors, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    aPredecessors = new ArrayList<>(pPredecessors);
    aProcessInstance = pProcessInstance;
    if (((aPredecessors == null) || (aPredecessors.size()==0)) && !(pNode instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(ProcessNodeImpl pNode, ProcessInstance pProcessInstance, TaskState pState) {
    aNode = pNode;
    aProcessInstance = pProcessInstance;
    aState = pState;
  }

  public ProcessNodeImpl getNode() {
    return aNode;
  }

  public List<ProcessData> getResult() {
    return aResult;
  }

  public Collection<Handle<? extends ProcessNodeInstance>> getDirectPredecessors() {
    return aPredecessors;
  }

  public void setDirectPredecessors(Collection<Handle<? extends ProcessNodeInstance>> pPredecessors) {
    if (pPredecessors==null || pPredecessors.isEmpty()) {
      aPredecessors = Collections.emptyList();
    } else {
      aPredecessors = new ArrayList<>(pPredecessors);
    }
  }

  public Throwable getFailureCause() {
    return aFailureCause;
  }

  @Override
  public TaskState getState() {
    return aState;
  }

  @Override
  public void setState(DBTransaction pTransaction, final TaskState pNewState) throws SQLException {
    if ((aState != null) && (aState.compareTo(pNewState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + aState + " new:" + pNewState);
    }
    aState = pNewState;
    aProcessInstance.getEngine().updateStorage(pTransaction, this);
  }

  @Override
  public void setHandle(final long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public <U> boolean provideTask(DBTransaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    try {
      final boolean result = aNode.provideTask(pTransaction, pMessageService, this);
      setState(pTransaction, TaskState.Sent);
      return result;
    } catch (RuntimeException e) {
      failTask(pTransaction, e);
      throw e;
    }
  }

  @Override
  public <U> boolean takeTask(DBTransaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    final boolean result = aNode.takeTask(pMessageService, this);
    setState(pTransaction, TaskState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(DBTransaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    final boolean startTask = aNode.startTask(pMessageService, this);
    setState(pTransaction, TaskState.Started);
    return startTask;
  }

  @Override
  public void finishTask(DBTransaction pTransaction, final Node pResultPayload) throws SQLException {
    aResult.add(new ProcessData(null, pResultPayload==null ? null : pResultPayload.toString()));
    setState(pTransaction, TaskState.Complete);
  }

  @Override
  public void cancelTask(DBTransaction pTransaction) throws SQLException {
    setState(pTransaction, TaskState.Cancelled);
  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName() + " (" + aState + ")";
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  @Override
  public void failTask(DBTransaction pTransaction, final Throwable pCause) throws SQLException {
    aFailureCause = pCause;
    setState(pTransaction, TaskState.Failed);
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param pResults the new results.
   */
  void setResult(List<ProcessData> pResults) {
    aResult.clear();
    aResult.addAll(pResults);
  }

  public void instantiateXmlPlaceholders(Source source, final Result result) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newInstance();
    final XMLOutputFactory xof = XMLOutputFactory.newInstance();
    final XMLEventReader xer = xif.createXMLEventReader(source);
    final XMLEventWriter xew = xof.createXMLEventWriter(result);

    while (xer.hasNext()) {
      final XMLEvent event = xer.nextEvent();
      if (event.isStartElement()) {
        final StartElement se = event.asStartElement();
        final QName eName = se.getName();
        if (Constants.MODIFY_NS.toString().equals(eName.getNamespaceURI())) {
          @SuppressWarnings("unchecked")
          final Iterator<Attribute> attributes = se.getAttributes();
          if (eName.getLocalPart().equals("attribute")) {
            writeAttribute(this, xer, attributes, xew);
          } else if (eName.getLocalPart().equals("element")) {
            writeElement(this, xer, attributes, xew);
          } else {
            throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported activity modifier");
          }
        } else {
          xew.add(se);
        }
      } else {
        if (event.isCharacters()) {
          final Characters c = event.asCharacters();
          final String charData = c.getData();
          int i;
          for (i = 0; i < charData.length(); ++i) {
            if (!Character.isWhitespace(charData.charAt(i))) {
              break;
            }
          }
          if (i == charData.length()) {
            continue; // ignore it, and go to next event
          }
        }

        if (event instanceof Namespace) {

          final Namespace ns = (Namespace) event;
          if (!ns.getNamespaceURI().equals(Constants.MODIFY_NS)) {
            xew.add(event);
          }
        } else {
          try {
            xew.add(event);
          } catch (final IllegalStateException e) {
            final StringBuilder errorMessage = new StringBuilder("Error adding event: ");
            errorMessage.append(event.toString()).append(' ');
            if (event.isStartElement()) {
              errorMessage.append('<').append(event.asStartElement().getName()).append('>');
            } else if (event.isEndElement()) {
              errorMessage.append("</").append(event.asEndElement().getName()).append('>');
            }
            getLogger().log(Level.WARNING, errorMessage.toString(), e);

            throw e;
          }
        }
      }
    }
  }

  private static Logger getLogger() {
    Logger logger = Logger.getLogger(nl.adaptivity.process.engine.processModel.ProcessNodeInstance.class.getName());
    return logger;
  }

  private static void writeElement(ProcessNodeInstance pNodeInstance, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
    String valueName = null;
    {
      while (pAttributes.hasNext()) {
        final Attribute attr = pAttributes.next();
        final String attrName = attr.getName().getLocalPart();
        if ("value".equals(attrName)) {
          valueName = attr.getValue();
        }
      }
    }
    {
      final XMLEvent ev = in.nextEvent();

      while (!ev.isEndElement()) {
        if (ev.isStartElement()) {
          throw new MessagingFormatException("Violation of schema");
        }
        if (ev.isAttribute()) {
          final Attribute attr = (Attribute) ev;
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          }
        }
      }
    }
    if (valueName != null) {
      final XMLEventFactory xef = XMLEventFactory.newInstance();

      if ("handle".equals(valueName)) {
        out.add(xef.createCharacters(Long.toString(pNodeInstance.getHandle())));
      } else if ("endpoint".equals(valueName)) {
        final QName qname1 = new QName(Constants.MY_JBI_NS, "endpointDescriptor", "");
        final List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", Constants.MY_JBI_NS));
        out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

        {
          EndpointDescriptor localEndpoint = pNodeInstance.getProcessInstance().getEngine().getLocalEndpoint();
          out.add(xef.createAttribute("serviceNS", localEndpoint.getServiceName().getNamespaceURI()));
          out.add(xef.createAttribute("serviceLocalName", localEndpoint.getServiceName().getLocalPart()));
          out.add(xef.createAttribute("endpointName", localEndpoint.getEndpointName()));
          out.add(xef.createAttribute("endpointLocation", localEndpoint.getEndpointLocation().toString()));
        }

        out.add(xef.createEndElement(qname1, namespaces.iterator()));
      }
    } else {
      throw new MessagingFormatException("Missing parameter name");
    }

  }

  private static void writeAttribute(ProcessNodeInstance pNodeInstance, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
    String valueName = null;
    String paramName = null;
    {
      while (pAttributes.hasNext()) {
        final Attribute attr = pAttributes.next();
        final String attrName = attr.getName().getLocalPart();
        if ("value".equals(attrName)) {
          valueName = attr.getValue();
        } else if ("name".equals(attrName)) {
          paramName = attr.getValue();
        }
      }
    }
    {
      final XMLEvent ev = in.nextEvent();

      while (!ev.isEndElement()) {
        if (ev.isStartElement()) {
          throw new MessagingFormatException("Violation of schema");
        }
        if (ev.isAttribute()) {
          final Attribute attr = (Attribute) ev;
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          } else if ("name".equals(attrName)) {
            paramName = attr.getValue();
          }
        }
      }
    }
    if (valueName != null) {


      final XMLEventFactory xef = XMLEventFactory.newInstance();

      if ("handle".equals(valueName)) {
        Attribute attr;
        if (paramName != null) {
          attr = xef.createAttribute(paramName, Long.toString(pNodeInstance.getHandle()));
        } else {
          attr = xef.createAttribute("handle", Long.toString(pNodeInstance.getHandle()));
        }
        out.add(attr);
      } else if ("owner".equals(valueName)) {
        Attribute attr;
        if (paramName != null) {
          attr = xef.createAttribute(paramName, pNodeInstance.getProcessInstance().getOwner().getName());
        } else {
          attr = xef.createAttribute("owner", pNodeInstance.getProcessInstance().getOwner().getName());
        }
        out.add(attr);
      } else if ("instancehandle".equals(valueName)) {
        Attribute attr;
        if (paramName != null) {
          attr = xef.createAttribute(paramName, pNodeInstance.getProcessInstance().getOwner().getName());
        } else {
          attr = xef.createAttribute("instancehandle", pNodeInstance.getProcessInstance().getOwner().getName());
        }
        out.add(attr);
      }


    } else {
      throw new MessagingFormatException("Missing parameter name");
    }

  }

  public XmlProcessNodeInstance toXmlNode() {
    XmlProcessNodeInstance result = new XmlProcessNodeInstance();
    result.setState(aState);
    result.setHandle(aHandle);

    if (aNode instanceof Activity<?>) {
      Activity<?> act = (Activity<?>) aNode;
      IXmlMessage message = act.getMessage();
      Source source = message.getBodySource();

      final DOMResult transformResult = new DOMResult();
      try {
        instantiateXmlPlaceholders(source, transformResult);
        result.setBody(new Body(transformResult.getNode()));
      } catch (XMLStreamException e) {
        getLogger().log(Level.WARNING, "Error processing body", e);
      }
    }

    result.setProcessinstance(aProcessInstance.getHandle());

    List<Long> predecessors = result.getPredecessors();
    for(Handle<? extends ProcessNodeInstance> h: aPredecessors) {
      predecessors.add(Long.valueOf(h.getHandle()));
    }
    return result;
  }

}
