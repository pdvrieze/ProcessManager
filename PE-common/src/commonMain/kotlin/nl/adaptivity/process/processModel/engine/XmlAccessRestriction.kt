package nl.adaptivity.process.processModel.engine

import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat

internal class XmlAccessRestriction(private val restriction: String): AccessRestriction {
    override fun hasAccess(context: Any?, principal: PrincipalCompat, permission: SecurityProvider.Permission): Boolean {
        // XXX Do something better for this
        return restriction.trim() == principal.getName()
    }

    override fun serializeToString(): String {
        return restriction
    }
}
