package nl.adaptivity.process.engine.jbi;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;


public class JBIProcessEngine implements Component, Runnable {

  private static final String OP_POST_MESSAGE = "postMessage";
  private static final String OP_START_PROCESS = "startProcess";
  public static final QName SERVICE_QNAME = new QName("http://adaptivity.nl/ProcessEngine/","ProcessEngine");
  private ComponentContext aContext;
  private ServiceEndpoint aEndPoint;
  private Thread aThread;
  private boolean aKeepRunning = true;

  @Override
  public ComponentLifeCycle getLifeCycle() {
    return new PELifeCycle(this);
  }

  @Override
  public Document getServiceDescription(ServiceEndpoint pEndpoint) {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document result = builder.parse(getClass().getClassLoader().getResourceAsStream("ProcessEngine-jbi.wsdl"));
      return result;
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    } catch (SAXException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public ServiceUnitManager getServiceUnitManager() {
    // TODO accept process models as service units?
    return null;
  }

  @Override
  public boolean isExchangeWithConsumerOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    if (pEndpoint.getEndpointName()=="endpoint" && pEndpoint.getServiceName().equals(SERVICE_QNAME)) {
      final String operationName = pExchange.getOperation().getLocalPart();
      return operationName.equals(OP_START_PROCESS) || operationName.equals(OP_POST_MESSAGE);  
      
    } else {
      return false;
    }
  }

  @Override
  public boolean isExchangeWithProviderOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public ServiceEndpoint resolveEndpointReference(DocumentFragment pEpr) {
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
//    aContext.registerExternalEndpoint(aEndPoint);
  }
  
  void activateEndPoint() {
  }
  
  public void run() {
    Logger logger = null;
    try {
      logger = aContext.getLogger(null, null);
    } catch (MissingResourceException e1) {
      e1.printStackTrace();
    } catch (JBIException e) {
      e.printStackTrace();
    }

    DeliveryChannel deliveryChannel;
    try {
      deliveryChannel = aContext.getDeliveryChannel();
    } catch (MessagingException e) {
      if (logger != null) {
        logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
      } else {
        e.printStackTrace();
      }
      return;
    }
    
    while (aKeepRunning) {

      try {
        MessageExchange ex = deliveryChannel.accept(1000);
        if (ex!=null) {
          final String localPart = ex.getOperation().getLocalPart();
          if (localPart.equals(OP_POST_MESSAGE)) {
            initPostMessage(logger, ex);
          } else if (localPart.equals(OP_START_PROCESS)) {
            initStartProcess(logger, ex);
          }
        }
      } catch (MessagingException e) {
        if (logger != null) {
          logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
        } else {
          e.printStackTrace();
        }
      }
      try {
        
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void initStartProcess(Logger pLogger, MessageExchange pEx) {
    pLogger.entering(JBIProcessEngine.class.getName(), "initStartProcess");
    
    try {
      NormalizedMessage msg = pEx.getMessage("msg");
      Source content = msg.getContent();
      
      
    } catch (Exception e) {
      pLogger.throwing(JBIProcessEngine.class.getName(), "initStartProcess", e);
      try {
        Fault f = pEx.createFault();
        pEx.setFault(f);
      } catch (MessagingException e2) {
        pLogger.throwing(JBIProcessEngine.class.getName(), "initStartProcess", e2);
      }
//      pEx.setError(e);
    }
  }

  private void initPostMessage(Logger pLogger, MessageExchange pEx) {
    pLogger.entering(JBIProcessEngine.class.getName(), "initPostMessage");

  }

  public void stop() {
    aKeepRunning = false;
    aThread.interrupt();
  }
  
}
