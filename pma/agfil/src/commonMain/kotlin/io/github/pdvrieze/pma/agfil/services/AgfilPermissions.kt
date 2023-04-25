package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.CustomerId
import io.github.pdvrieze.pma.agfil.data.InvoiceId
import net.devrieze.util.security.SecurityProvider.Permission
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.UseAuthScope

sealed class AgfilPermissions {

    sealed class AbstractClaimPermission: AgfilPermissions(), AuthScope {
        operator fun invoke(claimId: ClaimId): UseAuthScope {
            return ClaimUseScope(this, claimId)
        }
    }

    /** Permission to start a handling process in the garage */
    object INFORM_GARAGE: AgfilPermissions(), UseAuthScope

    object LIST_GARAGES: AgfilPermissions(), UseAuthScope

    object FIND_CUSTOMER_ID: AgfilPermissions(), UseAuthScope

    object SEND_CAR: AgfilPermissions(), AuthScope {
        operator fun invoke(claimId: ClaimId): UseAuthScope = ClaimUseScope(this, claimId)
    }

    object GET_POLICY: AbstractClaimPermission() {
        operator fun invoke(customerId: CustomerId): UseAuthScope = CustomerUseScope(this, customerId)
    }

    object ASSESSOR {
        object ASSESS_DAMAGE: AbstractClaimPermission()
        object NEGOTIATE_REPAIR_COSTS: AbstractClaimPermission()
    }

    object LEECS {
        object START_PROCESSING: AbstractClaimPermission()
        object INTERNAL {
            object CONTACT_GARAGE: AbstractClaimPermission()
        }

    }

    object POLICYHOLDER {
        object SEND_CLAIM_FORM: AbstractClaimPermission()
        object ASSIGN_GARAGE: AbstractClaimPermission()
        object INTERNAL {
            object REPORT_CLAIM: AgfilPermissions(), UseAuthScope
            object SEND_CAR: AgfilPermissions(), AuthScope {
                operator fun invoke(carRegistration: CarRegistration): UseAuthScope = CarUseScope(this, carRegistration)
            }
        }
    }


    object EUROP_ASSIST {
        /** Permission to pick a garage (in europassist service)*/
        object PICK_GARAGE: AgfilPermissions(), UseAuthScope
        object INTERNAL {
            object ASSIGN_GARAGE: AbstractClaimPermission()
        }
    }

    object GARAGE {
        object INFORM_INCOMING_CAR: AgfilPermissions(), UseAuthScope
        object SEND_CAR: AgfilPermissions(), AuthScope {
            operator fun invoke(carRegistration: CarRegistration): UseAuthScope = CarUseScope(this, carRegistration)
        }

        object NOTIFY_INVOICE_PAID: AgfilPermissions(), AuthScope {
            operator fun invoke(invoiceId: InvoiceId): UseAuthScope = InvoiceUseScope(this, invoiceId)
        }
        object REVIEW_CAR: AgfilPermissions(), AuthScope {
            operator fun invoke(carRegistration: CarRegistration): UseAuthScope = CarUseScope(this, carRegistration)
        }
        object NEGOTIATE_REPAIR_COSTS: AbstractClaimPermission()
    }


    object AGFIL {
        object CLAIM {
            object CREATE: AgfilPermissions(), UseAuthScope
        }

        object GET_CUSTOMER_INFO: AgfilPermissions(), UseAuthScope {
            operator fun invoke(customerId: CustomerId): UseAuthScope = CustomerUseScope(this, customerId)
        }

        object INTERNAL {
            object SEND_CLAIM_FORM: AbstractClaimPermission()
            object TERMINATE_CLAIM: AbstractClaimPermission()
            object NOTIFIY_INVALID_CLAIM: AbstractClaimPermission()
            object PROCESS_CLAIM_FORM: AbstractClaimPermission()
            object PAY_GARAGE_INVOICE: AbstractClaimPermission()
        }
    }

    object CLAIM {
        object NOTIFY_ASSIGNED: AbstractClaimPermission()
        object READ: AbstractClaimPermission()
        object READ_ACCIDENTINFO: AbstractClaimPermission()
        object RETURN_FORM: AbstractClaimPermission()
        object REGISTER_INVOICE: AbstractClaimPermission()
        object RECORD_ASSIGNED_GARAGE : AbstractClaimPermission()
    }

    private class ClaimUseScope(base: AuthScope, claimId: ClaimId): IdUseScope<ClaimId>(base, claimId)

    private class CustomerUseScope(base: AuthScope, customerId: CustomerId): IdUseScope<CustomerId>(base, customerId)

    private class InvoiceUseScope(base: AuthScope, invoiceId: InvoiceId): IdUseScope<InvoiceId>(base, invoiceId)

    private class CarUseScope(base: AuthScope, carRegistration: CarRegistration): IdUseScope<CarRegistration>(base, carRegistration)

    abstract class IdUseScope<V>(private val base: AuthScope, val id: V): UseAuthScope {
        override val description: String
            get() = "${base.description}(${id})"

        @Suppress("UNCHECKED_CAST")
        override fun includes(useScope: Permission): Boolean = when {
            javaClass != useScope.javaClass -> false
            base!=(useScope as IdUseScope<V>).base -> false
            else -> id==useScope.id
        }

        override fun toString() = description
    }

}
