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

package nl.adaptivity.process.engine.test.loanOrigination.systems

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.lang.IllegalStateException
import java.security.Principal

class TaskList(authService:AuthService, clientAuth: IdSecretAuthInfo, val principal: Principal): ServiceImpl(authService, clientAuth) {
    val nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>? get() = taskIdentityToken?.nodeInstanceHandle

    private val tokens = mutableListOf<AuthToken>()

    private var taskIdentityToken: AuthToken? = null
        private set

    fun registerToken(authorizationCode: AuthorizationCode): AuthToken {
        assert(taskIdentityToken == null)
        val token = authService.getAuthToken(serviceAuth, authorizationCode)
        tokens.add(token)


        return authService.createTaskIdentityToken(serviceAuth, token.nodeInstanceHandle, principal).also {
            taskIdentityToken = it
        }
    }

    fun finishTask(nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>) {
        if(nodeInstanceHandle!=taskIdentityToken?.nodeInstanceHandle) {
            throw IllegalStateException("task identity token is not for active task")
        }
        taskIdentityToken = null
        tokens.removeIf { it.nodeInstanceHandle == nodeInstanceHandle }
    }

    fun contextImpl(browser: Browser): Context = ContextImpl(browser)

    interface Context {
        fun loginToService(service: ServiceImpl): AuthToken
    }

    private inner class ContextImpl(val browser: Browser) : Context {
        override fun loginToService(service: ServiceImpl): AuthToken {
            return browser.loginToService(service)
//            return authService.getAuthTokenDirect(browser.user, taskIdentityToken!!, service, ANYSCOPE)
        }

    }
}
