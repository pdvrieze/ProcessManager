package nl.adaptivity.process.engine.jbi;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.management.ObjectName;


public class PELifeCycle implements ComponentLifeCycle {

  private final JBIProcessEngine mProcessEngine;

  public PELifeCycle(JBIProcessEngine processEngine) {
    mProcessEngine = processEngine;
  }

  @Override
  public ObjectName getExtensionMBeanName() {
    return null;
  }

  @Override
  public void init(ComponentContext context) throws JBIException {
    mProcessEngine.setContext(context);
    Logger logger = context.getLogger(null, null);
    logger.info("ProcessEngine initialized");
  }

  @Override
  public void shutDown() throws JBIException {
    // TODO Auto-generated method stub
  }

  @Override
  public void start() throws JBIException {
    mProcessEngine.getLogger().info("ProcessEngine starting");
    mProcessEngine.startEngine();
    mProcessEngine.startEndPoint();
  }

  @Override
  public void stop() throws JBIException {
    mProcessEngine.getLogger().info("ProcessEngine stopping");
    mProcessEngine.stop();
  }

}
