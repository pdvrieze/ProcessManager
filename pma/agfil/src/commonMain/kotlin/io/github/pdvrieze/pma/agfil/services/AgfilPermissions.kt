package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.ClaimId
import net.devrieze.util.security.SecurityProvider.Permission
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.UseAuthScope

sealed class AgfilPermissions {

    /** Permission to pick a garage (in europassist service)*/
    object PICK_GARAGE: AgfilPermissions(), UseAuthScope

    /** Permission to start a handling process in the garage */
    object INFORM_GARAGE: AgfilPermissions(), UseAuthScope

    object LIST_GARAGES: AgfilPermissions(), UseAuthScope

    object SEND_CAR: AgfilPermissions(), AuthScope {
        fun context(claimId: ClaimId): UseAuthScope {
            return ClaimUseScope(this, claimId)
        }
    }

    private class ClaimUseScope(private val base: AuthScope, val claimId: ClaimId): UseAuthScope {
        override val description: String
            get() = "${base.description}(${claimId.id})"

        override fun includes(useScope: Permission): Boolean = when {
            useScope !is ClaimUseScope -> false
            base!=useScope.base -> false
            else -> claimId==useScope.claimId
        }
    }

}
