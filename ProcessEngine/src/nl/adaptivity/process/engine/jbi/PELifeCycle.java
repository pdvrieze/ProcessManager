package nl.adaptivity.process.engine.jbi;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.management.ObjectName;


public class PELifeCycle implements ComponentLifeCycle {

  private final JBIProcessEngine aProcessEngine;

  public PELifeCycle(JBIProcessEngine pProcessEngine) {
    aProcessEngine = pProcessEngine;
  }

  @Override
  public ObjectName getExtensionMBeanName() {
    return null;
  }

  @Override
  public void init(ComponentContext pContext) throws JBIException {
    aProcessEngine.setContext(pContext);
    Logger logger = pContext.getLogger(null, null);
    logger.info("ProcessEngine inited");
  }

  @Override
  public void shutDown() throws JBIException {
    // TODO Auto-generated method stub
  }

  @Override
  public void start() throws JBIException {
    aProcessEngine.startEngine();
    aProcessEngine.startEndPoint();
    // TODO Auto-generated method stub
  }

  @Override
  public void stop() throws JBIException {
    aProcessEngine.stop();
  }

}
