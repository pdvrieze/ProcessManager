/*
 * Copyright (c) 2018.
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

import java.security.Principal;


/**
 * Security provider that allows everything.
 *
 * @author Paul de Vrieze
 */
public abstract class BaseSecurityProvider implements SecurityProvider {

  @Override
  public final PermissionResult ensurePermission(@NotNull final Permission permission, final Principal subject) {
    return ensurePermission(getPermission(permission, subject), " denied permission to " + subject.getName() + ". to perform "
                                                                + permission + " To allow this set a security provider.");
  }

  private PermissionResult ensurePermission(final PermissionResult permissionResult, final String deniedMessage) {
    switch (permissionResult) {
      case GRANTED:
        return PermissionResult.GRANTED;
      case DENIED:
        throw new PermissionDeniedException(getClass().getSimpleName() + deniedMessage);
      case UNAUTHENTICATED:
        throw new AuthenticationNeededException("For permissions to be available, authentication is needed");
    }
    return permissionResult;
  }

  @Override
  public final PermissionResult ensurePermission(@NotNull final Permission permission, final Principal subject, @NotNull final Principal objectPrincipal) {
    return ensurePermission(getPermission(permission, subject, objectPrincipal)," denied permission to " + subject.getName() + " to perform "
                                          + permission + " in relation to role " + objectPrincipal.getName() + ". To allow this set a security provider.");
  }

  @Override
  public final PermissionResult ensurePermission(@NotNull final Permission permission, final Principal subject, @NotNull final SecureObject<?> secureObject) {
    return ensurePermission(getPermission(permission, subject, secureObject), " denied permission to " + subject.getName() + " to perform "
                                          + permission + " on " + secureObject + ". To allow this set a security provider.");
  }

  @Override
  public final boolean hasPermission(@NotNull final Permission permission, final Principal subject, @NotNull final SecureObject<?> secureObject) {
    return getPermission(permission, subject, secureObject)==PermissionResult.GRANTED;
  }

  @Override
  public final boolean hasPermission(@NotNull final Permission permission, final Principal subject) {
    return getPermission(permission, subject) == PermissionResult.GRANTED;
  }

  @Override
  public final boolean hasPermission(@NotNull final Permission permission, final Principal subject, @NotNull final Principal objectPrincipal) {
    return getPermission(permission, subject, objectPrincipal)==PermissionResult.GRANTED;
  }
}
