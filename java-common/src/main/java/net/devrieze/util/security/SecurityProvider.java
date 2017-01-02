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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.Principal;


public interface SecurityProvider {

  /**
   * Special principal that represents the system.
   */
  RolePrincipal SYSTEMPRINCIPAL= new RolePrincipal() {
    @Override
    public boolean hasRole(@NotNull final String role) {
      return true;
    }

    @Override
    public String getName() {
      return "<SYSTEM PRINCIPAL>";
    }

    @Override
    public String toString() {
      return getName();
    }
  };

  /**
   * Simple marker interface to represent a permission.
   */
  public interface Permission {
    // marker interface
  }

  /**
   * The result of a permission request. A separate type to encourage permanent permissions.
   */
  enum PermissionResult {
    /**
     * Permission has been granted.
     */
    GRANTED,
    /**
     * The user does not have permission for this operation.
     */
    DENIED,
    /**
     * There is no authenticated user.
     */
    UNAUTHENTICATED
  }

  /**
   * Ensure that the user has the given permission.
   *
   * @param permission The permission to verify.
   * @param subject The user to check the permission against.
   * @throws PermissionDeniedException Thrown if the permission is denied.
   * @throws AuthenticationNeededException Thrown if the user has no permission.
   * @return The result. This should always be {@link PermissionResult#GRANTED}.
   */
  PermissionResult ensurePermission(@NotNull Permission permission, @Nullable Principal subject);

  /**
   * Ensure that the user has the given permission in relation to another user.
   *
   * @param permission The permission to verify.
   * @param subject The user to check the permission against.
   * @param objectPrincipal The principal that represents other part of the equation.
   * @throws PermissionDeniedException Thrown if the permission is denied.
   * @throws AuthenticationNeededException Thrown if the user has no permission.
   * @return The result. This should always be {@link PermissionResult#GRANTED}.
   */
  PermissionResult ensurePermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull Principal objectPrincipal);

  /**
   * Ensure that the user has the given permission in relation to a given
   * object.
   *
   * @param permission The permission to verify.
   * @param subject The user to verify the permission for.
   * @param secureObject The object the permission applies to
   * @throws PermissionDeniedException Thrown if the permission is denied.
   * @throws AuthenticationNeededException Thrown if the user has no permission.
   * @return The result. This should always be {@link PermissionResult#GRANTED}.
   */
  PermissionResult ensurePermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull SecureObject secureObject);

  /**
   * Determine whether the user has the permission given
   *
   * @param permission The permission to check
   * @param subject The user
   * @param secureObject The object of the activity
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull SecureObject<?> secureObject);

  PermissionResult getPermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull SecureObject<?> secureObject);

  /**
   * Determine whether the user has the given permission.
   *
   * @param permission The permission to verify.
   * @param subject The user to check the permission against.
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(@NotNull Permission permission, @Nullable Principal subject);

  PermissionResult getPermission(@NotNull Permission permission, @Nullable Principal subject);

  /**
   * Determine whether the user has the given permission in relation to another
   * user.
   *
   * @param permission The permission to verify.
   * @param subject The user to check the permission against.
   * @param objectPrincipal The principal that represents other part of the equation.
   * @return <code>true</code> if the user has the permission,
   *         <code>false</code> if not.
   */
  boolean hasPermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull Principal objectPrincipal);

  PermissionResult getPermission(@NotNull Permission permission, @Nullable Principal subject, @NotNull Principal objectPrincipal);

}
