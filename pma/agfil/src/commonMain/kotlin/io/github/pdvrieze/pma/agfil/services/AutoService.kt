package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.models.*

interface AutoService {
    /** The authentication service */
    val authServiceClient: DefaultAuthServiceClient
    /** A function that can resolve services from their name */
    val serviceResolver: ServiceResolver
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
    serviceId: ServiceId<S>,
    authToken: PmaAuthToken,
    requestedScope: AuthScope = ANYSCOPE,
    action: ServiceInvocationContext<S>.() -> R
): R {
    return resolveService(serviceId, authToken, requestedScope).action()
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
    return resolveService(serviceResolver.resolveService(serviceName), authToken, requestedScope)
}

@PublishedApi
internal fun <S : Service> AutoService.resolveService(
    serviceId: ServiceId<S>,
    authToken: PmaAuthToken,
    requestedScope: AuthScope
): ServiceInvocationContext<S> {
    return resolveService(serviceResolver.resolveService(serviceId), authToken, requestedScope)
}

@PublishedApi
internal fun <S : Service> AutoService.resolveService(
    service: S,
    baseToken: PmaAuthToken,
    requestedScope: AuthScope
): ServiceInvocationContext<S> {
    val serviceToken = authServiceClient.exchangeDelegateToken(baseToken, service.serviceInstanceId, requestedScope)
    return ServiceInvocationContext(service, serviceToken)
}

inline fun <R> AutoService.withGarage(
    authToken: PmaAuthToken,
    garageInfo: GarageInfo?,
    requestedScope: AuthScope = ANYSCOPE,
    action: ServiceInvocationContext<GarageService>.() -> R
): R {
    val garageName = requireNotNull(garageInfo) { "No garage assigned to claim" }.service
    return withService(garageName, authToken, requestedScope, action)
}

//fun ServiceBase.withService(serviceName: ServiceName<S>)
