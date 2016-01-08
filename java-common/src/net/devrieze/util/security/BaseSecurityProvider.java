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


/**
 * Security provider that allows everything.
 *
 * @author Paul de Vrieze
 */
public abstract class BaseSecurityProvider implements SecurityProvider {

  @Override
  public final void ensurePermission(final Permission pPermission, final Principal pUser) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return; }
    if (!hasPermission(pPermission, pUser)) {
      throw new PermissionDeniedException(getClass().getSimpleName() + " denied permission to " + pUser.getName() + ". to perform "
          + pPermission + " To allow this set a security provider.");
    }
  }

  @Override
  public final void ensurePermission(final Permission pPermission, final Principal pUser, final Principal pObject) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return; }
    if (!hasPermission(pPermission, pUser, pObject)) {
      throw new PermissionDeniedException(getClass().getSimpleName() + " denied permission to " + pUser.getName() + " to perform "
          + pPermission + "on " + pObject.getName() + ". To allow this set a security provider.");
    }
  }

  @Override
  public final void ensurePermission(final Permission pPermission, final Principal pUser, final SecureObject pObject) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return; }
    if (!hasPermission(pPermission, pUser, pObject)) {
      throw new PermissionDeniedException(getClass().getSimpleName() + " denied permission to " + pUser.getName() + " to perform "
          + pPermission + "on " + pObject + ". To allow this set a security provider.");
    }
  }

}
