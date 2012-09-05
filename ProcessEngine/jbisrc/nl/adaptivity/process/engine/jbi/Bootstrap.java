package nl.adaptivity.process.engine.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.InstallationContext;
import javax.management.ObjectName;


public class Bootstrap implements javax.jbi.component.Bootstrap{

  @Override
  public void cleanUp() throws JBIException {
  }

  @Override
  public ObjectName getExtensionMBeanName() {
    return null;
  }

  @Override
  public void init(InstallationContext pArg0) throws JBIException {
  }

  @Override
  public void onInstall() throws JBIException {
  }

  @Override
  public void onUninstall() throws JBIException {
  }

}
