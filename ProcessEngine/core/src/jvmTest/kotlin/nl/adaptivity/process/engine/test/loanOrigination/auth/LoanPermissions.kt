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

enum class LoanPermissions : AuthScope {
    INVALIDATE_ACTIVITY {
        override fun includes(scope: AuthScope): Boolean {
            return when (scope) {
                is ExtScope -> scope.extraData != "-1"
                else        -> scope == this
            }
        }
    },
    EVALUATE_LOAN,
    CREATE_CUSTOMER,
    QUERY_CUSTOMER_DATA,
    UPDATE_CUSTOMER_DATA,
    UPDATE_ACTIVITY_STATE,
    GET_CREDIT_REPORT,
    /** Identify the user as themselves */
    IDENTIFY,
    /** Create a token that allows a "user" to editify as task */
    CREATE_TASK_IDENTITY,
    GRANT_PERMISSION
    ;

    fun context(contextData: Any): AuthScope {
        return ExtScope(this, contextData.toString())
    }

    override fun includes(scope: AuthScope): Boolean {
        return this == scope
    }

    override val description: String
        get() = name
}

