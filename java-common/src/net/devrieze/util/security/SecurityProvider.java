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
