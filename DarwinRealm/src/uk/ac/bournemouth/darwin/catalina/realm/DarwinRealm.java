package uk.ac.bournemouth.darwin.catalina.realm;


import java.security.Principal;

import org.apache.catalina.connector.CoyotePrincipal;
import org.apache.catalina.realm.Constants;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.util.StringManager;

import net.devrieze.util.DBHelper;


public class DarwinRealm extends RealmBase {

  protected final String info = "uk.ac.bournemouth.darwin.catalina.realm.DarwinRealm/1.0";

  protected static final String name = "DarwinRealm";

  private static StringManager sm = StringManager.getManager(Constants.Package);


  public String getInfo() {
    return info;
  }


  protected String getName() {
    return name;
  }


  public boolean hasRole(Principal principal, String role) {
    if (principal instanceof DarwinUserPrincipalImpl) {
      return ((DarwinUserPrincipalImpl) principal).getRoles().contains(role);
    }

    if (principal instanceof CoyotePrincipal) {
      // Look up this user in the UserDatabaseRealm.  The new
      // principal will contain UserDatabaseRealm role info.
      DarwinUserPrincipalImpl p = getDarwinPrincipal(principal.getName());
      if (p != null) {
        return p.getRoles().contains(role);
      }
    }
    return false;
  }


  private DarwinUserPrincipalImpl getDarwinPrincipal(String pName) {
    return new DarwinUserPrincipalImpl(pName);
  }


  @Override
  protected String getPassword(String pArg0) {
    // Never return a password, we don't store that, and it's a bad idea.
    return null;
  }


  @Override
  protected DarwinPrincipal getPrincipal(String pArg0) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }


  public static DBHelper getDbHelper() {
    return DBHelper.dbHelper(getDBResource(), DarwinRealm.class);
  }


  private static String getDBResource() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }
  
  
  
}
