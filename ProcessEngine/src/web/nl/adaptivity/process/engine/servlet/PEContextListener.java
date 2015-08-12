package nl.adaptivity.process.engine.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import nl.adaptivity.messaging.DarwinMessenger;
import nl.adaptivity.messaging.MessagingRegistry;


public class PEContextListener implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent pSce) {
    MessagingRegistry.registerMessenger(null);
  }

  @Override
  public void contextInitialized(final ServletContextEvent pSce) {
    DarwinMessenger.register();
  }

}
