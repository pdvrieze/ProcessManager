package uk.ac.bournemouth.darwin.catalina.realm;

import java.security.Principal;
import java.util.Set;

import net.devrieze.util.StringCache;


/**
 * A principal for the darwin system.
 *
 * @author Paul de Vrieze
 */
public interface DarwinPrincipal extends Principal {

  /**
   * Determine whether the user has overall admin rights.
   *
   * @return <code>true</code> if the user is admin, <code>false</code> if not.
   */
  boolean isAdmin();

  /**
   * Determine whether the user has the given role. Note that depending on the
   * implementation some caching can occur and therefore the answer might not
   * reflect very recent changes in the database.
   *
   * @param pRoleName The role to verify.
   * @return <code>true</code> if the user has the role, <code>false</code> if
   *         not.
   */
  boolean hasRole(String pRoleName);

  /**
   * Get the email address of the user.
   *
   * @return The email address, or <code>null</code> if not known.
   */
  CharSequence getEmail();


  /**
   * Get a set of the roles of the user. Note that changes to this will not be
   * allowed, and in any case would not be reflected in the database.
   *
   * @return The set of roles.
   */
  Set<? extends CharSequence> getRolesSet();

  /**
   * Create a copy principal that uses the given stringcache to reuse strings.
   * This should save memory as well as make comparison faster.
   *
   * @param pStringCache The stringcache to use for reusing strings.
   * @return A principal using the de-duped strings. Note that this may, but
   *         does not ahve to return the original princpal.
   */
  Principal cacheStrings(StringCache pStringCache);


}
