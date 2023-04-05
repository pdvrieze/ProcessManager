package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UiService

interface RunnableUiService: UiService {
    fun loginBrowser(browser: Browser): PmaAuthToken
    override val serviceName: ServiceName<RunnableUiService>
    override val serviceInstanceId: ServiceId<RunnableUiService>
}

interface RunnableAutomatedService: AutomatedService
