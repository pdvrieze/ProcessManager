/*
 * Copyright (c) 2018.
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

package net.devrieze.util.security

import nl.adaptivity.util.multiplatform.PrincipalCompat

typealias Principal = PrincipalCompat

interface SecurityProvider {

    /**
     * Simple marker interface to represent a permission.
     */
    interface Permission// marker interface

    /**
     * The result of a permission request. A separate type to encourage permanent permissions.
     */
    enum class PermissionResult {
        /**
         * Permission has been granted.
         */
        GRANTED,
        /**
         * The user does not have permission for this operation.
         */
        DENIED,
        /**
         * There is no authenticated user.
         */
        UNAUTHENTICATED
    }


    /**
     * Result type for intermediate decisions.
     */
    enum class IntermediatePermissionDecision {
        ALWAYS_ALLOW,
        ALLOW,
        PASS,
        DENY,
        ALWAYS_DENY;

        infix fun and(otherProducer: () -> IntermediatePermissionDecision) = when(this) {
            ALWAYS_ALLOW, ALWAYS_DENY, DENY -> this
            ALLOW, PASS -> otherProducer()
        }

        infix fun andBool(otherProducer: () -> Boolean?): IntermediatePermissionDecision {
            return when(this) {
                ALWAYS_ALLOW, ALWAYS_DENY, DENY -> this
                ALLOW, PASS -> when (otherProducer()) {
                    true -> ALLOW
                    null -> this // This is a pass
                    else -> DENY
                }
            }
        }

        infix fun or(otherProducer: () -> IntermediatePermissionDecision) = when(this) {
            ALWAYS_ALLOW, ALWAYS_DENY, ALLOW -> this
            PASS, DENY -> otherProducer()
        }

        infix fun orBool(otherProducer: () -> Boolean?) = when(this) {
            ALWAYS_ALLOW, ALWAYS_DENY, ALLOW -> this
            PASS, DENY -> when(otherProducer()) {
                true -> ALLOW
                else -> this // even null either stays pass or deny
            }
        }

        fun toPermissionResult(): PermissionResult = when(this) {
            ALLOW, ALWAYS_ALLOW -> PermissionResult.GRANTED
            else -> PermissionResult.DENIED
        }

        companion object {
            fun from(boolean: Boolean?) = when(boolean) {
                true -> ALLOW
                else -> DENY
            }
        }
    }



    /**
     * Ensure that the user has the given permission.
     *
     * @param permission The permission to verify.
     * @param subject The user to check the permission against.
     * @throws PermissionDeniedException Thrown if the permission is denied.
     * @throws AuthenticationNeededException Thrown if the user has no permission.
     * @return The result. This should always be [PermissionResult.GRANTED].
     */
    fun ensurePermission(permission: Permission, subject: Principal?): PermissionResult

    /**
     * Ensure that the user has the given permission in relation to another user.
     *
     * @param permission The permission to verify.
     * @param subject The user to check the permission against.
     * @param objectPrincipal The principal that represents other part of the equation.
     * @throws PermissionDeniedException Thrown if the permission is denied.
     * @throws AuthenticationNeededException Thrown if the user has no permission.
     * @return The result. This should always be [PermissionResult.GRANTED].
     */
    fun ensurePermission(permission: Permission, subject: Principal?, objectPrincipal: Principal): PermissionResult

    /**
     * Ensure that the user has the given permission in relation to a given
     * object.
     *
     * @param permission The permission to verify.
     * @param subject The user to verify the permission for.
     * @param secureObject The object the permission applies to
     * @throws PermissionDeniedException Thrown if the permission is denied.
     * @throws AuthenticationNeededException Thrown if the user has no permission.
     * @return The result. This should always be [PermissionResult.GRANTED].
     */
    fun ensurePermission(permission: Permission, subject: Principal?, secureObject: SecuredObject<*>): PermissionResult

    /**
     * Determine whether the user has the permission given
     *
     * @param permission The permission to check
     * @param subject The user
     * @param secureObject The object of the activity
     * @return `true` if the user has the permission,
     * `false` if not.
     */
    fun hasPermission(permission: Permission, subject: Principal, secureObject: SecuredObject<*>): Boolean

    fun getPermission(permission: Permission, subject: Principal?, secureObject: SecuredObject<*>): PermissionResult

    /**
     * Determine whether the user has the given permission.
     *
     * @param permission The permission to verify.
     * @param subject The user to check the permission against.
     * @return `true` if the user has the permission,
     * `false` if not.
     */
    fun hasPermission(permission: Permission, subject: Principal): Boolean

    /**
     * Determine whether the given permission is held by the subject
     */
    fun getPermission(permission: Permission, subject: Principal?): PermissionResult

    /**
     * Determine whether the user has the given permission in relation to another
     * user.
     *
     * @param permission The permission to verify.
     * @param subject The user to check the permission against.
     * @param objectPrincipal The principal that represents other part of the equation.
     * @return `true` if the user has the permission,
     * `false` if not.
     */
    fun hasPermission(permission: Permission, subject: Principal, objectPrincipal: Principal): Boolean

    /**
     * Determine whether the subject has the given permission in relation to the specific object
     */
    fun getPermission(permission: Permission, subject: Principal?, objectPrincipal: Principal): PermissionResult

    companion object {

        /**
         * Special principal that represents the system.
         */
        @Deprecated("", ReplaceWith("net.devrieze.util.security.SYSTEMPRINCIPAL", "net.devrieze.util.security.SYSTEMPRINCIPAL"))
        val SYSTEMPRINCIPAL: RolePrincipal = net.devrieze.util.security.SYSTEMPRINCIPAL
    }

}
