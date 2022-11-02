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
import nl.adaptivity.process.engine.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanApplication

sealed class LoanPermissions : PermissionScope {
    object POST_TASK: LoanPermissions(), UseAuthScope
    object SIGN : LoanPermissions(), UseAuthScope
    object ACCEPT_TASK : LoanPermissions(), UseAuthScope {
        operator fun invoke(hNodeInstance: PNIHandle) =
            contextImpl(hNodeInstance)
    }

    object PRICE_LOAN : LoanPermissions() {
        fun context(customerId: String, amount: Double): UseAuthScope {
            return MonetaryUseScope(PRICE_LOAN, customerId, amount)
        }

        fun restrictTo(maxAmount: Double): PermissionScope {
            return MonetaryRestrictionPermissionScope(PRICE_LOAN, maxAmount = maxAmount)
        }

        fun restrictTo(customerId: String, maxAmount: Double = Double.NaN): PermissionScope {
            return MonetaryRestrictionPermissionScope(PRICE_LOAN, customerId, maxAmount)
        }
    }

    object PRINT_OFFER : LoanPermissions(), UseAuthScope
    object SIGN_LOAN : LoanPermissions() {
        fun context(customerId: String, offerAmount: Double): UseAuthScope {
            return MonetaryUseScope(SIGN_LOAN, customerId, offerAmount)
        }

        fun restrictTo(customerId: String? = null, maxAmount: Double = Double.NaN): PermissionScope {
            return MonetaryRestrictionPermissionScope(SIGN_LOAN, customerId, maxAmount)
        }

        override fun includes(useScope: UseAuthScope): Boolean = when (useScope) {
            is ExtScope<*> -> useScope.scope == this
            else           -> false
        }
    }

    object INVALIDATE_ACTIVITY : LoanPermissions() {
        fun context(hNodeInstance: PNIHandle) =
            UPDATE_ACTIVITY_STATE.contextImpl(hNodeInstance)

        override fun includes(useScope: UseAuthScope): Boolean {
            return when (useScope) {
                is ExtScope<*> -> Handle.invalid<Any>() != useScope.extraData
                else           -> false
            }
        }
    }

    object EVALUATE_LOAN : LoanPermissions() {
        fun context(application: LoanApplication) = context(application.customerId, application.amount)
        fun context(customerId: String, amount: Double) = MonetaryUseScope(EVALUATE_LOAN, customerId, amount)
        operator fun invoke(customerId: String? = null, maxAmount: Double = Double.NaN): PermissionScope {
            return MonetaryRestrictionPermissionScope(EVALUATE_LOAN, customerId, maxAmount)
        }
    }

    object CREATE_CUSTOMER : LoanPermissions(), UseAuthScope
    object QUERY_CUSTOMER_DATA : LoanPermissions() {
        operator fun invoke(customerId: String) = contextImpl(customerId)
    }

    object UPDATE_CUSTOMER_DATA : LoanPermissions(), UseAuthScope
    object UPDATE_ACTIVITY_STATE : LoanPermissions() {

        operator fun invoke(hNodeInstance: PNIHandle) =
            contextImpl(hNodeInstance)

        override fun includes(useScope: UseAuthScope): Boolean {
            return useScope is ExtScope<*> && useScope.scope==this
        }

        override fun intersect(otherScope: PermissionScope): PermissionScope? {
            return when {
                otherScope == UPDATE_ACTIVITY_STATE -> this
                otherScope is ExtScope<*> && otherScope.scope ==this -> otherScope
                else -> null
            }
        }
    }

    object GET_CREDIT_REPORT : LoanPermissions() {
        operator fun invoke(taxId: String) = contextImpl(taxId)
    }

    /** Identify the user as themselves */
    object IDENTIFY : LoanPermissions(), UseAuthScope

    /** Create a token that allows a "user" to identify as task */
    object CREATE_TASK_IDENTITY : LoanPermissions(), UseAuthScope


