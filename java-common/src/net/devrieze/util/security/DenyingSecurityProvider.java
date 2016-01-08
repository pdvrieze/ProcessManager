package net.devrieze.util.security;

import java.security.Principal;


public final class DenyingSecurityProvider extends BaseSecurityProvider {

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser, final SecureObject pObject) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return true; }
    return false;
  }

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return true; }
    return false;
  }

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser, final Principal pObject) {
    if (pUser==SecurityProvider.SYSTEMPRINCIPAL) { return true; }
    return false;
  }

}
