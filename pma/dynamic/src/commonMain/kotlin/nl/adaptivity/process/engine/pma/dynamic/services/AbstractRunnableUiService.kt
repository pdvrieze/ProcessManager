package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

abstract class AbstractRunnableUiService : ServiceBase, RunnableUiService {

    constructor(authService: AuthService, serviceAuth: PmaIdSecretAuthInfo) : super(authService, serviceAuth)
    constructor(authService: AuthService, name: String) : super(authService, name)

    abstract override val serviceName: ServiceName<AbstractRunnableUiService>
    abstract override val serviceInstanceId: ServiceId<AbstractRunnableUiService>

    override fun loginBrowser(browser: Browser): PmaAuthToken {
        val authorization = browser.loginToService(authService, this)
        return authService.exchangeAuthCode(browser.auth, authorization)
    }

}

