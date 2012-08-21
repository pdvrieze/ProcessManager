package uk.ac.bournemouth.darwin.catalina.realm;

import java.util.Collections;
import java.util.Set;

import net.devrieze.util.DBHelper;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  public DarwinUserPrincipalImpl(String pName) {
    super(pName);
  }

  private Set<? extends String> aRoles;

  public Set<? extends String> getRoles() {
    refreshIfNeeded();
    return Collections.unmodifiableSet(aRoles);
  }

  private void refreshIfNeeded() {
    if (aRoles==null || needsRefresh()) {
      aRoles = Collections.emptySet();
// TODO Add roles to the database
//      DBHelper db = DarwinRealm.getDbHelper();
//      db.makeQuery();
      
    }
  }

  
  
}
