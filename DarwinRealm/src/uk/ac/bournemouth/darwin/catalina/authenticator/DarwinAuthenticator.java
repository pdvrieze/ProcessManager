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

package uk.ac.bournemouth.darwin.catalina.authenticator;

import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.StringAdapter;
import org.apache.catalina.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DarwinAuthenticator extends ValveBase implements Authenticator, Lifecycle {


  @NotNull
  private static final String QUERY_USER_FROM_DWNID = "SELECT user FROM tokens WHERE ip=? AND token=? AND (epoch + 1800) > UNIX_TIMESTAMP()";

  private enum AuthResult {
    AUTHENTICATED,
    ERROR,
    EXPIRED,
    LOGIN_NEEDED;
  }

  private static final String AUTHTYPE = "DARWIN";

  public static final String DBRESOURCE = "java:comp/env/jdbc/webauth";

  private static final String LOGGERNAME = "DarwinRealm";

  private boolean mStarted = false;

  /**
   * The lifecycle event support for this component.
   */
  @NotNull
  protected LifecycleSupport mLifecycle = new LifecycleSupport(this);

  @Nullable
  private Context mContext;

  private String mLoginPage = "/accounts/login";


  @Override
  public void addLifecycleListener(final LifecycleListener listener) {
    mLifecycle.addLifecycleListener(listener);
  }

  @Override
  public void setContainer(final Container container) {
    super.setContainer(container);
    if (container instanceof Context) {
      mContext = (Context) container;
    }
  }

  @Override
  public LifecycleListener[] findLifecycleListeners() {
    return mLifecycle.findLifecycleListeners();
  }

  @Override
  public void removeLifecycleListener(final LifecycleListener listener) {
    mLifecycle.removeLifecycleListener(listener);
  }

  @Override
  public void start() throws LifecycleException {
    if (mStarted) {
      throw new LifecycleException("Already started");
    }
    mLifecycle.fireLifecycleEvent(START_EVENT, null);
    mStarted = true;
    setLoginPage(null); // Default is not specified.
  }

  @Override
  public void stop() throws LifecycleException {
    mLifecycle.fireLifecycleEvent(STOP_EVENT, null);
    mStarted = false;
  }

  @Override
  public void invoke(final Request request, final Response response) throws IOException, ServletException {

    final AuthResult authresult = authenticate(request);

    final Context context = this.mContext;
    final Realm realm = context == null ? null : context.getRealm();
    if (realm != null) {
      logFine("This context has an authentication realm, enforce the constraints");
      final SecurityConstraint[] constraints = realm.findSecurityConstraints(request, context);
      if (constraints == null) {
        logFine("Realm has no constraints, calling next in chain");
        // Unconstrained
        getNext().invoke(request, response);
        return;
      }
      // Need security, set cache control
      response.setHeader("Cache-Control", "private");
      if (!realm.hasUserDataPermission(request, response, constraints)) {
        denyPermission(response);
        return;
      }

      boolean authRequired = true;
      for (int i = 0; (i < constraints.length) && authRequired; i++) {
        if (!constraints[i].getAuthConstraint()) {
          authRequired = false;
        } else if (!constraints[i].getAllRoles()) {
          final String[] roles = constraints[i].findAuthRoles();
          if ((roles == null) || (roles.length == 0)) {
            authRequired = false;
          }
        }
      }

      if (authRequired) {
        if ((authresult == AuthResult.AUTHENTICATED) && realm.hasResourcePermission(request, response, constraints, context)) {
          getNext().invoke(request, response);
          return;
        } else if (authresult == AuthResult.AUTHENTICATED) { // We are authenticated, but the wrong user.
          response.sendError(HttpServletResponse.SC_FORBIDDEN, "User " + request.getUserPrincipal() + " does not have permission for "
              + realm.getInfo() + " class:" + realm.getClass().getName());
        } else {
          if (authresult != AuthResult.ERROR) {
            // Not logged in yet. So go to login page.
            final LoginConfig loginConfig = context == null ? null : context.getLoginConfig();
            String loginpage = loginConfig != null ? loginConfig.getLoginPage() : null;
            if (loginpage == null) {
              loginpage = mLoginPage;
            }
            if (loginpage != null) {
              final StringBuilder incommingPath = new StringBuilder();
              incommingPath.append(request.getPathInfo());
              response.sendRedirect(loginpage + "?redirect=" + URLEncoder.encode(incommingPath.toString(), "utf-8"));
            } else {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You need to log in for this page, but no login page is configured");
            }
          }
          return;
        }


      } else {
        getNext().invoke(request, response);
        return;
      }

    } else {
      // No realm, no authentication required.
      getNext().invoke(request, response);
    }
  }

  public String getLoginPage() {
    return mLoginPage;
  }

  public void setLoginPage(final String loginPage) {
    mLoginPage = loginPage;
  }

  public static DarwinPrincipal getPrincipal(final String user) {
    try {
      return getDarwinPrincipal(DBConnection.getDataSource(DBRESOURCE), null, user);
    } catch (NamingException e) {
      getLogger().log(Level.WARNING, "Failure to connect to database", e);
      return null;
    }
  }

  public static DarwinPrincipal asDarwinPrincipal(final Principal user) {
    try {
      final Realm realm = null;

      return toDarwinPrincipal(DBConnection.getDataSource(DBRESOURCE), realm, user);
    } catch (NamingException e) {
      getLogger().log(Level.WARNING, "Failure to connect to database", e);
      return null;
    }
  }

  private static DarwinUserPrincipal getDarwinPrincipal(final DataSource dataSource, final Realm realm, final String userName) {
    return new DarwinUserPrincipalImpl(dataSource, realm, userName);
  }

  private static DarwinUserPrincipal toDarwinPrincipal(final DataSource dataSource, final Realm realm, final Principal principal) {
    if (principal == null) {
      return null;
    }
    if (principal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) principal;
    }
    return new DarwinUserPrincipalImpl(dataSource, realm, principal.getName());
  }

  @NotNull
  private static AuthResult authenticate(final Request request) {
    DarwinUserPrincipal principal;
    final DataSource dataSource;
    try {
      dataSource = DBConnection.getDataSource(DBRESOURCE);
    } catch (NamingException e) {
      getLogger().log(Level.WARNING, "Failure to connect to database", e);
      return AuthResult.ERROR;
    }
    principal = toDarwinPrincipal(dataSource, request.getContext().getRealm(), request.getUserPrincipal());
    if (principal != null) {
      logFine("Found preexisting principal, converted to darwinprincipal: " + principal.getName());
      request.setAuthType(AUTHTYPE);
      request.setUserPrincipal(principal);
      return AuthResult.AUTHENTICATED;
    }


    String user = null;
    final Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (final Cookie cookie : cookies) {
        if ("DWNID".equals(cookie.getName())) {
          final String requestIp = request.getRemoteAddr();
          logFiner("Found DWNID cookie with value: '" + cookie.getValue() + "' and request ip:" + requestIp);
          try (final DBConnection db = DBConnection.newInstance(dataSource)){
            try (final DBQuery query = db.makeQuery(QUERY_USER_FROM_DWNID)){
              query.addParam(1, requestIp);
              query.addParam(2, cookie.getValue());
              try(final ResultSet result = query.execQuery()) {
                if (result != null) {
                  try(StringAdapter adapter = new StringAdapter(query, result, false)) {
                    final Iterator<String> it = adapter.iterator();
                    if (it.hasNext()) {
                      user = it.next();
                    } else {
                      logFine("Expired cookie: '" + cookie.getValue() + '\'');
                      return AuthResult.EXPIRED;
                    }
                  }
                }
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            }
          }
          break;
        }
      }
    } else {
      logFiner("No authentication cookie found");
    }
    if (user != null) {
      logFine("Authenticated user " + user);
      request.setAuthType(AUTHTYPE);
      principal = getDarwinPrincipal(dataSource, request.getContext().getRealm(), user);
      request.setUserPrincipal(principal);
      return AuthResult.AUTHENTICATED;
    }
    return AuthResult.LOGIN_NEEDED;
  }

  private static void denyPermission(final Response response) throws IOException {
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  private static Logger getLogger() {
    return Logger.getLogger(LOGGERNAME);
  }

  private static void logFiner(final String string) {
    getLogger().finer(string);
  }

  private static void logFine(final String string) {
    getLogger().fine(string);
  }

  private static void logInfo(final String message) {
    getLogger().info(message);
  }

}
