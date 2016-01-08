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

package uk.ac.bournemouth.darwin.catalina.realm;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;


/**
 * Base class for darwin principals. This will allo
 *
 * @author Paul de Vrieze
 */
public abstract class DarwinBasePrincipal extends GenericPrincipal implements DarwinPrincipal {

  /**
   * Principals will refresh against the database after 5 minutes. This means
   * that database changes will be effective after a maximum of 5 minutes.
   */
  private static final long MAX_CACHE = 300000; // Ten minute cache

  /**
   * Attribute to record when we last checked the database. By default very far
   * in the past so that we will certainly need to check.
   */
  private final long mLastChecked = Long.MIN_VALUE;


  /**
   * Create a new {@link DarwinBasePrincipal}
   * @param realm The realm the principal is recorded against.
   * @param name The name of the principal.
   */
  public DarwinBasePrincipal(final Realm realm, final String name) {
    super(realm, name, null);
  }

  /**
   * This is used by subclasses to determine whether the user data needs to be reretrieved from the database.
   * @return <code>true</code> if the database needs to be consulted.
   */
  protected boolean needsRefresh() {
    final long now = System.currentTimeMillis();
    return now < (mLastChecked + MAX_CACHE);
  }

}
