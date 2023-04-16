package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UiService

interface RunnableUiService: UiService {
    val authServiceClient: DefaultAuthServiceClient
    override val serviceName: ServiceName<RunnableUiService>
    override val serviceInstanceId: ServiceId<RunnableUiService>


    fun loginBrowser(browser: Browser): PmaAuthToken {
        val authorization = browser.loginToService(authServiceClient.authService, this)
        return authServiceClient.authService.exchangeAuthCode(browser.auth, authorization)
    }

}

interface RunnableAutomatedService: AutomatedService
