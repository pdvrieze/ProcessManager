package nl.adaptivity.process.engine.jbi;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.management.ObjectName;


public class PELifeCycle implements ComponentLifeCycle {

  private final JBIProcessEngine aProcessEngine;

  public PELifeCycle(JBIProcessEngine processEngine) {
    aProcessEngine = processEngine;
  }

  @Override
  public ObjectName getExtensionMBeanName() {
    return null;
  }

  @Override
  public void init(ComponentContext context) throws JBIException {
    aProcessEngine.setContext(context);
    Logger logger = context.getLogger(null, null);
    logger.info("ProcessEngine initialized");
  }

  @Override
  public void shutDown() throws JBIException {
    // TODO Auto-generated method stub
  }

  @Override
  public void start() throws JBIException {
    aProcessEngine.getLogger().info("ProcessEngine starting");
    aProcessEngine.startEngine();
    aProcessEngine.startEndPoint();
  }

  @Override
  public void stop() throws JBIException {
    aProcessEngine.getLogger().info("ProcessEngine stopping");
    aProcessEngine.stop();
  }

}
