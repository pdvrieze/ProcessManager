package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo

abstract class AbstractRunnableAutomatedService(
    authService: AuthService,
    serviceAuth: PmaIdSecretAuthInfo
) : ServiceBase(authService, serviceAuth), RunnableAutomatedService
