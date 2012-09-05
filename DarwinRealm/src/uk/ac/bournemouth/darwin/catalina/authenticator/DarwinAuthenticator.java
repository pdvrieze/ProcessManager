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

import net.devrieze.util.DBHelper;
import net.devrieze.util.DBHelper.DBQuery;
import net.devrieze.util.StringAdapter;

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

import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;
import uk.ac.bournemouth.darwin.html.util.DarwinHtml;


public class DarwinAuthenticator extends ValveBase implements Authenticator, Lifecycle{
  
  
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
  private StringBuilder aError = new StringBuilder();
  private String aLoginPage = "/accounts/login";
  

  private AuthResult authenticate(Request pRequest, Response pResponse) throws IOException {
    DBHelper db = getDatabase();
    try {
      DarwinUserPrincipal principal = toDarwinPrincipal(db, pRequest.getContext().getRealm(), pRequest.getUserPrincipal());
      if (principal != null) { 
        logFine("Found preexisting principal, converted to darwinprincipal: "+principal.getName());
        pRequest.setAuthType(AUTHTYPE);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED; 
      }
      
      
      String user =null;
      try {
        final Cookie[] cookies = pRequest.getCookies();
        if (cookies!=null) {
          for (Cookie cookie: cookies) {
            if ("DWNID".equals(cookie.getName())) {
              String requestIp = pRequest.getRemoteAddr();
              logFine("Found DWNID cookie with value: '"+cookie.getValue()+"' and request ip:"+requestIp);
              DBQuery query = db.makeQuery("SELECT user FROM tokens WHERE ip=? AND token=? AND (epoch + 1800) > UNIX_TIMESTAMP()");
              try {
                query.addParam(1, requestIp);
                query.addParam(2, cookie.getValue());
                ResultSet result = query.execQuery();
                if (result!=null) {
                  Iterator<String> it = (new StringAdapter(query, result, false)).iterator();
                  
                  if (it.hasNext()) {
                    user = it.next();
                  } else {
                    logFine("Expired cookie: '"+cookie.getValue()+'\'');
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
      } catch (SQLException e) {
        logError("Error while verifying cookie in database", e);
        DarwinHtml.writeError(pResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while authenticating", e);
        return AuthResult.ERROR;
      }
      if (user!=null) {
        logFine("Authenticated user "+user);
        pRequest.setAuthType(AUTHTYPE);
        principal = getDarwinPrincipal(db, pRequest.getContext().getRealm(), user);
        pRequest.setUserPrincipal(principal);
        return AuthResult.AUTHENTICATED; 
      }
    } finally {
      try {
        db.close();
      } catch (SQLException e) {
        logError("Error closing database connection", e);
        aDb = null;
      }
    }      
    return AuthResult.LOGIN_NEEDED;
  }

  private DarwinUserPrincipal getDarwinPrincipal(DBHelper pDbHelper, Realm pRealm, String pUserName) {
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pUserName);
  }

  private DarwinUserPrincipal toDarwinPrincipal(DBHelper pDbHelper, Realm pRealm, Principal pPrincipal) {
    if (pPrincipal==null) { return null; }
    if (pPrincipal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) pPrincipal;
    }
    return new DarwinUserPrincipalImpl(pDbHelper, pRealm, pPrincipal.getName());
  }

  DBHelper getUserDatabase(Request pRequest) {
    return DBHelper.dbHelper(DBRESOURCE, pRequest);
  }

  
  
  @Override
  public void setContainer(Container pContainer) {
    super.setContainer(pContainer);
    if (pContainer instanceof Context) {
      aContext = (Context)pContainer; 
    }
  }

  public String getLoginPage() {
    return aLoginPage;
  }

  public void setLoginPage(String loginPage) {
    aLoginPage = loginPage;
  }

  @Override
  public void addLifecycleListener(LifecycleListener pListener) {
    aLifecycle.addLifecycleListener(pListener);
  }

  @Override
  public LifecycleListener[] findLifecycleListeners() {
    return aLifecycle.findLifecycleListeners();
  }

  @Override
  public void removeLifecycleListener(LifecycleListener pListener) {
    aLifecycle.removeLifecycleListener(pListener);
  }

  @Override
  public void start() throws LifecycleException {
    if (aStarted) throw new LifecycleException("Already started");
    aLifecycle.fireLifecycleEvent(START_EVENT, null);
    aStarted = true;
    setLoginPage(null); // Default is not specified.
  }
  
  DBHelper getDatabase() {
    if (aDb==null) {
      aDb = DBHelper.dbHelper(DBRESOURCE, this);
    }
    return aDb;
  }

  @Override
  public void stop() throws LifecycleException {
    aLifecycle.fireLifecycleEvent(STOP_EVENT, null);
    aStarted = false;

    try {
      if (aDb!=null) {
        aDb.close();
      }
    } catch (SQLException e) {
      throw new LifecycleException(e);
    }
    aDb=null;
  }

  @Override
  public void invoke(Request pRequest, Response pResponse) throws IOException, ServletException {

    AuthResult authresult = authenticate(pRequest, pResponse);
    
    Realm realm = aContext.getRealm();
    if (realm!=null) {
      logInfo("This context has an authentication realm, enforce the constraints");
      SecurityConstraint[] constraints = realm.findSecurityConstraints(pRequest, aContext);
      if (constraints==null) {
        logInfo("Realm has no constraints, calling next in chain");
        // Unconstrained
        getNext().invoke(pRequest, pResponse);
        return;
      }
      // Need security, set cache control
      pResponse.setHeader("Cache-Control", "private");
      if (!realm.hasUserDataPermission(pRequest, pResponse,
          constraints)) {
        denyPermission(pResponse);
        return;
      }

      boolean authRequired = true;
      for(int i=0; i < constraints.length && authRequired; i++) {
          if(!constraints[i].getAuthConstraint()) {
              authRequired = false;
          } else if(!constraints[i].getAllRoles()) {
              String [] roles = constraints[i].findAuthRoles();
              if(roles == null || roles.length == 0) {
                  authRequired = false;
              }
          }
      }
      
      if (authRequired) {
        if (authresult==AuthResult.AUTHENTICATED && realm.hasResourcePermission(pRequest, pResponse, constraints, aContext)) {
          getNext().invoke(pRequest, pResponse);
          return;
        } else if (authresult==AuthResult.AUTHENTICATED) { // We are authenticated, but the wrong user.
          pResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "User "+pRequest.getUserPrincipal()+" does not have permission for "+realm.getInfo()+" class:"+realm.getClass().getName() );
        }else {
          if (authresult!=AuthResult.ERROR) {
            // Not logged in yet. So go to login page.
            LoginConfig loginConfig = aContext.getLoginConfig();
            String loginpage = loginConfig!=null ? loginConfig.getLoginPage() : null;
            if (loginpage==null) { loginpage = aLoginPage; }
            if (loginpage!=null) {
              StringBuilder incommingPath = new StringBuilder();
              incommingPath.append(pRequest.getPathInfo());
              pResponse.sendRedirect(loginpage+"?redirect="+URLEncoder.encode(incommingPath.toString(), "utf-8"));
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

  private void denyPermission(Response pResponse) throws IOException {
    pResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
  }
  
  private Logger getLogger() {
    return Logger.getLogger(LOGGERNAME);
  }
  
  private void logFine(String pString) {
    getLogger().fine(pString);
  }
  
  private void logFiner(String pString) {
    getLogger().finer(pString);
  }

  private void logError(String pMessage) {
    getLogger().severe(pMessage);
  }
  
  private void logError(String pMessage, Throwable pException) {
    getLogger().log(Level.SEVERE, pMessage, pException);
  }
  
  private void logWarning(String pMessage) {
    getLogger().warning(pMessage);
  }
  
  private void logWarning(String pMessage, Throwable pException) {
    getLogger().log(Level.WARNING, pMessage, pException);
  }
  
  private void logInfo(String pMessage) {
    getLogger().info(pMessage);
  }
  
}
