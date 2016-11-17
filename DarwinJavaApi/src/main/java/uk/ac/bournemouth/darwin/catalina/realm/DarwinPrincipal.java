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

import net.devrieze.util.StringCache;
import net.devrieze.util.security.RolePrincipal;

import java.security.Principal;
import java.util.Set;


/**
 * A principal for the darwin system.
 *
 * @author Paul de Vrieze
 */
public interface DarwinPrincipal extends RolePrincipal {

  /**
   * Determine whether the user has overall admin rights.
   *
   * @return <code>true</code> if the user is admin, <code>false</code> if not.
   */
  boolean isAdmin();

  /**
   * Determine whether the user has the given role. Note that depending on the
   * implementation some caching can occur and therefore the answer might not
   * reflect very recent changes in the database.
   *
   * @param roleName The role to verify.
   * @return <code>true</code> if the user has the role, <code>false</code> if
   *         not.
   */
  boolean hasRole(String roleName);

  /**
   * Get the email address of the user.
   *
   * @return The email address, or <code>null</code> if not known.
   */
  CharSequence getEmail();


  /**
   * Get a set of the roles of the user. Note that changes to this will not be
   * allowed, and in any case would not be reflected in the database.
   *
   * @return The set of roles.
   */
  Set<? extends CharSequence> getRolesSet();

  /**
   * Create a copy principal that uses the given stringcache to reuse strings.
   * This should save memory as well as make comparison faster.
   *
   * @param stringCache The stringcache to use for reusing strings.
   * @return A principal using the de-duped strings. Note that this may, but
   *         does not ahve to return the original princpal.
   */
  Principal cacheStrings(StringCache stringCache);


}
