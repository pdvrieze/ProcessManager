package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.IdSecretAuthInfo

abstract class AbstractRunnableAutomatedService(
    authService: AuthService,
    serviceAuth: IdSecretAuthInfo
) : ServiceBase(authService, serviceAuth), RunnableAutomatedService
