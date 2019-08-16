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

import nl.adaptivity.process.engine.test.loanOrigination.Random
import nl.adaptivity.process.engine.test.loanOrigination.systems.AuthService
import nl.adaptivity.process.engine.test.loanOrigination.systems.Browser
import kotlin.random.nextULong

interface Service {
    val serviceId: String
}

open class ServiceImpl(protected val authService: AuthService, protected val serviceAuth: IdSecretAuthInfo) : Service {

    override val serviceId: String get() = serviceAuth.principal.name

    @UseExperimental(ExperimentalUnsignedTypes::class)
    constructor(authService: AuthService, name: String): this(authService, authService.registerClient(name, Random.nextULong().toString(16)))

    protected fun validateAuthInfo(authInfo: AuthInfo, scope: UseAuthScope) {
        authService.validateAuthInfo(this, authInfo, scope)
    }

    fun loginBrowser(browser: Browser): AuthToken {
        return browser.loginToService(authService, this)


    }
}
