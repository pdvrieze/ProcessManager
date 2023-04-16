package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.util.multiplatform.PrincipalCompat

class EnumeratedTaskList constructor(
    serviceName: ServiceName<EnumeratedTaskList>,
    authService: AuthService,
    engineService: EngineService,
    clientAuth: PmaIdSecretAuthInfo,
    val principals: List<PrincipalCompat>
) : TaskList<EnumeratedTaskList>(serviceName, authService, engineService, clientAuth) {

    constructor(
        serviceName: ServiceName<EnumeratedTaskList>,
        authService: AuthService,
        engineService: EngineService,
        clientAuth: PmaIdSecretAuthInfo,
        principal: PrincipalCompat
    ) : this(serviceName, authService, engineService, clientAuth, listOf(principal))

    override fun servesFor(principal: PrincipalCompat): Boolean {
        return principal in principals
    }

    override fun getServiceState(): String = principals.joinToString(prefix = "[", postfix = "]")
}

class DynamicTaskList constructor(
    serviceName: String,
    authService: AuthService,
    engineService: EngineService,
    clientAuth: PmaIdSecretAuthInfo,
    val domain: String,
) : TaskList<DynamicTaskList>(ServiceName(serviceName), authService, engineService, clientAuth) {
    override fun servesFor(principal: PrincipalCompat): Boolean {
        val principalDomain = principal.name.substringAfter('@', "")
        return domain == principalDomain
    }
}
