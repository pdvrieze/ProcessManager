package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.models.ServiceName
import java.util.logging.Logger

abstract class AbstractRunnableUiService<S: AbstractRunnableUiService<S>> : ServiceBase<S>, RunnableUiService {

    constructor(
        authService: AuthService,
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        logger: Logger = authService.logger
    ) : super(authService, serviceAuth, serviceName, logger)

    constructor(authServiceClient: DefaultAuthServiceClient, serviceName: ServiceName<S>, logger: Logger) : super(
        authServiceClient,
        serviceName,
        logger
    )

    constructor(authService: AuthService, adminAuth: PmaAuthInfo, serviceName: ServiceName<S>, logger: Logger = authService.logger) : super(
        authService,
        adminAuth,
        serviceName,
        logger
    )

    override fun loginBrowser(browser: Browser): PmaAuthToken {
        val authorization = browser.loginToService(authServiceClient.authService, this)
        return authServiceClient.authService.exchangeAuthCode(browser.auth, authorization)
    }

}

