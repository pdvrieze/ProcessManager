/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.test.loanOrigination.auth

import net.devrieze.util.Handle
import net.devrieze.util.security.SecurityProvider.Permission
import nl.adaptivity.process.engine.pma.ExtScope
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanApplication

sealed class LoanPermissions {
    object SIGN : LoanPermissions(), UseAuthScope

    object PRICE_LOAN : LoanPermissions(), AuthScope {
        fun context(customerId: String, amount: Double): UseAuthScope {
            return MonetaryUseScope(PRICE_LOAN, customerId, amount)
        }

        fun restrictTo(maxAmount: Double): AuthScope {
            return MonetaryRestrictionPermissionScope(PRICE_LOAN, maxAmount = maxAmount)
        }

        fun restrictTo(
            customerId: String,
            maxAmount: Double = Double.NaN
        ): AuthScope {
            return MonetaryRestrictionPermissionScope(PRICE_LOAN, customerId, maxAmount)
        }
    }

    object PRINT_OFFER : LoanPermissions(), UseAuthScope
    object SIGN_LOAN : LoanPermissions(), UseAuthScope {
        fun context(customerId: String, offerAmount: Double): UseAuthScope {
            return MonetaryUseScope(SIGN_LOAN, customerId, offerAmount)
        }

        fun restrictTo(
            customerId: String? = null,
            maxAmount: Double = Double.NaN
        ): AuthScope {
            return MonetaryRestrictionPermissionScope(SIGN_LOAN, customerId, maxAmount)
        }

        override fun includes(useScope: Permission): Boolean = when (useScope) {
            is ExtScope<*> -> useScope.scope == this
            else -> false
        }
    }

    object INVALIDATE_ACTIVITY : LoanPermissions(), AuthScope {
        fun context(hNodeInstance: PNIHandle) =
            UPDATE_ACTIVITY_STATE.contextImpl(hNodeInstance)

        override fun includes(useScope: Permission): Boolean {
            return when (useScope) {
                is ExtScope<*> -> Handle.invalid<Any>() != useScope.extraData
                else -> false
            }
        }
    }

    object EVALUATE_LOAN : LoanPermissions(), AuthScope {
        fun context(application: LoanApplication) = context(application.customerId, application.amount)
        fun context(customerId: String, amount: Double) = MonetaryUseScope(EVALUATE_LOAN, customerId, amount)
        operator fun invoke(
            customerId: String? = null,
            maxAmount: Double = Double.NaN
        ): AuthScope {
            return MonetaryRestrictionPermissionScope(EVALUATE_LOAN, customerId, maxAmount)
        }
    }

    object CREATE_CUSTOMER : LoanPermissions(), UseAuthScope
    object QUERY_CUSTOMER_DATA : LoanPermissions(), AuthScope {
        operator fun invoke(customerId: String) = contextImpl(customerId)
    }

    object UPDATE_CUSTOMER_DATA : LoanPermissions(), UseAuthScope
    object UPDATE_ACTIVITY_STATE : LoanPermissions(), AuthScope {

        operator fun invoke(hNodeInstance: PNIHandle) =
            contextImpl(hNodeInstance)

        override fun includes(useScope: Permission): Boolean {
            return useScope is ExtScope<*> && useScope.scope == this
        }

        override fun intersect(otherScope: AuthScope): AuthScope {
            return when {
                otherScope == UPDATE_ACTIVITY_STATE -> this
                otherScope is ExtScope<*> && otherScope.scope == this -> otherScope
                else -> super.intersect(otherScope)
            }
        }
    }

    object GET_CREDIT_REPORT : LoanPermissions(), AuthScope {
        operator fun invoke(taxId: String) = contextImpl(taxId)
    }

    object OPEN_ACCOUNT : LoanPermissions(), AuthScope {
        operator fun invoke(customerId: String) = contextImpl(customerId)
    }


    protected fun <V> contextImpl(contextData: V): ExtScope<V> {
        require(this is AuthScope) {"${this.javaClass.name} is not an AuthScope"}
        return ExtScope(this, contextData)
    }

    override fun toString(): String = javaClass.simpleName.substringAfterLast('.')
}

class MonetaryUseScope(val scope: LoanPermissions, val customerId: String, val amount: Double) :
    UseAuthScope {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MonetaryUseScope

        if (scope != other.scope) return false
        if (customerId != other.customerId) return false
        if (amount != other.amount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scope.hashCode()
        result = 31 * result + customerId.hashCode()
        result = 31 * result + amount.hashCode()
        return result
    }

    override fun toString(): String {
        return "MonetaryUseScope(scope=$scope, customerId='$customerId', amount=$amount)"
    }

}

class MonetaryRestrictionPermissionScope(
    private val scope: AuthScope,
    val customerId: String? = null,
    val maxAmount: Double = Double.NaN
                                        ) : AuthScope {
    override fun includes(useScope: Permission): Boolean {
        if (useScope !is MonetaryUseScope) return false
        if (customerId != null && useScope.customerId != customerId) return false
        if (maxAmount.isFinite() && useScope.amount > maxAmount) return false
        return true
    }

    override fun union(otherScope: AuthScope): AuthScope {
        require(otherScope != ANYSCOPE) { "Union with ANYSCOPE is insecure" }

        if (otherScope !is MonetaryRestrictionPermissionScope ||
            scope != otherScope.scope ||
            (customerId != null && otherScope.customerId != null && customerId != otherScope.customerId)
        ) return UnionPermissionScope(listOf(this, otherScope))
        val effectiveCustomerId = if (customerId != null) otherScope.customerId else null
        val effectiveMax = when {
            !maxAmount.isFinite() -> otherScope.maxAmount
            !otherScope.maxAmount.isFinite() -> maxAmount
            else -> maxOf(maxAmount, otherScope.maxAmount)
        }
        return MonetaryRestrictionPermissionScope(scope, effectiveCustomerId, effectiveMax)
    }

    override fun intersect(otherScope: AuthScope): AuthScope {
        when {
            otherScope is UnionPermissionScope -> return otherScope.intersect(this)
            otherScope !is MonetaryRestrictionPermissionScope -> return super.intersect(otherScope)
            scope != otherScope.scope -> return EMPTYSCOPE
            customerId != null && otherScope.customerId != null && customerId != otherScope.customerId -> return EMPTYSCOPE
            else -> {
                val effectiveCustomerId = customerId ?: otherScope.customerId
                val effectiveMax = when {
                    !maxAmount.isFinite() -> otherScope.maxAmount
                    !otherScope.maxAmount.isFinite() -> maxAmount
                    else -> minOf(maxAmount, otherScope.maxAmount)
                }
                if (effectiveMax <= 0.0) return EMPTYSCOPE
                return MonetaryRestrictionPermissionScope(scope, effectiveCustomerId, effectiveMax)
            }
        }
    }

    override val description: String
        get() = when {
            customerId == null && !maxAmount.isFinite() -> "${scope.description}()"
            customerId == null -> "${scope.description}(# <= $maxAmount)"
            !maxAmount.isFinite() -> "${scope.description}($customerId)"
            else -> "${scope.description}($customerId, # <= $maxAmount)"
        }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MonetaryRestrictionPermissionScope

        if (scope != other.scope) return false
        if (customerId != other.customerId) return false
        if (maxAmount != other.maxAmount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scope.hashCode()
        result = 31 * result + (customerId?.hashCode() ?: 0)
        result = 31 * result + maxAmount.hashCode()
        return result
    }


}
