package uk.ac.bournemouth.darwin.catalina.realm;

/**
 * Principal darwin users that has a method for retrieving email addresses of
 * users.
 *
 * @author Paul de Vrieze
 */
public interface DarwinUserPrincipal extends DarwinPrincipal {

  @Override
  CharSequence getEmail();

}
