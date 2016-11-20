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

import org.jetbrains.annotations.NotNull;

import java.security.Principal;


public final class DenyingSecurityProvider extends BaseSecurityProvider {

  @Override
  public PermissionResult getPermission(@NotNull final Permission permission, final Principal subject, @NotNull final SecureObject<?> secureObject) {
    return subject==SYSTEMPRINCIPAL ? PermissionResult.GRANTED : subject==null ? PermissionResult.UNAUTHENTICATED : PermissionResult.DENIED;
  }

  @Override
  public PermissionResult getPermission(@NotNull final Permission permission, final Principal subject) {
    return subject==SYSTEMPRINCIPAL ? PermissionResult.GRANTED : subject==null ? PermissionResult.UNAUTHENTICATED : PermissionResult.DENIED;
  }

  @Override
  public PermissionResult getPermission(@NotNull final Permission permission, final Principal subject, @NotNull final Principal objectPrincipal) {
    return subject==SYSTEMPRINCIPAL ? PermissionResult.GRANTED : subject==null ? PermissionResult.UNAUTHENTICATED : PermissionResult.DENIED;
  }

}
