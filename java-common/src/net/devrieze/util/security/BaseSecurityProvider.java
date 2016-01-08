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
