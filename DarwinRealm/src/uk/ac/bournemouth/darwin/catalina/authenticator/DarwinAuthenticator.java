package uk.ac.bournemouth.darwin.catalina.authenticator;

import java.io.IOException;

import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;


public class DarwinAuthenticator extends AuthenticatorBase{

  
  
  @Override
  protected boolean authenticate(Request pRequest, Response pResponse, LoginConfig pConfig) throws IOException {
    
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  
  
}
