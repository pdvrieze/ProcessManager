package uk.ac.bournemouth.darwin.catalina.realm;


public interface DarwinUserPrincipal extends DarwinPrincipal {

  @Override
  CharSequence getEmail();

}
