package uk.ac.bournemouth.darwin.catalina.authenticator;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;

import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;
import uk.ac.bournemouth.darwin.html.util.DarwinHtml;

import net.devrieze.util.db.DBHelper;
import net.devrieze.util.db.StringAdapter;
import net.devrieze.util.db.DBHelper.DBQuery;


public class DarwinAuthenticator extends ValveBase implements Authenticator, Lifecycle {


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
  protected LifecycleSupport aLifecycle = new LifecycleSupport(this);

  private DBHelper aDb;

  private Context aContext;

  private final StringBuilder aError = new StringBuilder();

  private String aLoginPage = "/accounts/login";


  private AuthResult authenticate(final Request pRequest, final Response pResponse) throws IOException {
    final DBHelper db = getDatabase();
    try {
      DarwinUserPrincipal principal = toDarwinPrincipal(db, pRequest.getContext().getRealm(), pRequest.getUserPrincipal());
      if (principal != null) {
        logFine("Found preexisting principal, converted to darwinprincipal: " + principal.getName());
        pRequest.setAuthType(AUTHTYPE);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED;
      }


      String user = null;
      try {
        final Cookie[] cookies = pRequest.getCookies();
        if (cookies != null) {
          for (final Cookie cookie : cookies) {
            if ("DWNID".equals(cookie.getName())) {
              final String requestIp = pRequest.getRemoteAddr();
              logFine("Found DWNID cookie with value: '" + cookie.getValue() + "' and request ip:" + requestIp);
              final DBQuery query = db.makeQuery("SELECT user FROM tokens WHERE ip=? AND token=? AND (epoch + 1800) > UNIX_TIMESTAMP()");
              try {
                query.addParam(1, requestIp);
                query.addParam(2, cookie.getValue());
                final ResultSet result = query.execQuery();
                if (result != null) {
                  final Iterator<String> it = (new StringAdapter(query, result, false)).iterator();

                  if (it.hasNext()) {
                    user = it.next();
                  } else {
                    logFine("Expired cookie: '" + cookie.getValue() + '\'');
                    return AuthResult.EXPIRED;
                  }
                }
              } finally {
                query.close();
              }
              break;
            }
          }
        }
      } catch (final SQLException e) {
        logError("Error while verifying cookie in database", e);
        DarwinHtml.writeError(pResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while authenticating", e);
        return AuthResult.ERROR;
      }
      if (user != null) {
        logFine("Authenticated user " + user);
        pRequest.setAuthType(AUTHTYPE);
        principal = getDarwinPrincipal(db, pRequest.getContext().getRealm(), user);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED;
      }
    } finally {
      try {
        db.close();
      } catch (final SQLException e) {
        logError("Error closing database connection", e);
        aDb = null;
      }
    }
    return AuthResult.LOGIN_NEEDED;
  }

  private static DarwinUserPrincipal getDarwinPrincipal(final DBHelper pDbHelper, final Realm pRealm, final String pUserName) {
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pUserName);
  }

  private static DarwinUserPrincipal toDarwinPrincipal(final DBHelper pDbHelper, final Realm pRealm, final Principal pPrincipal) {
    if (pPrincipal == null) {
      return null;
    }
    if (pPrincipal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) pPrincipal;
    }
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pPrincipal.getName());
  }

  static DBHelper getUserDatabase(final Request pRequest) {
    return DBHelper.dbHelper(DBRESOURCE, pRequest);
  }


  @Override
  public void setContainer(final Container pContainer) {
    super.setContainer(pContainer);
    if (pContainer instanceof Context) {
      aContext = (Context) pContainer;
    }
  }

  public String getLoginPage() {
    return aLoginPage;
  }

  public void setLoginPage(final String loginPage) {
    aLoginPage = loginPage;
  }

  @Override
  public void addLifecycleListener(final LifecycleListener pListener) {
    aLifecycle.addLifecycleListener(pListener);
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

  DBHelper getDatabase() {
    if (aDb == null) {
      aDb = getDatabaseStatic(this);
    }
    return aDb;
  }

  private static DBHelper getDatabaseStatic(final Object pKey) {
    return DBHelper.dbHelper(DBRESOURCE, pKey);
  }

  @Override
  public void stop() throws LifecycleException {
    aLifecycle.fireLifecycleEvent(STOP_EVENT, null);
    aStarted = false;

    try {
      if (aDb != null) {
        aDb.close();
      }
    } catch (final SQLException e) {
      throw new LifecycleException(e);
    }
    aDb = null;
  }

  @Override
  public void invoke(final Request pRequest, final Response pResponse) throws IOException, ServletException {

    final AuthResult authresult = authenticate(pRequest, pResponse);

    final Realm realm = aContext.getRealm();
    if (realm != null) {
      logInfo("This context has an authentication realm, enforce the constraints");
      final SecurityConstraint[] constraints = realm.findSecurityConstraints(pRequest, aContext);
      if (constraints == null) {
        logInfo("Realm has no constraints, calling next in chain");
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
        if ((authresult == AuthResult.AUTHENTICATED) && realm.hasResourcePermission(pRequest, pResponse, constraints, aContext)) {
          getNext().invoke(pRequest, pResponse);
          return;
        } else if (authresult == AuthResult.AUTHENTICATED) { // We are authenticated, but the wrong user.
          pResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "User " + pRequest.getUserPrincipal() + " does not have permission for "
              + realm.getInfo() + " class:" + realm.getClass().getName());
        } else {
          if (authresult != AuthResult.ERROR) {
            // Not logged in yet. So go to login page.
            final LoginConfig loginConfig = aContext.getLoginConfig();
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

  private void denyPermission(final Response pResponse) throws IOException {
    pResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  private Logger getLogger() {
    return Logger.getLogger(LOGGERNAME);
  }

  private void logFine(final String pString) {
    getLogger().fine(pString);
  }

  private void logFiner(final String pString) {
    getLogger().finer(pString);
  }

  private void logError(final String pMessage) {
    getLogger().severe(pMessage);
  }

  private void logError(final String pMessage, final Throwable pException) {
    getLogger().log(Level.SEVERE, pMessage, pException);
  }

  private void logWarning(final String pMessage) {
    getLogger().warning(pMessage);
  }

  private void logWarning(final String pMessage, final Throwable pException) {
    getLogger().log(Level.WARNING, pMessage, pException);
  }

  private void logInfo(final String pMessage) {
    getLogger().info(pMessage);
  }

  public static DarwinPrincipal getPrincipal(final String pUser) {
    final DBHelper db = getDatabaseStatic(DarwinAuthenticator.class);
    try {
      return getDarwinPrincipal(db, null, pUser);
    } finally {
      try {
        db.close();
      } catch (final SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static DarwinPrincipal asDarwinPrincipal(final Principal pUser) {
    final DBHelper db = getDatabaseStatic(DarwinAuthenticator.class);
    final Realm realm = null;

    return toDarwinPrincipal(db, realm, pUser);
  }

}
