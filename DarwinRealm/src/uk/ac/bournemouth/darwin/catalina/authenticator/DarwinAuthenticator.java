package uk.ac.bournemouth.darwin.catalina.authenticator;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

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

import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;
import uk.ac.bournemouth.darwin.html.util.DarwinHtml;

import net.devrieze.util.DBHelper;
import net.devrieze.util.DBHelper.DBQuery;
import net.devrieze.util.StringAdapter;


public class DarwinAuthenticator extends ValveBase implements Authenticator, Lifecycle{
  
  private static final String AUTHTYPE = "DARWIN";
  public static final String DBRESOURCE = "java:/comp/env/jdbc/webauth";

  private boolean aStarted = false;
  
  /**
   * The lifecycle event support for this component.
   */
  protected LifecycleSupport aLifecycle = new LifecycleSupport(this);
  private DBHelper aDb;
  private Context aContext;

  private boolean authenticate(Request pRequest, Response pResponse) throws IOException {
    DarwinUserPrincipal principal = toDarwinPrincipal(pRequest.getUserPrincipal());
    if (principal != null) { 
      pRequest.setAuthType(AUTHTYPE);
      pRequest.setUserPrincipal(principal);
      return (true); 
    }
    
    
    String user =null;
    try {
      DBHelper db = getDatabase();
      final Cookie[] cookies = pRequest.getCookies();
      if (cookies!=null) {
        for (Cookie cookie: cookies) {
          if ("DWNID".equals(cookie.getName())) {
            // TODO Look up user from database
            String requestIp = pRequest.getRemoteAddr();
            DBQuery query = db.makeQuery("SELECT user FROM tokens WHERE ip=? AND token=? AND (epoch +1800) > UNIX_TIMESTAMP()");
            try {
              query.addParam(1, requestIp);
              query.addParam(2, cookie.getValue());
              ResultSet result = query.execQuery();
              if (result!=null) {
                Iterator<String> it = (new StringAdapter(result)).iterator();
                
                if (it.hasNext()) {
                  user = it.next();
                }
                result.close();
              }
            } finally {
              query.close();
            }
            break;
          }
        }
      }
    } catch (SQLException e) {
      DarwinHtml.writeError(pResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while authenticating", e);
      return false;
    }
    if (user!=null) {
      pRequest.setAuthType(AUTHTYPE);
      principal = getDarwinPrincipal(user);
      pRequest.setUserPrincipal(principal);
      return (true); 
    }
    return false;
  }

  private DarwinUserPrincipal getDarwinPrincipal(String pUserName) {
    return new DarwinUserPrincipalImpl(pUserName);
  }

  private DarwinUserPrincipal toDarwinPrincipal(Principal pPrincipal) {
    if (pPrincipal==null) { return null; }
    if (pPrincipal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) pPrincipal;
    }
    return new DarwinUserPrincipalImpl(pPrincipal.getName());
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
    LoginConfig config = aContext.getLoginConfig();

    boolean authenticated = authenticate(pRequest, pResponse);
    
    Realm realm = aContext.getRealm();
    if (realm!=null) {
      SecurityConstraint[] constraints = realm.findSecurityConstraints(pRequest, aContext);
      if (constraints==null) {
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
        if (authenticated && realm.hasResourcePermission(pRequest, pResponse, constraints, aContext)) {
          getNext().invoke(pRequest, pResponse);
          return;
        } else {
          
          // Not logged in yet. So go to login page.
          String loginpage = config.getLoginPage();
          if (loginpage!=null) {
            StringBuilder incommingPath = new StringBuilder();
            incommingPath.append(pRequest.getPathInfo());
            pResponse.sendRedirect(loginpage+"?redirect="+URLEncoder.encode(incommingPath.toString(), "utf-8"));
          } else {
            pResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
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
  
}
