/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import nl.adaptivity.messaging.DarwinMessenger;
import nl.adaptivity.messaging.MessagingRegistry;


public class PEContextListener implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    MessagingRegistry.registerMessenger(null);
  }

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    DarwinMessenger.register();
  }

}
