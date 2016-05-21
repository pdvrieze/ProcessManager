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

package net.devrieze.util.security;

import java.security.Principal;


public interface SecurityProvider {

  public static final Principal SYSTEMPRINCIPAL= new SimplePrincipal("<SYSTEM PRINCIPAL>");

  public interface Permission {
    // marker interface
  }

  /**
   * Ensure that the user has the given permission.
   *
   * @param pPermission The permission to verify.
   * @param pUser The user to check the permission against.
   * @throws PermissionDeniedException Thrown if the permission is denied.
   */
  void ensurePermission(Permission pPermission, Principal pUser);

  /**
   * Ensure that the user has the given permission in relation to another user.
   *
   * @param pPermission The permission to verify.
   * @param pUser The user to check the permission against.
   * @param pObject The principal that represents other part of
   * @throws PermissionDeniedException Thrown if the permission is denied.
   */
  void ensurePermission(Permission pPermission, Principal pUser, Principal pObject);

  /**
   * Ensure that the user has the given permission in relation to a given
   * object.
   *
   * @param pPermission The permission to verify.
   * @param pUser The user to verify the permission for.
   * @param pObject The object the permission applies to
   */
  void ensurePermission(Permission pPermission, Principal pUser, SecureObject pObject);

  /**
   * Determine whether the user has the permission given
   *
   * @param pPermission The permission to check
   * @param pUser The user
   * @param pObject The object of the activity
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(Permission pPermission, Principal pUser, SecureObject pObject);

  /**
   * Determine whether the user has the given permission.
   *
   * @param pPermission The permission to verify.
   * @param pUser The user to check the permission against.
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(Permission pPermission, Principal pUser);

  /**
   * Determine whether the user has the given permission in relation to another
   * user.
   *
   * @param pPermission The permission to verify.
   * @param pUser The user to check the permission against.
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(Permission pPermission, Principal pUser, Principal pObject);

}
