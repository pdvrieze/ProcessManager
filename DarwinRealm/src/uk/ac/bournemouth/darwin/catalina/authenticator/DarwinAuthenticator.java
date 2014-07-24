package uk.ac.bournemouth.darwin.catalina.authenticator;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.StringAdapter;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;

import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;


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

  public static final String DBRESOURCE = "java:/comp/env/jdbc/webauth";

  private static final String LOGGERNAME = "DarwinRealm";

  private boolean aStarted = false;

  /**
   * The lifecycle event support for this component.
   */
  @NotNull
  protected LifecycleSupport aLifecycle = new LifecycleSupport(this);

  @Nullable
  private Context aContext;

  private String aLoginPage = "/accounts/login";


  @Override
  public void addLifecycleListener(final LifecycleListener pListener) {
    aLifecycle.addLifecycleListener(pListener);
  }

  @Override
  public void setContainer(final Container pContainer) {
    super.setContainer(pContainer);
    if (pContainer instanceof Context) {
      aContext = (Context) pContainer;
    }
  }

  @Override
  public LifecycleListener[] findLifecycleListeners() {
    return aLifecycle.findLifecycleListeners();
  }

  @Override
  public void removeLifecycleListener(final LifecycleListener pListener) {
    aLifecycle.removeLifecycleListener(pListener);
  }

  @Override
  public void start() throws LifecycleException {
    if (aStarted) {
      throw new LifecycleException("Already started");
    }
    aLifecycle.fireLifecycleEvent(START_EVENT, null);
    aStarted = true;
    setLoginPage(null); // Default is not specified.
  }

  @Override
  public void stop() throws LifecycleException {
    aLifecycle.fireLifecycleEvent(STOP_EVENT, null);
    aStarted = false;
  }

  @Override
  public void invoke(final Request pRequest, final Response pResponse) throws IOException, ServletException {

    final AuthResult authresult = authenticate(pRequest);

    final Context context = this.aContext;
    final Realm realm = context == null ? null : context.getRealm();
    if (realm != null) {
      logFine("This context has an authentication realm, enforce the constraints");
      final SecurityConstraint[] constraints = realm.findSecurityConstraints(pRequest, context);
      if (constraints == null) {
        logFine("Realm has no constraints, calling next in chain");
        // Unconstrained
        getNext().invoke(pRequest, pResponse);
        return;
      }
      // Need security, set cache control
      pResponse.setHeader("Cache-Control", "private");
      if (!realm.hasUserDataPermission(pRequest, pResponse, constraints)) {
        denyPermission(pResponse);
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
        if ((authresult == AuthResult.AUTHENTICATED) && realm.hasResourcePermission(pRequest, pResponse, constraints, context)) {
          getNext().invoke(pRequest, pResponse);
          return;
        } else if (authresult == AuthResult.AUTHENTICATED) { // We are authenticated, but the wrong user.
          pResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "User " + pRequest.getUserPrincipal() + " does not have permission for "
              + realm.getInfo() + " class:" + realm.getClass().getName());
        } else {
          if (authresult != AuthResult.ERROR) {
            // Not logged in yet. So go to login page.
            final LoginConfig loginConfig = context == null ? null : context.getLoginConfig();
            String loginpage = loginConfig != null ? loginConfig.getLoginPage() : null;
            if (loginpage == null) {
              loginpage = aLoginPage;
            }
            if (loginpage != null) {
              final StringBuilder incommingPath = new StringBuilder();
              incommingPath.append(pRequest.getPathInfo());
              pResponse.sendRedirect(loginpage + "?redirect=" + URLEncoder.encode(incommingPath.toString(), "utf-8"));
            } else {
              pResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You need to log in for this page, but no login page is configured");
            }
          }
          return;
        }


      } else {
        getNext().invoke(pRequest, pResponse);
        return;
      }

    } else {
      // No realm, no authentication required.
      getNext().invoke(pRequest, pResponse);
    }
  }

  public String getLoginPage() {
    return aLoginPage;
  }

  public void setLoginPage(final String loginPage) {
    aLoginPage = loginPage;
  }

  public static DarwinPrincipal getPrincipal(final String pUser) {
    try(final DBConnection db = DBConnection.newInstance(DBRESOURCE)) {
      return getDarwinPrincipal(db, null, pUser);
    }
  }

  public static DarwinPrincipal asDarwinPrincipal(final Principal pUser) {
    try (final DBConnection db = getDatabaseStatic()) {
      final Realm realm = null;

      return toDarwinPrincipal(db, realm, pUser);
    }
  }

  private static DarwinUserPrincipal getDarwinPrincipal(final DBConnection pDbHelper, final Realm pRealm, final String pUserName) {
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pUserName);
  }

  private static DarwinUserPrincipal toDarwinPrincipal(final DBConnection pDbHelper, final Realm pRealm, final Principal pPrincipal) {
    if (pPrincipal == null) {
      return null;
    }
    if (pPrincipal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) pPrincipal;
    }
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pPrincipal.getName());
  }

  @NotNull
  private AuthResult authenticate(final Request pRequest) {
    try (final DBConnection db = getDatabase()){
      DarwinUserPrincipal principal = toDarwinPrincipal(db, pRequest.getContext().getRealm(), pRequest.getUserPrincipal());
      if (principal != null) {
        logInfo("Found preexisting principal, converted to darwinprincipal: " + principal.getName());
        pRequest.setAuthType(AUTHTYPE);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED;
      }


      String user = null;
      final Cookie[] cookies = pRequest.getCookies();
      if (cookies != null) {
        for (final Cookie cookie : cookies) {
          if ("DWNID".equals(cookie.getName())) {
            final String requestIp = pRequest.getRemoteAddr();
            logFine("Found DWNID cookie with value: '" + cookie.getValue() + "' and request ip:" + requestIp);
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
            break;
          }
        }
      } else {
        logFine("No authentication cookie found");
      }
      if (user != null) {
        logInfo("Authenticated user " + user);
        pRequest.setAuthType(AUTHTYPE);
        principal = getDarwinPrincipal(db, pRequest.getContext().getRealm(), user);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED;
      }
    }
    return AuthResult.LOGIN_NEEDED;
  }

  DBConnection getDatabase() {
    return DBConnection.newInstance(DBRESOURCE);
  }

  private static DBConnection getDatabaseStatic() {
    return DBConnection.newInstance(DBRESOURCE);
  }

  private static void denyPermission(final Response pResponse) throws IOException {
    pResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  private static Logger getLogger() {
    return Logger.getLogger(LOGGERNAME);
  }

  private static void logFine(final String pString) {
    getLogger().fine(pString);
  }

  private static void logInfo(final String pMessage) {
    getLogger().info(pMessage);
  }

}
