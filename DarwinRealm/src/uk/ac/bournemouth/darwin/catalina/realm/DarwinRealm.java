package uk.ac.bournemouth.darwin.catalina.realm;


import net.devrieze.util.db.DBConnection;
import org.apache.catalina.*;
import org.apache.catalina.connector.CoyotePrincipal;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.util.LifecycleSupport;
import uk.ac.bournemouth.darwin.catalina.authenticator.DarwinAuthenticator;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;


public class DarwinRealm implements Realm, Lifecycle {

  private static final String INFO = "uk.ac.bournemouth.darwin.catalina.realm.DarwinRealm/1.0";

  @SuppressWarnings("unused")
  private static final String NAME = "DarwinRealm";

  private static final String RESOURCE = DarwinAuthenticator.DBRESOURCE;

  private boolean aStarted = false;

  /**
   * The lifecycle event support for this component.
   */
  protected LifecycleSupport aLifecycle = new LifecycleSupport(this);

  private Container aContainer;

  PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);


  @Override
  public void start() throws LifecycleException {
    if (aStarted) {
      throw new LifecycleException("Already started");
    }
    aLifecycle.fireLifecycleEvent(START_EVENT, null);
    aStarted = true;
  }


  @Override
  public void stop() throws LifecycleException {
    aLifecycle.fireLifecycleEvent(STOP_EVENT, null);
    aStarted = false;
  }


  @Override
  public void addLifecycleListener(final LifecycleListener listener) {
    aLifecycle.addLifecycleListener(listener);
  }


  @Override
  public LifecycleListener[] findLifecycleListeners() {
    return aLifecycle.findLifecycleListeners();
  }


  @Override
  public void removeLifecycleListener(final LifecycleListener listener) {
    aLifecycle.removeLifecycleListener(listener);
  }


  @Override
  public Container getContainer() {
    return aContainer;
  }


  @Override
  public void setContainer(final Container container) {
    final Container oldContainer = aContainer;
    aContainer = container;
    propChangeSupport.firePropertyChange("container", oldContainer, aContainer);
  }


  @Override
  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    propChangeSupport.addPropertyChangeListener(listener);
  }


  @Override
  public Principal authenticate(final String username, final String credentials) {
    throw new UnsupportedOperationException("In this implementation the realm does not support independent authentication.");
  }


  @Override
  public Principal authenticate(final String username, final byte[] credentials) {
    throw new UnsupportedOperationException("In this implementation the realm does not support independent authentication.");
  }


  @Override
  public Principal authenticate(final String username, final String digest, final String nonce, final String nc, final String cnonce, final String qop, final String realm, final String md5a2) {
    throw new UnsupportedOperationException("In this implementation the realm does not support independent authentication.");
  }


  @Override
  public Principal authenticate(final X509Certificate[] certs) {
    throw new UnsupportedOperationException("In this implementation the realm does not support independent authentication.");
  }


  @Override
  public void backgroundProcess() {
    // TODO Auto-generated method stub
  }


  @Override
  public SecurityConstraint[] findSecurityConstraints(final Request request, final Context context) {
    ArrayList<SecurityConstraint> result;

    final SecurityConstraint[] constraints = context.findConstraints();
    if ((constraints == null) || (constraints.length == 0)) {
      return null;
    }

    // Check each defined security constraint
    String uri = request.getRequestPathMB().toString();
    // Bug47080 - in rare cases this may be null
    // Mapper treats as '/' do the same to prevent NPE
    if (uri == null) {
      uri = "/";
    }

    // First try simple matches
    result = new ArrayList<>();
    final String method = request.getMethod();

    {
      boolean found = false;
      for (final SecurityConstraint constraint : constraints) {
        final SecurityCollection[] collections = constraint.findCollections();
        if ((collections == null) || (collections.length == 0)) {
          continue;
        }
        for (final SecurityCollection collection : collections) {
          final String[] patterns = collection.findPatterns();
          if ((patterns == null) || (patterns.length == 0)) {
            continue;
          }
          for (final String pattern : patterns) {
            if (uri.equals(pattern)) {
              found = true;
              if (collection.findMethod(method)) {
                result.add(constraint);
              }
            }
          }
        }
      }

      // Return on match
      if (found) {
        return result.toArray(new SecurityConstraint[result.size()]);
      }
    }

    { // Now try simple patterns.
      boolean found = false;
      int longest = -1;
      for (final SecurityConstraint constraint : constraints) {
        final SecurityCollection[] collections = constraint.findCollections();
        if ((collections == null) || (collections.length == 0)) {
          continue;
        }
        for (final SecurityCollection collection : collections) {
          final String[] patterns = collection.findPatterns();
          if ((patterns == null) || (patterns.length == 0)) {
            continue;
          }
          boolean matched = false;
          int length = -1;
          for (final String pattern : patterns) {
            if (pattern.startsWith("/") && pattern.endsWith("/*") && (pattern.length() >= longest)) {
              if (pattern.length() == 2) {
                matched = true;
                length = pattern.length();
              } else if (pattern.regionMatches(0, uri, 0, pattern.length() - 1)
                  || (((pattern.length() - 2) == uri.length()) && pattern.regionMatches(0, uri, 0, pattern.length() - 2))) {
                matched = true;
                length = pattern.length();
              }

            }
          }
          if (matched) {
            if (length > longest) {
              result.clear();
              longest = length;
            }
            found = true;
            if (collection.findMethod(method)) {
              result.add(constraint);
            }
          }
        }
      }
      // Return on match
      if (found) {
        return result.toArray(new SecurityConstraint[result.size()]);
      }
    }

    { // Now more complex patterns
      boolean found = false;
      for (final SecurityConstraint constraint : constraints) {
        final SecurityCollection[] collections = constraint.findCollections();
        if ((collections == null) || (collections.length == 0)) {
          continue;
        }
        SecurityCollection matchingCollection = null;
        for (final SecurityCollection collection : collections) {
          final String[] patterns = collection.findPatterns();
          if ((patterns == null) || (patterns.length == 0)) {
            continue;
          }
          for (final String pattern : patterns) {
            if (pattern.startsWith("*.")) {
              final int slash = uri.lastIndexOf("/");
              final int dot = uri.lastIndexOf(".");
              if ((slash >= 0) && (dot > slash) && (dot != (uri.length() - 1)) && ((uri.length() - dot) == (pattern.length() - 1))) {
                if (pattern.regionMatches(1, uri, dot, uri.length() - dot)) {
                  matchingCollection = collection;
                }
              }
            }
          }
        }
        if (matchingCollection != null) {
          found = true;
          if (matchingCollection.findMethod(method)) {
            result.add(constraint);
          }

        }
      }
      // Return on match
      if (found) {
        return result.toArray(new SecurityConstraint[result.size()]);
      }
    }

    {
      for (final SecurityConstraint constraint : constraints) {
        final SecurityCollection[] collections = constraint.findCollections();
        boolean matched = false;
        if ((collections == null) || (collections.length == 0)) {
          continue;
        }
        forCollection: for (final SecurityCollection collection : collections) {
          final String[] patterns = collection.findPatterns();
          if ((patterns == null) || (patterns.length == 0)) {
            continue;
          }
          for (final String pattern : patterns) {
            if ("/".equals(pattern)) {
              matched = true;
              break forCollection;
            }
          }

        }
        if (matched) {
          result.add(constraint);
        }
      }
    }

    return result.toArray(new SecurityConstraint[result.size()]);
  }


  @Override
  public boolean hasResourcePermission(final Request request, final Response response, final SecurityConstraint[] constraints, final Context context) throws IOException {
    if ((constraints == null) || (constraints.length == 0)) {
      return (true);
    }


    // Which user principal have we already authenticated?
    final Principal principal = request.getPrincipal();
    boolean status = false;
    for (final SecurityConstraint constraint : constraints) {

      String roles[];
      if (constraint.getAllRoles()) {
        // * means all roles defined in web.xml
        roles = request.getContext().findSecurityRoles();
      } else {
        roles = constraint.findAuthRoles();
      }

      if (roles == null) {
        roles = new String[0];
      }


      if ((roles.length == 0) && !constraint.getAllRoles()) {
        if (constraint.getAuthConstraint()) {
          status = false; // No listed roles means no access at all
          break;
        } else {
          status = true;
        }
      } else if (principal == null) {
        // No user, no access
      } else {
        for (final String role : roles) {
          if (hasRole(principal, role)) {
            status = true;
          }
        }
      }
    }

    // Return a "Forbidden" message denying access to this resource
    if (!status) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    return status;
  }


  @Override
  public boolean hasUserDataPermission(final Request request, final Response response, final SecurityConstraint[] constraints) throws IOException {
    if ((constraints == null) || (constraints.length == 0)) {
      return true;
    }

    for (final SecurityConstraint constraint : constraints) {
      final String userConstraint = constraint.getUserConstraint();
      if ((userConstraint == null) || "NONE".equals(userConstraint)) {
        return true;
      }
    }

    if (request.isSecure()) {
      return true;
    }

    final int redirectPort = request.getConnector().getRedirectPort();

    if (redirectPort <= 0) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }

    final StringBuilder newUrl = new StringBuilder();
    newUrl.append("https://").append(request.getServerName());
    if (redirectPort != 433) {
      newUrl.append(':').append(redirectPort);
    }
    newUrl.append(request.getRequestURI());
    final String queryString = request.getQueryString();
    if ((queryString != null) && (queryString.length() > 0)) {
      newUrl.append('?').append(queryString);
    }
    response.sendRedirect(newUrl.toString());
    return false;
  }


  @Override
  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    propChangeSupport.removePropertyChangeListener(listener);
  }


  @Override
  public String getInfo() {
    return INFO;
  }


  @Override
  public boolean hasRole(Principal principal, final String role) {
    final Principal usedPrincipal;
    if (principal instanceof GenericPrincipal) {
      usedPrincipal = ((GenericPrincipal) principal).getUserPrincipal();
      if (usedPrincipal instanceof GenericPrincipal) {
        return ((GenericPrincipal) usedPrincipal).hasRole(role);
      }
    } else {
      usedPrincipal = principal;
    }

    if (usedPrincipal instanceof CoyotePrincipal) {
      // Look up this user in the UserDatabaseRealm.  The new
      // principal will contain UserDatabaseRealm role info.
      final DarwinUserPrincipalImpl p;
      try {
        p = getDarwinPrincipal(usedPrincipal.getName());
      } catch (NamingException e) {
        return false;
      }
      if (p != null) {
        return p.hasRole(role);
      }
    }
    return false;
  }


  private DarwinUserPrincipalImpl getDarwinPrincipal(final String name) throws NamingException {
    return new DarwinUserPrincipalImpl(getDataSource(), this, name);
  }

  private static DataSource getDataSource() throws NamingException {
    return DBConnection.getDataSource(getDBResource());
  }

  private static String getDBResource() {
    return RESOURCE;
  }


}
