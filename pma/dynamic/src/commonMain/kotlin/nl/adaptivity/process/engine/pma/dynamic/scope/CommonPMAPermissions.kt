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

package nl.adaptivity.process.engine.pma.dynamic.scope

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.pma.ExtScope
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance

sealed class CommonPMAPermissions : AuthScope {
    object POST_TASK : CommonPMAPermissions(), UseAuthScope
    object ACCEPT_TASK : CommonPMAPermissions(), UseAuthScope {
        operator fun invoke(hNodeInstance: Handle<SecureProcessNodeInstance>) =
            contextImpl(hNodeInstance)
    }


    object INVALIDATE_ACTIVITY : CommonPMAPermissions() {
        fun context(hNodeInstance: Handle<SecureProcessNodeInstance>) =
            UPDATE_ACTIVITY_STATE.contextImpl(hNodeInstance)

        override fun includes(useScope: UseAuthScope): Boolean {
            return when (useScope) {
                is ExtScope<*> -> Handle.invalid<Any>() != useScope.extraData
                else -> false
            }
        }
    }


    object UPDATE_ACTIVITY_STATE : CommonPMAPermissions() {

        operator fun invoke(hNodeInstance: Handle<SecureProcessNodeInstance>) =
            contextImpl(hNodeInstance)

        override fun includes(useScope: UseAuthScope): Boolean {
            return useScope is ExtScope<*> && useScope.scope == this
        }

        override fun intersect(otherScope: AuthScope): AuthScope? {
            return when {
                otherScope == UPDATE_ACTIVITY_STATE -> this
                otherScope is ExtScope<*> && otherScope.scope == this -> otherScope
                else -> null
            }
        }
    }


    /** Identify the user as themselves */
    object IDENTIFY : CommonPMAPermissions(), UseAuthScope

    /** Create a token that allows a "user" to identify as task */
    object CREATE_TASK_IDENTITY : CommonPMAPermissions(), UseAuthScope


