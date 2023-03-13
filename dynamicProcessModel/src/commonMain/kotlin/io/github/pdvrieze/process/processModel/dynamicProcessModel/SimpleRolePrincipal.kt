package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.security.RolePrincipal
import nl.adaptivity.util.multiplatform.PrincipalCompat

class SimpleRolePrincipal(private val name: String, vararg roles: String): RolePrincipal {
    private val roles = roles.toSet()

    override fun hasRole(role: String): Boolean = role in roles

    override fun getName(): String = name

    override fun equals(other: Any?): Boolean {
        if (other !is PrincipalCompat) return false
        return name == other.name
    }

    override fun toString(): String {
        return "SimpleRolePrincipal(name='$name', roles=$roles)"
    }


}
