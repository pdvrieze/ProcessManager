package net.devrieze.util.security;

import java.security.Principal;


/**
 * Security provider that allows everything.
 * 
 * @author Paul de Vrieze
 */
public class PermissiveProvider extends BaseSecurityProvider {

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser, final SecureObject pObject) {
    return true;
  }

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser) {
    return true;
  }

  @Override
  public boolean hasPermission(final Permission pPermission, final Principal pUser, final Principal pObject) {
    return true;
  }

}