    object GRANT_GLOBAL_PERMISSION : LoanPermissions() {
        fun context(
            clientId: String,
            service: Service,
            scope: PermissionScope
                   ): UseAuthScope {
            return ContextScope(clientId, service.serviceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            service: Service? = null,
            scope: PermissionScope? = null
                      ): PermissionScope {
            return ContextScope(clientId, service?.serviceId, scope)
        }

        fun restrictTo(
            service: Service? = null,
            scope: PermissionScope? = null
                      ): PermissionScope {
            return ContextScope(null, service?.serviceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is ContextScope) return true
            if (useScope is GRANT_ACTIVITY_PERMISSION.ContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: PermissionScope): PermissionScope? {
            if (otherScope is ContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: PermissionScope): PermissionScope {
            if (otherScope is ContextScope) return this
            return super.union(otherScope)
        }

        data class ContextScope(val clientId: String?, val serviceId: String?, val childScope: PermissionScope? = null) :
            UseAuthScope, PermissionScope {
            override fun includes(useScope: UseAuthScope): Boolean = when {
                useScope is GRANT_ACTIVITY_PERMISSION.ContextScope &&
                    useScope.childScope!=null &&
                    (clientId == null || clientId == useScope.clientId) &&
                    (serviceId == null || serviceId != useScope.serviceId) &&
                    (childScope == null || childScope.intersect(useScope.childScope) == useScope.childScope)
                                   -> true

                useScope !is ContextScope ||
                    useScope.childScope == null ||
                    clientId != null && clientId != useScope.clientId ||
                    serviceId != null && serviceId != useScope.serviceId
                                   -> false

                childScope == null -> true

                else               -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            fun toActivityPermission(taskHandle: PNIHandle): PermissionScope {
                return GRANT_ACTIVITY_PERMISSION.restrictTo(taskHandle, clientId, serviceId, childScope)
            }

            override fun intersect(otherScope: PermissionScope): PermissionScope? {
                if (otherScope == GRANT_GLOBAL_PERMISSION) {
                    return this
                } else if (otherScope is GRANT_ACTIVITY_PERMISSION.ContextScope) {
                    return toActivityPermission(otherScope.taskInstanceHandle).intersect(otherScope)
                } else if (otherScope is UnionPermissionScope) {
                    return otherScope.intersect(this)
                } else if (otherScope !is ContextScope) return  null

                val effectiveClient = when {
                    clientId == null -> otherScope.clientId
                    otherScope.clientId == null -> otherScope.clientId
                    clientId == otherScope.clientId -> clientId
                    else -> return null
                }

                val effectiveService = when {
                    serviceId == null               -> otherScope.serviceId
                    otherScope.serviceId == null      -> otherScope.serviceId
                    serviceId == otherScope.serviceId -> serviceId
                    else                            -> return null
                }

                val effectiveScope = when {
                    childScope == null       -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else                     -> childScope.intersect(otherScope.childScope) ?: return null
                }
                return ContextScope(effectiveClient, effectiveService, effectiveScope)
            }

            override fun union(otherScope: PermissionScope): PermissionScope {
                if (otherScope == GRANT_GLOBAL_PERMISSION) return otherScope
                else if (otherScope !is ContextScope) return UnionPermissionScope(listOf(this, otherScope))

                val effectiveClient = when {
                    clientId == null || otherScope.clientId==null -> null
                    clientId == otherScope.clientId -> clientId
                    else -> return UnionPermissionScope(listOf(this, otherScope))
                }

                val effectiveService = when {
                    serviceId == null               -> null
                    otherScope.serviceId == null      -> null
                    serviceId == otherScope.serviceId -> serviceId
                    else                            -> return UnionPermissionScope(listOf(this, otherScope))
                }

                val effectiveScope = when {
                    childScope == null       -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else                     -> childScope.union(otherScope.childScope)
                }
                return ContextScope(effectiveClient, effectiveService, effectiveScope)
            }

            override val description: String
                get() = "GRANT_GLOBAL_PERMISSION(${clientId?:"<anyClient>"}.${serviceId?:"<anyService>"}.${childScope?.description?:"*"})"

            override fun toString(): String {
                return description
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ContextScope

                if (clientId != other.clientId) return false
                if (serviceId != other.serviceId) return false
                if (childScope != other.childScope) return false

                return true
            }

            override fun hashCode(): Int {
                var result = clientId?.hashCode() ?: 0
                result = 31 * result + (serviceId?.hashCode() ?: 0)
                result = 31 * result + (childScope?.hashCode() ?: 0)
                return result
            }


        }
    }

    object GRANT_ACTIVITY_PERMISSION : LoanPermissions() {
        fun context(
            taskHandle: PNIHandle,
            clientId: String,
            service: Service,
            scope: PermissionScope,
        ): UseAuthScope {
            return ContextScope(taskHandle, clientId, service.serviceId, scope)
        }

        fun restrictTo(
            taskHandle: PNIHandle,
            clientId: String? = null,
            service: Service? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return ContextScope(taskHandle, clientId, service?.serviceId, scope)
        }

        fun restrictTo(
            taskHandle: PNIHandle,
            clientId: String? = null,
            serviceId: String? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return ContextScope(taskHandle, clientId, serviceId, scope)
        }

        fun restrictTo(
            taskHandle: PNIHandle,
            service: Service? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return ContextScope(taskHandle, null, service?.serviceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is ContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: PermissionScope): PermissionScope? {
            if (otherScope is ContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: PermissionScope): PermissionScope {
            if (otherScope is ContextScope) return this
            return super.union(otherScope)
        }

        data class ContextScope(val taskInstanceHandle: PNIHandle, val clientId: String?, val serviceId: String?, val childScope: PermissionScope? = null) :
            UseAuthScope, PermissionScope {

            init {
                assert(taskInstanceHandle.isValid)
            }

            override fun includes(useScope: UseAuthScope): Boolean = when {
                useScope !is ContextScope ||
                    useScope.childScope == null ||
                    !useScope.taskInstanceHandle.isValid ||
                    useScope.taskInstanceHandle != taskInstanceHandle ||
                    clientId != null && clientId != useScope.clientId ||
                    serviceId != null && serviceId != useScope.serviceId
                                   -> false
                childScope == null -> true
                else               -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            override fun intersect(otherScope: PermissionScope): PermissionScope? {
                if (otherScope == GRANT_ACTIVITY_PERMISSION) {
                    return this
                } else if (otherScope is GRANT_GLOBAL_PERMISSION.ContextScope) {
                    return intersect(otherScope.toActivityPermission(taskInstanceHandle))
                } else if (otherScope is UnionPermissionScope) {
                    return otherScope.intersect(this)
                } else if (otherScope !is ContextScope ||
                    taskInstanceHandle!=otherScope.taskInstanceHandle) return null

                val effectiveClient = when {
                    clientId == null -> otherScope.clientId
                    otherScope.clientId == null -> otherScope.clientId
                    clientId == otherScope.clientId -> clientId
                    else -> return null
                }

                val effectiveService = when {
                    serviceId == null               -> otherScope.serviceId
                    otherScope.serviceId == null      -> otherScope.serviceId
                    serviceId == otherScope.serviceId -> serviceId
                    else                            -> return null
                }

                val effectiveScope = when {
                    childScope == null       -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else                     -> childScope.intersect(otherScope.childScope) ?: return null
                }
                return ContextScope(taskInstanceHandle, effectiveClient, effectiveService, effectiveScope)
            }

            override fun union(otherScope: PermissionScope): PermissionScope {
                if (otherScope == GRANT_ACTIVITY_PERMISSION) return otherScope
                else if (otherScope !is ContextScope ||
                        taskInstanceHandle != otherScope.taskInstanceHandle
                    ) return UnionPermissionScope(listOf(this, otherScope))

                val effectiveClient = when {
                    clientId == null || otherScope.clientId==null -> null
                    clientId == otherScope.clientId -> clientId
                    else -> return UnionPermissionScope(listOf(this, otherScope))
                }

                val effectiveService = when {
                    serviceId == null               -> null
                    otherScope.serviceId == null      -> null
                    serviceId == otherScope.serviceId -> serviceId
                    else                            -> return UnionPermissionScope(listOf(this, otherScope))
                }

                val effectiveScope = when {
                    childScope == null       -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else                     -> childScope.union(otherScope.childScope)
                }
                return ContextScope(taskInstanceHandle, effectiveClient, effectiveService, effectiveScope)
            }

            override val description: String
                get() = "GRANT_ACTIVITY_PERMISSION($taskInstanceHandle, ${clientId?:"<anyClient>"}->${serviceId?:"<anyService>"}.${childScope?.description?:"*"})"

            override fun toString(): String {
                return description
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ContextScope

                if (clientId != other.clientId) return false
                if (serviceId != other.serviceId) return false
                if (childScope != other.childScope) return false

                return true
            }

            override fun hashCode(): Int {
                var result = clientId?.hashCode() ?: 0
                result = 31 * result + (serviceId?.hashCode() ?: 0)
                result = 31 * result + (childScope?.hashCode() ?: 0)
                return result
            }

        }
    }

    object DELEGATED_PERMISSION : LoanPermissions() {
        fun context(
            clientId: String,
            service: Service,
            scope: PermissionScope,
        ): UseAuthScope {
            return DelegateContextScope(service.serviceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            service: Service? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return DelegateContextScope(service?.serviceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            serviceId: String? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return DelegateContextScope(serviceId, scope)
        }

        fun restrictTo(
            service: Service? = null,
            scope: PermissionScope? = null,
        ): PermissionScope {
            return DelegateContextScope(service?.serviceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is DelegateContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: PermissionScope): PermissionScope? {
            if (otherScope is DelegateContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: PermissionScope): PermissionScope {
            if (otherScope is DelegateContextScope) return this
            return super.union(otherScope)
        }

        data class DelegateContextScope(val serviceId: String?, val childScope: PermissionScope? = null) :
            UseAuthScope, PermissionScope {

            override fun includes(useScope: UseAuthScope): Boolean = when {
                useScope !is DelegateContextScope ||
                    useScope.childScope == null ||
                    serviceId != null && serviceId != useScope.serviceId
                                   -> false
                childScope == null -> true
                else               -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            override fun intersect(otherScope: PermissionScope): PermissionScope? {
                when (otherScope) {
                    DELEGATED_PERMISSION -> return this
                    is UnionPermissionScope -> return otherScope.intersect(this)
                    !is DelegateContextScope -> return null

                    else -> {

                        val effectiveService = when {
                            serviceId == null               -> otherScope.serviceId
                            otherScope.serviceId == null      -> otherScope.serviceId
                            serviceId == otherScope.serviceId -> serviceId
                            else                            -> return null
                        }

                        val effectiveScope = when {
                            childScope == null       -> otherScope.childScope
                            otherScope.childScope == null -> otherScope.childScope
                            else                     -> childScope.intersect(otherScope.childScope) ?: return null
                        }
                        return DelegateContextScope(effectiveService, effectiveScope)
                    }
                }

            }

            override fun union(otherScope: PermissionScope): PermissionScope {
                when (otherScope) {
                    DELEGATED_PERMISSION -> return otherScope
                    !is DelegateContextScope -> return UnionPermissionScope(listOf(this, otherScope))
                    else -> {

                        val effectiveService = when {
                            serviceId == null               -> null
                            otherScope.serviceId == null      -> null
                            serviceId == otherScope.serviceId -> serviceId
                            else                            -> return UnionPermissionScope(listOf(this, otherScope))
                        }

                        val effectiveScope = when {
                            childScope == null       -> otherScope.childScope
                            otherScope.childScope == null -> otherScope.childScope
                            else                     -> childScope.union(otherScope.childScope)
                        }
                        return DelegateContextScope(effectiveService, effectiveScope)
                    }
                }
            }

            override val description: String
                get() = "DELEGATED_PERMISSION(->${serviceId ?: "<anyService>"}.${childScope?.description ?: "*"})"

            override fun toString(): String {
                return description
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as DelegateContextScope

                if (serviceId != other.serviceId) return false
                if (childScope != other.childScope) return false

                return true
            }

            override fun hashCode(): Int {
                var result = (serviceId?.hashCode() ?: 0)
                result = 31 * result + (childScope?.hashCode() ?: 0)
                return result
            }

        }
    }

    object OPEN_ACCOUNT : LoanPermissions() {
        operator fun invoke(customerId: String) = contextImpl(customerId)
    }


    protected fun <V> contextImpl(contextData: V): ExtScope<V> {
        return ExtScope(this, contextData)
    }

    override fun includes(useScope: UseAuthScope): Boolean {
        return this == useScope
    }

    override fun intersect(otherScope: PermissionScope): PermissionScope? {
        if (otherScope is UnionPermissionScope) return otherScope.intersect(this)
        return if (otherScope is UseAuthScope && includes(otherScope)) otherScope else null
    }

    override fun union(otherScope: PermissionScope): PermissionScope = when (otherScope) {
        this -> this
        else -> UnionPermissionScope(listOf(this, otherScope))
    }

    override val description: String
        get() = javaClass.simpleName.substringAfterLast('.')

    override fun toString(): String = description
}

class MonetaryUseScope(val scope: LoanPermissions, val customerId: String, val amount: Double) : UseAuthScope {
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
    private val scope: LoanPermissions,
    val customerId: String? = null,
    val maxAmount: Double = Double.NaN
                                        ) : PermissionScope {
    override fun includes(useScope: UseAuthScope): Boolean {
        if (useScope !is MonetaryUseScope) return false
        if (customerId != null && useScope.customerId != customerId) return false
        if (maxAmount.isFinite() && useScope.amount > maxAmount) return false
        return true
    }

    override fun union(otherScope: PermissionScope): PermissionScope {
        if (otherScope !is MonetaryRestrictionPermissionScope ||
            scope != otherScope.scope ||
            (customerId != null && otherScope.customerId != null && customerId != otherScope.customerId)
        ) return UnionPermissionScope(listOf(this, otherScope))
        val effectiveCustomerId = if (customerId != null) otherScope.customerId else null
        val effectiveMax = when {
            !maxAmount.isFinite()            -> otherScope.maxAmount
            !otherScope.maxAmount.isFinite() -> maxAmount
            else                             -> maxOf(maxAmount, otherScope.maxAmount)
        }
        return MonetaryRestrictionPermissionScope(scope, effectiveCustomerId, effectiveMax)
    }

    override fun intersect(otherScope: PermissionScope): PermissionScope? {
        if (otherScope is UnionPermissionScope) return otherScope.intersect(this)
        if (otherScope !is MonetaryRestrictionPermissionScope) return null
        if (scope != otherScope.scope) return null
        if (customerId != null && otherScope.customerId != null && customerId != otherScope.customerId) return null
        val effectiveCustomerId = customerId ?: otherScope.customerId
        val effectiveMax = when {
            !maxAmount.isFinite()            -> otherScope.maxAmount
            !otherScope.maxAmount.isFinite() -> maxAmount
            else                             -> minOf(maxAmount, otherScope.maxAmount)
        }
        if (effectiveMax <= 0.0) return null
        return MonetaryRestrictionPermissionScope(scope, effectiveCustomerId, effectiveMax)
    }

    override val description: String
        get() = when {
            customerId == null && !maxAmount.isFinite() -> "${scope.description}()"
            customerId == null                          -> "${scope.description}(# <= $maxAmount)"
            !maxAmount.isFinite()                       -> "${scope.description}($customerId)"
            else                                        -> "${scope.description}($customerId, # <= $maxAmount)"
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
