package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo

abstract class AbstractRunnableAutomatedService : ServiceBase, RunnableAutomatedService {
    constructor(authService: AuthService, serviceAuth: PmaIdSecretAuthInfo) : super(authService, serviceAuth)
    constructor(authService: AuthService, name: String) : super(authService, name)

}
