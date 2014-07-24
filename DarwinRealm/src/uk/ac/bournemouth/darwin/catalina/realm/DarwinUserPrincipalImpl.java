package uk.ac.bournemouth.darwin.catalina.realm;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.StringAdapter;

import org.apache.catalina.Realm;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";

  private final DataSource aDataSource;

  public DarwinUserPrincipalImpl(final DataSource pDataSource, final Realm pRealm, final String pName) {
    super(pRealm, pName);
    aDataSource = pDataSource;
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
    refreshIfNeeded();
    return Collections.unmodifiableSet(new HashSet<>(aRoles));
  }

  @SuppressWarnings("null")
  @Override
  @NotNull
  public String[] getRoles() {
    Set<? extends String> lroles;
    synchronized (this) {
      refreshIfNeeded();
      lroles = aRoles;
      if (roles == null) {
        roles = lroles.toArray(new String[lroles.size()]);
        Arrays.sort(roles);
      }
    }
    return roles;
  }

  private synchronized void refreshIfNeeded() {
    if ((!(aRoles instanceof HashSet)) || needsRefresh()) {
      aRoles = new HashSet<>();
      roles = null;
      try(final DBConnection db = DBConnection.newInstance(aDataSource)) {
        try(final DBQuery query = db.makeQuery("SELECT role FROM user_roles WHERE user=?")) {
          query.addParam(1, getName());
          try (final StringAdapter queryResult = new StringAdapter(query, query.execQuery(), true)){
            for (final String role : queryResult.all()) {
              aRoles.add(role);
            }
          }
        }
      }
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
    refreshIfNeeded();
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
    refreshIfNeeded();
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
    DBConnection.setStringCache(pStringCache);

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
