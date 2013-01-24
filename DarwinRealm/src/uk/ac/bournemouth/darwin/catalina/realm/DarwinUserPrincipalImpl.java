package uk.ac.bournemouth.darwin.catalina.realm;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.Realm;

import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBHelper;
import net.devrieze.util.db.DBHelper.DBQuery;
import net.devrieze.util.db.StringAdapter;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";

  private final DBHelper aDbHelper;

  public DarwinUserPrincipalImpl(final DBHelper dbHelper, final Realm pRealm, final String pName) {
    super(pRealm, pName);
    aDbHelper = dbHelper;
  }

  private Set<String> aRoles;

  /**
   * Get a set of all the roles in the principal. Note that this will create a
   * copy to allow concurrency and refreshes.
   */
  @Override
  public synchronized Set<? extends String> getRolesSet() {
    refreshIfNeeded();
    return Collections.unmodifiableSet(new HashSet<String>(aRoles));
  }

  @Override
  public String[] getRoles() {
    Set<? extends String> lroles;
    synchronized (this) {
      refreshIfNeeded();
      lroles = aRoles;
      if ((roles == null) && (aRoles != null)) {
        roles = lroles.toArray(new String[lroles.size()]);
        Arrays.sort(roles);
      }
    }
    return roles;
  }

  private synchronized void refreshIfNeeded() {
    if ((aRoles == null) || needsRefresh()) {
      aRoles = new HashSet<String>();
      roles = null;
      final DBQuery query = aDbHelper.makeQuery("SELECT role FROM user_roles WHERE user=?");
      query.addParam(1, getName());
      final StringAdapter queryResult = new StringAdapter(query, query.execQuery(), true);
      try {
        for (final String role : queryResult.all()) {
          aRoles.add(role);
        }
      } finally {
        queryResult.close();
      }
    }
  }

  @Override
  public boolean hasRole(final String pRole) {
    if ("*".equals(pRole)) {
      return true;
    }
    if (pRole == null) {
      return false;
    }
    refreshIfNeeded();
    return (aRoles != null) && aRoles.contains(pRole);
  }


  @Override
  public Principal getUserPrincipal() {
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("DarwinUserPrincipal[").append(getName());
    refreshIfNeeded();
    if (aRoles != null) {
      char sep = '(';
      for (final String role : aRoles) {
        result.append(sep).append(role);
        sep = ',';
      }
    }
    result.append("])");
    return result.toString();
  }

  @Override
  public CharSequence getEmail() {
    final String lName = getName();
    final StringBuilder result = new StringBuilder(lName.length() + DOMAIN.length() + 1);
    result.append(lName).append('@').append(DOMAIN);
    return result;
  }

  @Override
  public boolean isAdmin() {
    return hasRole("admin");
  }

  @Override
  public synchronized Principal cacheStrings(final StringCache pStringCache) {
    name = pStringCache.lookup(this.name);
    aDbHelper.setStringCache(pStringCache);

    // Instead of resetting the roles holder, just update the set to prevent database
    // roundtrips.
    if (aRoles != null) {
      final Set<String> tmpRoles = aRoles;
      aRoles = new HashSet<String>();
      for (final String role : tmpRoles) {
        aRoles.add(pStringCache.lookup(role));
      }
      roles = null; // Just remove cache. This doesn't need database roundtrip
    }
    return this;
  }

}
