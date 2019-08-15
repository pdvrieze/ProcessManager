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
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanApplication

sealed class LoanPermissions : AuthScope {
    object PRICE_LOAN: LoanPermissions() {
        fun context(amount: Double): ExtScope<Double> {
            return contextImpl(amount)
        }
    }
    object PRINT_OFFER: LoanPermissions()
    object SIGN_LOAN: LoanPermissions() {
        fun context(customerId: String, offerAmount: Double): AuthScope {
            return contextImpl(customerId)
        }

        override fun includes(scope: AuthScope): Boolean = when (scope) {
            is ExtScope<*> -> scope.scope == this
            else -> scope == this
        }
    }
    object INVALIDATE_ACTIVITY: LoanPermissions() {
        fun context(hNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>) =
            UPDATE_ACTIVITY_STATE.contextImpl(hNodeInstance)

        override fun includes(scope: AuthScope): Boolean {
            return when (scope) {
                is ExtScope<*> -> getInvalidHandle<Any>() != scope.extraData
                else              -> scope == this
            }
        }
    }
    object EVALUATE_LOAN: LoanPermissions() {
        fun context(application: LoanApplication): AuthScope = context(application.customerId, application.amount)
        fun context(customerId: String, amount: Double): AuthScope = contextImpl(customerId)
    }
    object CREATE_CUSTOMER: LoanPermissions()
    object QUERY_CUSTOMER_DATA: LoanPermissions() {
        fun context(customerId: String): AuthScope = contextImpl(customerId)
    }

    object UPDATE_CUSTOMER_DATA: LoanPermissions()
    object UPDATE_ACTIVITY_STATE: LoanPermissions() {

        fun context(hNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>) =
            contextImpl(hNodeInstance)

    }
    object GET_CREDIT_REPORT: LoanPermissions() {
        fun context(taxId: String) = contextImpl(taxId)
    }
    /** Identify the user as themselves */
    object IDENTIFY: LoanPermissions()
    /** Create a token that allows a "user" to editify as task */
    object CREATE_TASK_IDENTITY: LoanPermissions()
    object GRANT_PERMISSION: LoanPermissions() {
        fun context(service: Service, childScope: AuthScope): AuthScope {
            val serviceId = service.serviceId
            return ContextScope(serviceId, childScope)
        }

        class ContextScope(
            private val serviceId: String,
            private val childScope: AuthScope
                          ) : AuthScope {
                            override fun includes(scope: AuthScope): Boolean = when(scope){
                                is ContextScope -> serviceId == scope.serviceId && childScope.includes(scope.childScope)
                                else -> false
                            }

                            override val description: String
                                get() = "GRANT_PERMISSION($serviceId.${childScope.description})"
                        }
    }

    object OPEN_ACCOUNT: LoanPermissions() {
        fun context(customerId: String): AuthScope = QUERY_CUSTOMER_DATA.contextImpl(customerId)
    }


    protected fun <V> contextImpl(contextData: V): ExtScope<V> {
        return ExtScope(this, contextData)
    }

    override fun includes(scope: AuthScope): Boolean {
        return this == scope
    }


    override val description: String
        get() = javaClass.simpleName.substringAfterLast('.')
}

