package net.devrieze.util;

import java.security.Principal;

import uk.ac.bournemouth.darwin.auth.DarwinPermission;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal;


public class Users {
  
  private Users() {/* Just block construction */}

  /**
   * Determine whether the owner is allowed to assign ownership to the given
   * recipient.
   * @todo much more complex logic
   * @param pOwner Owner of the object
   * @param pRecipient Targetted recipient.
   * @return Whether assignment is allowed.
   */
  public static boolean canAssignOwnershipTo(Principal pOwner, String pRecipient) {
    if (pOwner instanceof DarwinPrincipal) {
      DarwinPrincipal dOwner = (DarwinPrincipal) pOwner;
      if (dOwner.isAdmin() && isUser(pRecipient)) {
        return true;
      }
    }
    String ownerName = pOwner.getName();
    return ownerName.equals(pRecipient);
  }

  private static boolean isUser(String pRecipient) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  

  /**
   * Ensure that the given user has the needed permission. 
   * @param pPermission The permission to verify
   * @param pUser The user to verify the permission on.
   */
  public static void ensurePermission(DarwinPermission pPermission, Principal pUser) {
//    DarwinPrincipal user = DarwinAuthenticator.asDarwinPrincipal(pUser);
  }

}
