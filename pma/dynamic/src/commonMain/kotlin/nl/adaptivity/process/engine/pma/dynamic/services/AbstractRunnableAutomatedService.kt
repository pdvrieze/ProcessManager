package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.models.ServiceName
import java.util.logging.Logger

abstract class AbstractRunnableAutomatedService<S: AbstractRunnableAutomatedService<S>> : ServiceBase<S>, RunnableAutomatedService {
    constructor(authServiceClient: DefaultAuthServiceClient, serviceName: ServiceName<S>, logger: Logger) :
        super(authServiceClient, serviceName, logger)

    constructor(
        authService: AuthService,
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        logger: Logger = authService.logger
    ) : super(authService, serviceAuth, serviceName, logger)

    constructor(authService: AuthService, adminAuth: PmaAuthInfo, serviceName: ServiceName<S>, logger: Logger = authService.logger) :
        super(authService, adminAuth, serviceName, logger)
}
