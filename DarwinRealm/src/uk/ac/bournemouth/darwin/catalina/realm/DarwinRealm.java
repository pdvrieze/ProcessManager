package uk.ac.bournemouth.darwin.catalina.realm;


import java.security.Principal;

import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.Realm;
import org.apache.catalina.realm.Constants;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.connector.CoyotePrincipal;


public class DarwinRealm
    extends UserDatabaseRealm
    implements Realm
{

    protected final String info =
        "uk.ac.bournemouth.darwin.catalina.realm.DarwinRealm/1.0";

    protected static final String name = "DarwinRealm";

    private static StringManager sm =
        StringManager.getManager(Constants.Package);


    public String getInfo() {
        return info;
    }


    protected String getName() {
        return name;
    }


    public boolean hasRole(Principal principal, String role) {

        if (principal instanceof CoyotePrincipal) {
            // Look up this user in the UserDatabaseRealm.  The new
            // principal will contain UserDatabaseRealm role info.
            Principal p = super.getPrincipal(principal.getName());
            if (p != null) {
                principal = p;
            }
        }
        return super.hasRole(principal, role);
    }
}
