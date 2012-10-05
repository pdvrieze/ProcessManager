package uk.ac.bournemouth.darwin.catalina.realm;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;


public abstract class DarwinBasePrincipal extends GenericPrincipal implements DarwinPrincipal {

  private static final long MAX_CACHE = 60000; // One minute cache

  private final long aLastChecked = Long.MIN_VALUE;


  public DarwinBasePrincipal(final Realm pRealm, final String pName) {
    super(pRealm, pName, null);
  }

  protected boolean needsRefresh() {
    final long now = System.currentTimeMillis();
    return now < (aLastChecked + MAX_CACHE);
  }

}
