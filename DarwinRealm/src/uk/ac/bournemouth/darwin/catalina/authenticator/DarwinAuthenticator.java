package uk.ac.bournemouth.darwin.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.Cookie;

import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;

import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;
import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipalImpl;


public class DarwinAuthenticator extends AuthenticatorBase{
  
  private static final String AUTHTYPE = "DARWIN";

  @Override
  protected boolean authenticate(Request pRequest, Response pResponse, LoginConfig pConfig) throws IOException {
    DarwinUserPrincipal principal = toDarwinPrincipal(pRequest.getUserPrincipal());
    if (principal != null) { 
      pRequest.setAuthType(AUTHTYPE);
      pRequest.setUserPrincipal(principal);
      return (true); 
    }
    
    for (Cookie cookie: pRequest.getCookies()) {
      if ("DWNID".equals(cookie.getName())) {
        // TODO Look up user from database
        
        break;
      }
    }
    
    // Not logged in yet. So go to login page.
    pResponse.sendRedirect(pConfig.getLoginPage());
    return false;
  }

  private DarwinUserPrincipal toDarwinPrincipal(Principal pPrincipal) {
    if (pPrincipal==null) { return null; }
    if (pPrincipal instanceof DarwinUserPrincipal) {
      return (DarwinUserPrincipal) pPrincipal;
    }
    return new DarwinUserPrincipalImpl(pPrincipal.getName());
  }

  
  
}
