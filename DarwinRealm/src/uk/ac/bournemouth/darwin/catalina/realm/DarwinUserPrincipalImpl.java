package uk.ac.bournemouth.darwin.catalina.realm;

import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.devrieze.util.DBHelper;
import net.devrieze.util.DBHelper.DBQuery;
import net.devrieze.util.StringAdapter;

import org.apache.catalina.Realm;



public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";
  private DBHelper aDbHelper;

  public DarwinUserPrincipalImpl(DBHelper dbHelper, Realm pRealm, String pName) {
    super(pRealm, pName);
    aDbHelper = dbHelper;
  }

  private Set<String> aRoles;

  public Set<? extends String> getRolesSet() {
    refreshIfNeeded();
    return Collections.unmodifiableSet(aRoles);
  }
  
  @Override
  public String[] getRoles() {
    Set<? extends String> lroles = getRolesSet();
    if (roles==null && (aRoles!=null)) {
      roles = lroles.toArray(new String[lroles.size()]);
      Arrays.sort(roles);
    }
    return roles;
  }

  private void refreshIfNeeded() {
    if (aRoles==null || needsRefresh()) {
      try {
        aRoles = new HashSet<String>();
        DBQuery query = aDbHelper.makeQuery("SELECT role FROM user_roles WHERE user=?");
        try {
          query.addParam(1, getName());
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
  public boolean hasRole(String pRole) {
    if ("*".equals(pRole)) { return true; }
    if (pRole == null) { return false; }
    refreshIfNeeded();
    return aRoles!=null && aRoles.contains(pRole);
  }

  
  
  @Override
  public Principal getUserPrincipal() {
    return this;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("DarwinUserPrincipal[")
          .append(getName());
    refreshIfNeeded();
    if (aRoles!=null) {
      char sep='(';
      for(String role:aRoles) {
        result.append(sep).append(role);
        sep=',';
      }
    }
    result.append("])");
    return result.toString();
  }

  @Override
  public CharSequence getEmail() {
    final String name = getName();
    StringBuilder result=new StringBuilder(name.length()+DOMAIN.length()+1);
    result.append(name).append('@').append(DOMAIN);
    return result;
  }

  
  
}
