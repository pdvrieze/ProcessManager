package uk.ac.bournemouth.darwin.catalina.realm;

import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.Realm;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection.DBHelper;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.StringAdapter;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";

  private final DBHelper aDbHelper;

  public DarwinUserPrincipalImpl(final DBHelper dbHelper, final Realm pRealm, final String pName) {
    super(pRealm, pName);
    aDbHelper = dbHelper;
  }

  @SuppressWarnings("null")
  @NotNull
  private Set<String> aRoles = Collections.emptySet();

  /**
   * Get a set of all the roles in the principal. Note that this will create a
   * copy to allow concurrency and refreshes.
   */
  @SuppressWarnings("null")
  @Override
  @NotNull
  public synchronized Set<? extends String> getRolesSet() {
    try {
      refreshIfNeeded();
    } catch (SQLException e) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new HashSet<>(aRoles));
  }

  @SuppressWarnings("null")
  @Override
  @NotNull
  public String[] getRoles() {
    Set<? extends String> lroles;
    synchronized (this) {
      try {
        refreshIfNeeded();
      } catch (SQLException e) {
        return new String[0];
      }
      lroles = aRoles;
      if (roles == null) {
        roles = lroles.toArray(new String[lroles.size()]);
        Arrays.sort(roles);
      }
    }
    return roles;
  }

  private synchronized void refreshIfNeeded() throws SQLException {
    if ((!(aRoles instanceof HashSet)) || needsRefresh()) {
      aRoles = new HashSet<>();
      roles = null;
      try(final DBConnection db = aDbHelper.getConnection();
          final DBQuery query = db.makeQuery("SELECT role FROM user_roles WHERE user=?")) {
        query.addParam(1, getName());
        try (final StringAdapter queryResult = new StringAdapter(query, query.execQuery(), true)){
          for (final String role : queryResult.all()) {
            aRoles.add(role);
          }
        }
      }
      markRefresh();
    }
  }

  @Override
  public boolean hasRole(@Nullable final String pRole) {
    if ("*".equals(pRole)) {
      return true;
    }
    if (pRole == null) {
      return false;
    }
    try {
      refreshIfNeeded();
    } catch (SQLException e) {
      return false;
    }
    return aRoles.contains(pRole);
  }


  @Override
  @NotNull
  public Principal getUserPrincipal() {
    return this;
  }

  @Override
  @NotNull
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("DarwinUserPrincipal[").append(getName());
    if (needsRefresh()) {
      result.append("stale|");
    }
    char sep = '(';
    for (final String role : aRoles) {
      result.append(sep).append(role);
      sep = ',';
    }
    result.append("])");

    @SuppressWarnings("null")
    @NotNull
    final String rs = result.toString();
    return rs;
  }

  @NotNull
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

  @NotNull
  @Override
  public synchronized Principal cacheStrings(final StringCache pStringCache) {
    name = pStringCache.lookup(this.name);
    aDbHelper.setStringCache(pStringCache);

    // Instead of resetting the roles holder, just update the set to prevent database
    // roundtrips.
    final Set<String> tmpRoles = aRoles;
    aRoles = new HashSet<>();
    for (final String role : tmpRoles) {
      aRoles.add(pStringCache.lookup(role));
    }
    roles = null; // Just remove cache. This doesn't need database roundtrip
    return this;
  }

}
