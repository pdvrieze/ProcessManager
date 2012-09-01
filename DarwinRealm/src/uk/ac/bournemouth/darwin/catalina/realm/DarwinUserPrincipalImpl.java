package uk.ac.bournemouth.darwin.catalina.realm;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.devrieze.util.DBHelper;
import net.devrieze.util.DBHelper.DBQuery;
import net.devrieze.util.StringAdapter;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";

  public DarwinUserPrincipalImpl(String pName) {
    super(pName);
  }

  private Set<String> aRoles;

  public Set<? extends String> getRoles(DBHelper db) {
    refreshIfNeeded(db);
    return Collections.unmodifiableSet(aRoles);
  }

  private void refreshIfNeeded(DBHelper db) {
    if (aRoles==null || needsRefresh()) {
      try {
        aRoles = new HashSet<String>();
        DBQuery query = db.makeQuery("SELECT role FROM user_roles WHERE user=?");
        try {
          query.addParam(0, getName());
          StringAdapter queryResult = new StringAdapter(query.execQuery());
          try {
            for(String role: queryResult) {
              aRoles.add(role);
            }
          } finally {
            queryResult.close();
          }
        } finally {
          query.close();
        }
      } catch (SQLException e) {
        // TODO log properly
        e.printStackTrace();
        aRoles = Collections.emptySet();
      }
    }
  }

  @Override
  public CharSequence getEmail() {
    final String name = getName();
    StringBuilder result=new StringBuilder(name.length()+DOMAIN.length()+1);
    result.append(name).append('@').append(DOMAIN);
    return result;
  }

  
  
}
