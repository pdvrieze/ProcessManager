package io.github.pdvrieze.pma.agfil.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.models.*

interface AutoService {
    /** The authentication service */
    val authService: AuthService
    /** A function that can resolve services from their name */
    val serviceResolver: ServiceResolver
    /** The authentication that identifies this service against the authentication service */
    val serviceAuth: PmaAuthInfo
}

inline fun <S : Service, R> AutoService.withService(
    serviceName: ServiceName<S>,
    authToken: PmaAuthToken,
    requestedScope: AuthScope = ANYSCOPE,
    action: ServiceInvocationContext<S>.() -> R
): R {
    return resolveService(serviceName, authToken, requestedScope).action()
}

inline fun <S : Service, R> AutoService.withService(
    service: S,
    authToken: PmaAuthToken,
    requestedScope: AuthScope = ANYSCOPE,
    action: ServiceInvocationContext<S>.() -> R
): R {
    return resolveService(service, authToken, requestedScope).action()
}

@PublishedApi
internal fun <S : Service> AutoService.resolveService(
    serviceName: ServiceName<S>,
    authToken: PmaAuthToken,
    requestedScope: AuthScope
): ServiceInvocationContext<S> {
    return resolveService(serviceResolver.resolve(serviceName), authToken, requestedScope)
}

@PublishedApi
internal fun <S : Service> AutoService.resolveService(
    service: S,
    baseToken: PmaAuthToken,
    requestedScope: AuthScope
): ServiceInvocationContext<S> {
    val serviceToken = authService.exchangeDelegateToken(serviceAuth, baseToken, service.serviceInstanceId, requestedScope)
    return ServiceInvocationContext(service, serviceToken)
}

//fun ServiceBase.withService(serviceName: ServiceName<S>)
