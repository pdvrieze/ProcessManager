package nl.adaptivity.process.engine.jbi;

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessModel;
import nl.adaptivity.process.engine.processModel.XmlProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRef;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.util.HttpMessage;


public class JBIProcessEngine implements Component, Runnable {

  private static final String OP_POST_MESSAGE = "postMessage";
  private static final String OP_START_PROCESS = "startProcess";
  public static final QName SERVICE_QNAME = new QName("http://adaptivity.nl/ProcessEngine/","ProcessEngine");
  private static final String LOGSUFFIX = null;
  private ComponentContext aContext;
  private ServiceEndpoint aEndPoint;
  private Thread aThread;
  private boolean aKeepRunning = true;
  private Logger aLogger;
  private ProcessEngine aProcessEngine;

  @Override
  public ComponentLifeCycle getLifeCycle() {
    return new PELifeCycle(this);
  }

  @Override
  public Document getServiceDescription(ServiceEndpoint pEndpoint) {
    logError("getServiceDescription");
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document result = builder.parse(getClass().getClassLoader().getResourceAsStream("ProcessEngine-jbi.wsdl"));
      return result;
    } catch (ParserConfigurationException e) {
      logError(e);
    } catch (SAXException e) {
      logError(e);
    } catch (IOException e) {
      logError(e);
    }
    return null;
  }

  private void logError(Throwable pE) {
    Logger logger = getLogger();
    if (logger==null) {
      pE.printStackTrace();
      return;
    }

    
    StackTraceElement[] stackTrace = pE.getStackTrace();
    
    String className = null;
    String methodName = null;
    if (stackTrace.length>=1) {
      className=stackTrace[0].getClassName();
      methodName=stackTrace[0].getMethodName();
    }
    logger.throwing(className, methodName, pE);
    
    CharArrayWriter writer = new CharArrayWriter();
    pE.printStackTrace(new PrintWriter(writer));
    logger.warning(writer.toString());

  }

  private void logError(String pMessage) {
    Logger logger = getLogger();
    if (logger==null) {
      System.err.print(pMessage);
      return;
    }
    
    logger.log(Level.WARNING, pMessage);
  }
  
  private void logEntry() {
    Logger logger = getLogger();
    if (logger==null) {
      System.err.print("Failing to get logger");
      return;
    }

    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement caller = new Exception().getStackTrace()[1];
      logger.entering(caller.getClassName(), caller.getMethodName());
    }
  }

  Logger getLogger() {
    if (aLogger != null) {
      return aLogger;
    }
    try {
      aLogger = aContext.getLogger(LOGSUFFIX, null);
      aLogger.setLevel(Level.ALL);
      return aLogger;
    } catch (MissingResourceException e1) {
      e1.printStackTrace();
    } catch (JBIException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ServiceUnitManager getServiceUnitManager() {
    // TODO accept process models as service units?
    return null;
  }

  @Override
  public boolean isExchangeWithConsumerOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    logEntry();
    if (pEndpoint.getEndpointName()=="endpoint" && pEndpoint.getServiceName().equals(SERVICE_QNAME)) {
      final String operationName = pExchange.getOperation().getLocalPart();
      return OP_START_PROCESS.equals(operationName) || 
             OP_POST_MESSAGE.equals(operationName) ||
             "GET".equals(operationName) ||
             "POST".equals(operationName) ||
             "PUT".equals(operationName) ||
             "DELETE".equals(operationName);
      
    } else {
      return false;
    }
  }

  @Override
  public boolean isExchangeWithProviderOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    logEntry();
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public ServiceEndpoint resolveEndpointReference(DocumentFragment pEpr) {
    logError("resolveEndpointReference");
    return null;
  }

  public void setContext(ComponentContext pContext) {
    aContext = pContext;
  }
  
  ComponentContext getContext() {
    return aContext;
  }

  void startEndPoint() throws JBIException {
    aThread = new Thread(this);
    aThread.start();
    aEndPoint = aContext.activateEndpoint(SERVICE_QNAME, "endpoint");
  }
  
  void activateEndPoint() {
    logError("activateEndPoint");
  }
  
  public void run() {
    Logger logger = getLogger();

    DeliveryChannel deliveryChannel;
    try {
      deliveryChannel = aContext.getDeliveryChannel();
    } catch (MessagingException e) {
      logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
      return;
    }
    
    while (aKeepRunning) {

      try {
        MessageExchange ex = deliveryChannel.accept(1000);
        if (ex!=null) {
          processMessage(deliveryChannel, ex);
        }
      } catch (MessagingException e) {
        if (logger != null) {
          logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
        } else {
          e.printStackTrace();
        }
      }
    }
  }

  private void processMessage(DeliveryChannel pDeliveryChannel, MessageExchange ex) throws MessagingException {
    logEntry();
    try {
      if (ex.getStatus()==ExchangeStatus.ACTIVE) {
        final String localPart = ex.getOperation().getLocalPart();
        if (localPart.equals(OP_POST_MESSAGE)) {
          initPostMessage(pDeliveryChannel, ex);
        } else if (localPart.equals(OP_START_PROCESS)) {
          initStartProcess(pDeliveryChannel, (InOut) ex);
        } else if ("GET".equals(localPart)) {
          processGet(pDeliveryChannel, (InOut) ex);
        } else if ("POST".equals(localPart)) {
          processPost(pDeliveryChannel, (InOut) ex);
        } else if ("PUT".equals(localPart)) {
          processPut(pDeliveryChannel, (InOut) ex);
        } else if ("DELETE".equals(localPart)) {
          processDelete(pDeliveryChannel, (InOut) ex);
        }
      }
    } catch (Exception e) {
      logError(e);
      ex.setError(e);
      pDeliveryChannel.send(ex);
    }
  }

  private void processGet(DeliveryChannel pDeliveryChannel, InOut pEx) throws Exception {
    HttpMessage message = JAXB.unmarshal(pEx.getInMessage().getContent(), HttpMessage.class);
    String pathInfo = message.getPathInfo();
    final Source result;
    if ("/processModels".equals(pathInfo)) {
      result = getProcessModels();
    } else {
      throw new FileNotFoundException();
    }
    NormalizedMessage reply = pEx.createMessage();
    reply.setContent(result);
    pEx.setMessage(reply, "out");
    pDeliveryChannel.send(pEx);
  }

  private Source getProcessModels() throws JAXBException {
    Iterable<ProcessModel> processModels = aProcessEngine.getProcessModels();
    ProcessModelRefs list = new ProcessModelRefs();
    for (ProcessModel pm: processModels) {
      list.add(pm.getRef());
    }
    return new JAXBSource(JAXBContext.newInstance(ProcessModelRefs.class), list);
  }

  private void processPost(DeliveryChannel pDeliveryChannel, InOut pEx) {
    HttpMessage message = JAXB.unmarshal(pEx.getInMessage().getContent(), HttpMessage.class);
    
    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  private void processDelete(DeliveryChannel pDeliveryChannel, InOut pEx) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private void processPut(DeliveryChannel pDeliveryChannel, InOut pEx) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private void initStartProcess(DeliveryChannel pDeliveryChannel, InOut pEx) {
    logEntry();
    
    try {
      NormalizedMessage msg = pEx.getInMessage();
      Source content = msg.getContent();
      ProcessModel processModel = JAXB.unmarshal(content, XmlProcessModel.class).toProcessModel();
      
      HProcessInstance result = aProcessEngine.startProcess(processModel, null);
      NormalizedMessage reply = pEx.createMessage();
      reply.setContent(new JAXBSource(JAXBContext.newInstance(HProcessInstance.class), result));
      pEx.setOutMessage(reply);
      pDeliveryChannel.send(pEx);
    } catch (Exception e) {
      logError(e);
      try {
        pEx.setError(e);
        pDeliveryChannel.send(pEx);
      } catch (MessagingException e2) {
        logError(e2);
      }
    }
  }

  private void initPostMessage(DeliveryChannel pDeliveryChannel, MessageExchange pEx) {
    logEntry();
    try {
      pEx.setStatus(ExchangeStatus.DONE);
      pDeliveryChannel.send(pEx);
    } catch (MessagingException e) {
      logError(e);
    }

  }

  public void stop() throws JBIException {
    aKeepRunning = false;
    aContext.deactivateEndpoint(aEndPoint);
    aThread.interrupt();
  }

  public void startEngine() {
    aProcessEngine = new ProcessEngine();
  }

  public ProcessEngine getProcessEngine() {
    return aProcessEngine;
  }
  
}