    object GRANT_GLOBAL_PERMISSION : CommonPMAPermissions() {
        fun context(
            clientId: String,
            service: Service,
            scope: AuthScope
        ): UseAuthScope {
            return ContextScope(clientId, service.serviceInstanceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            service: Service? = null,
            scope: AuthScope? = null
        ): AuthScope {
            return ContextScope(clientId, service?.serviceInstanceId, scope)
        }

        fun restrictTo(
            service: Service? = null,
            scope: AuthScope? = null
        ): AuthScope {
            return ContextScope(null, service?.serviceInstanceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is ContextScope) return true
            if (useScope is GRANT_ACTIVITY_PERMISSION.ContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: AuthScope): AuthScope? {
            if (otherScope is ContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: AuthScope): AuthScope {
            if (otherScope is ContextScope) return this
            return super.union(otherScope)
        }

        data class ContextScope(
            val clientId: String?,
            val serviceId: ServiceId<*>?,
            val childScope: AuthScope? = null
        ) : UseAuthScope, AuthScope {
            override fun includes(useScope: UseAuthScope): Boolean = when {
                useScope is GRANT_ACTIVITY_PERMISSION.ContextScope &&
                    useScope.childScope != null &&
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

                else -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            fun toActivityPermission(taskHandle: Handle<SecureProcessNodeInstance>): AuthScope {
                return GRANT_ACTIVITY_PERMISSION.restrictTo(taskHandle, clientId, serviceId, childScope)
            }

            override fun intersect(otherScope: AuthScope): AuthScope? {
                if (otherScope == GRANT_GLOBAL_PERMISSION) {
                    return this
                } else if (otherScope is GRANT_ACTIVITY_PERMISSION.ContextScope) {
                    return toActivityPermission(otherScope.taskInstanceHandle).intersect(otherScope)
                } else if (otherScope is UnionPermissionScope) {
                    return otherScope.intersect(this)
                } else if (otherScope !is ContextScope) return null

                val effectiveClient = when {
                    clientId == null -> otherScope.clientId
                    otherScope.clientId == null -> otherScope.clientId
                    clientId == otherScope.clientId -> clientId
                    else -> return null
                }

                val effectiveService = when {
                    serviceId == null -> otherScope.serviceId
                    otherScope.serviceId == null -> otherScope.serviceId
                    serviceId == otherScope.serviceId -> serviceId
                    else -> return null
                }

                val effectiveScope = when {
                    childScope == null -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else -> childScope.intersect(otherScope.childScope) ?: return null
                }
                return ContextScope(effectiveClient, effectiveService, effectiveScope)
            }

            override fun union(otherScope: AuthScope): AuthScope {
                if (otherScope == GRANT_GLOBAL_PERMISSION) return otherScope
                else if (otherScope !is ContextScope) return UnionPermissionScope(
                    listOf(this, otherScope)
                )

                val effectiveClient = when {
                    clientId == null || otherScope.clientId == null -> null
                    clientId == otherScope.clientId -> clientId
                    else -> return UnionPermissionScope(
                        listOf(
                            this,
                            otherScope
                        )
                    )
                }

                val effectiveService = when {
                    serviceId == null -> null
                    otherScope.serviceId == null -> null
                    serviceId == otherScope.serviceId -> serviceId
                    else -> return UnionPermissionScope(
                        listOf(
                            this,
                            otherScope
                        )
                    )
                }

                val effectiveScope = when {
                    childScope == null -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else -> childScope.union(otherScope.childScope)
                }
                return ContextScope(effectiveClient, effectiveService, effectiveScope)
            }

            override val description: String
                get() = "GRANT_GLOBAL_PERMISSION(${clientId ?: "<anyClient>"}.${serviceId ?: "<anyService>"}.${childScope?.description ?: "*"})"

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

    object GRANT_ACTIVITY_PERMISSION : CommonPMAPermissions() {
        fun context(
            taskHandle: Handle<SecureProcessNodeInstance>,
            clientId: String,
            service: Service,
            scope: AuthScope,
        ): UseAuthScope {
            return ContextScope(taskHandle, clientId, service.serviceInstanceId, scope)
        }

        fun context(
            taskHandle: Handle<SecureProcessNodeInstance>,
            clientId: String,
            serviceId: ServiceId<*>,
            scope: AuthScope,
        ): UseAuthScope {
            return ContextScope(taskHandle, clientId, serviceId, scope)
        }

        fun restrictTo(
            taskHandle: Handle<SecureProcessNodeInstance>,
            clientId: String? = null,
            service: Service? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return ContextScope(taskHandle, clientId, service?.serviceInstanceId, scope)
        }

        fun restrictTo(
            taskHandle: Handle<SecureProcessNodeInstance>,
            clientId: String? = null,
            serviceId: ServiceId<*>? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return ContextScope(taskHandle, clientId, serviceId, scope)
        }

        fun restrictTo(
            taskHandle: Handle<SecureProcessNodeInstance>,
            service: Service? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return ContextScope(taskHandle, null, service?.serviceInstanceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is ContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: AuthScope): AuthScope? {
            if (otherScope is ContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: AuthScope): AuthScope {
            if (otherScope is ContextScope) return this
            return super.union(otherScope)
        }

        data class ContextScope(
            val taskInstanceHandle: Handle<SecureProcessNodeInstance>,
            val clientId: String?,
            val serviceId: ServiceId<*>?,
            val childScope: AuthScope? = null
        ) :
            UseAuthScope, AuthScope {

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
                else -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            override fun intersect(otherScope: AuthScope): AuthScope? {
                if (otherScope == GRANT_ACTIVITY_PERMISSION) {
                    return this
                } else if (otherScope is GRANT_GLOBAL_PERMISSION.ContextScope) {
                    return intersect(otherScope.toActivityPermission(taskInstanceHandle))
                } else if (otherScope is UnionPermissionScope) {
                    return otherScope.intersect(this)
                } else if (otherScope !is ContextScope ||
                    taskInstanceHandle != otherScope.taskInstanceHandle
                ) return null

                val effectiveClient = when {
                    clientId == null -> otherScope.clientId
                    otherScope.clientId == null -> otherScope.clientId
                    clientId == otherScope.clientId -> clientId
                    else -> return null
                }

                val effectiveService = when {
                    serviceId == null -> otherScope.serviceId
                    otherScope.serviceId == null -> otherScope.serviceId
                    serviceId == otherScope.serviceId -> serviceId
                    else -> return null
                }

                val effectiveScope = when {
                    childScope == null -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else -> childScope.intersect(otherScope.childScope) ?: return null
                }
                return ContextScope(taskInstanceHandle, effectiveClient, effectiveService, effectiveScope)
            }

            override fun union(otherScope: AuthScope): AuthScope {
                if (otherScope == GRANT_ACTIVITY_PERMISSION) return otherScope
                else if (otherScope !is ContextScope ||
                    taskInstanceHandle != otherScope.taskInstanceHandle
                ) return UnionPermissionScope(listOf(this, otherScope))

                val effectiveClient = when {
                    clientId == null || otherScope.clientId == null -> null
                    clientId == otherScope.clientId -> clientId
                    else -> return UnionPermissionScope(
                        listOf(
                            this,
                            otherScope
                        )
                    )
                }

                val effectiveService = when {
                    serviceId == null -> null
                    otherScope.serviceId == null -> null
                    serviceId == otherScope.serviceId -> serviceId
                    else -> return UnionPermissionScope(
                        listOf(
                            this,
                            otherScope
                        )
                    )
                }

                val effectiveScope = when {
                    childScope == null -> otherScope.childScope
                    otherScope.childScope == null -> otherScope.childScope
                    else -> childScope.union(otherScope.childScope)
                }
                return ContextScope(taskInstanceHandle, effectiveClient, effectiveService, effectiveScope)
            }

            override val description: String
                get() = "GRANT_ACTIVITY_PERMISSION($taskInstanceHandle, ${clientId ?: "<anyClient>"}->${serviceId ?: "<anyService>"}.${childScope?.description ?: "*"})"

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

    object DELEGATED_PERMISSION : CommonPMAPermissions() {
        fun context(
            clientId: String,
            serviceId: ServiceId<*>,
            scope: AuthScope,
        ): UseAuthScope {
            return DelegateContextScope(serviceId, scope)
        }
        fun context(
            clientId: String,
            service: Service,
            scope: AuthScope,
        ): UseAuthScope {
            return DelegateContextScope(service.serviceInstanceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            service: Service? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return DelegateContextScope(service?.serviceInstanceId, scope)
        }

        fun restrictTo(
            clientId: String? = null,
            serviceId: ServiceId<*>? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return DelegateContextScope(serviceId, scope)
        }

        fun restrictTo(
            service: Service? = null,
            scope: AuthScope? = null,
        ): AuthScope {
            return DelegateContextScope(service?.serviceInstanceId, scope)
        }

        override fun includes(useScope: UseAuthScope): Boolean {
            if (useScope is DelegateContextScope) return true
            return super.includes(useScope)
        }

        override fun intersect(otherScope: AuthScope): AuthScope? {
            if (otherScope is DelegateContextScope) return otherScope
            return super.intersect(otherScope)
        }

        override fun union(otherScope: AuthScope): AuthScope {
            if (otherScope is DelegateContextScope) return this
            return super.union(otherScope)
        }

        data class DelegateContextScope(
            val serviceId: ServiceId<*>?,
            val childScope: AuthScope? = null
        ) : UseAuthScope, AuthScope {

            override fun includes(useScope: UseAuthScope): Boolean = when {
                useScope !is DelegateContextScope ||
                    useScope.childScope == null ||
                    serviceId != null && serviceId != useScope.serviceId
                -> false

                childScope == null -> true
                else -> childScope.intersect(useScope.childScope) == useScope.childScope
            }

            override fun intersect(otherScope: AuthScope): AuthScope? {
                when (otherScope) {
                    DELEGATED_PERMISSION -> return this
                    is UnionPermissionScope -> return otherScope.intersect(this)
                    !is DelegateContextScope -> return null

                    else -> {

                        val effectiveService = when {
                            serviceId == null -> otherScope.serviceId
                            otherScope.serviceId == null -> otherScope.serviceId
                            serviceId == otherScope.serviceId -> serviceId
                            else -> return null
                        }

                        val effectiveScope = when {
                            childScope == null -> otherScope.childScope
                            otherScope.childScope == null -> otherScope.childScope
                            else -> childScope.intersect(otherScope.childScope) ?: return null
                        }
                        return DelegateContextScope(effectiveService, effectiveScope)
                    }
                }

            }

            override fun union(otherScope: AuthScope): AuthScope {
                when (otherScope) {
                    DELEGATED_PERMISSION -> return otherScope
                    !is DelegateContextScope -> return UnionPermissionScope(
                        listOf(this, otherScope)
                    )

                    else -> {

                        val effectiveService = when {
                            serviceId == null -> null
                            otherScope.serviceId == null -> null
                            serviceId == otherScope.serviceId -> serviceId
                            else -> return UnionPermissionScope(
                                listOf(
                                    this,
                                    otherScope
                                )
                            )
                        }

                        val effectiveScope = when {
                            childScope == null -> otherScope.childScope
                            otherScope.childScope == null -> otherScope.childScope
                            else -> childScope.union(otherScope.childScope)
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

    protected fun <V> contextImpl(contextData: V): ExtScope<V> {
        return ExtScope(this, contextData)
    }

    override fun includes(useScope: UseAuthScope): Boolean {
        return this == useScope
    }

    override fun intersect(otherScope: AuthScope): AuthScope? {
        if (otherScope is UnionPermissionScope) return otherScope.intersect(this)
        return if (otherScope is UseAuthScope && includes(otherScope)) otherScope else null
    }

    override fun union(otherScope: AuthScope): AuthScope =
        when (otherScope) {
            this -> this
            else -> UnionPermissionScope(listOf(this, otherScope))
        }

    override val description: String
        get() = javaClass.simpleName.substringAfterLast('.')

    override fun toString(): String = description
}
