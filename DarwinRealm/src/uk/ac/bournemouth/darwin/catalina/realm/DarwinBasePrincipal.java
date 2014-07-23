package uk.ac.bournemouth.darwin.catalina.realm;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;


/**
 * Base class for darwin principals. This will allo
 *
 * @author Paul de Vrieze
 */
public abstract class DarwinBasePrincipal extends GenericPrincipal implements DarwinPrincipal {

  /**
   * Principals will refresh against the database after 5 minutes. This means
   * that database changes will be effective after a maximum of 5 minutes.
   */
  private static final long MAX_CACHE = 300000; // Ten minute cache

  /**
   * Attribute to record when we last checked the database. By default very far
   * in the past so that we will certainly need to check.
   */
  private final long aLastChecked = Long.MIN_VALUE;


  /**
   * Create a new {@link DarwinBasePrincipal}
   * @param pRealm The realm the principal is recorded against.
   * @param pName The name of the principal.
   */
  public DarwinBasePrincipal(final Realm pRealm, final String pName) {
    super(pRealm, pName, null);
  }

  /**
   * This is used by subclasses to determine whether the user data needs to be reretrieved from the database.
   * @return <code>true</code> if the database needs to be consulted.
   */
  protected boolean needsRefresh() {
    final long now = System.currentTimeMillis();
    return now < (aLastChecked + MAX_CACHE);
  }

}
