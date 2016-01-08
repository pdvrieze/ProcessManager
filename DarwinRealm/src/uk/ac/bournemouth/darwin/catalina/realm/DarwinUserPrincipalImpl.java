/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.darwin.catalina.realm;

import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.StringAdapter;
import org.apache.catalina.Realm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class DarwinUserPrincipalImpl extends DarwinBasePrincipal implements DarwinUserPrincipal {

  private static final String DOMAIN = "bournemouth.ac.uk";

  private final DataSource mDataSource;

  public DarwinUserPrincipalImpl(final DataSource dataSource, final Realm realm, final String name) {
    super(realm, name);
    mDataSource = dataSource;
  }

  @SuppressWarnings("null")
  @NotNull
  private Set<String> mRoles = Collections.emptySet();

  /**
   * Get a set of all the roles in the principal. Note that this will create a
   * copy to allow concurrency and refreshes.
   */
  @SuppressWarnings("null")
  @Override
  @NotNull
  public synchronized Set<? extends String> getRolesSet() {
    refreshIfNeeded();
    return Collections.unmodifiableSet(new HashSet<>(mRoles));
  }

  @SuppressWarnings("null")
  @Override
  @NotNull
  public String[] getRoles() {
    Set<? extends String> lroles;
    synchronized (this) {
      refreshIfNeeded();
      lroles = mRoles;
      if (roles == null) {
        roles = lroles.toArray(new String[lroles.size()]);
        Arrays.sort(roles);
      }
    }
    return roles;
  }

  private synchronized void refreshIfNeeded() {
    if ((!(mRoles instanceof HashSet)) || needsRefresh()) {
      mRoles = new HashSet<>();
      roles = null;
      try(final DBConnection db = DBConnection.newInstance(mDataSource)) {
        try(final DBQuery query = db.makeQuery("SELECT role FROM user_roles WHERE user=?")) {
          query.addParam(1, getName());
          try (final StringAdapter queryResult = new StringAdapter(query, query.execQuery(), true)){
            for (final String role : queryResult.all()) {
              mRoles.add(role);
            }
          }
        }
      }
    }
  }

  @Override
  public boolean hasRole(@Nullable final String role) {
    if ("*".equals(role)) {
      return true;
    }
    if (role == null) {
      return false;
    }
    refreshIfNeeded();
    return mRoles.contains(role);
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
    for (final String role : mRoles) {
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
  public synchronized Principal cacheStrings(final StringCache stringCache) {
    name = stringCache.lookup(this.name);
    DBConnection.setStringCache(stringCache);

    // Instead of resetting the roles holder, just update the set to prevent database
    // roundtrips.
    final Set<String> tmpRoles = mRoles;
    mRoles = new HashSet<>();
    for (final String role : tmpRoles) {
      mRoles.add(stringCache.lookup(role));
    }
    roles = null; // Just remove cache. This doesn't need database roundtrip
    return this;
  }

}
