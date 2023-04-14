package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.AutomatedService

interface GarageAccessService: AutomatedService {
    /** The list of garage services (to resolve) */
    val garageServices: List<GarageService>

    /** The authentication service */
    val authService: AuthService

    /** The authentication that identifies this service against the authentication service */
    val serviceAuth: PmaAuthInfo

    /** The property to access the internal details */
    val internal: Internal

    interface Internal {
        val outer: GarageAccessService

    }
}

@PublishedApi
internal fun GarageAccessService.Internal.resolveGarageService(
    authToken: PmaAuthToken,
    garageInfo: GarageInfo?
): ServiceInvocationContext<GarageService> {
    val garageName = requireNotNull(garageInfo) { "No garage assigned to claim" }.name
    val garageService = outer.garageServices.first { it.serviceInstanceId.serviceId == garageName }
    val delegateToken = outer.authService.exchangeDelegateToken(
        outer.serviceAuth,
        authToken,
        garageService.serviceInstanceId,
        CommonPMAPermissions.IDENTIFY
    )
    return ServiceInvocationContext(garageService, delegateToken)
}

inline fun <R> GarageAccessService.Internal.withGarage(
    authToken: PmaAuthToken,
    garageInfo: GarageInfo?,
    action: ServiceInvocationContext<GarageService>.() -> R
): R {
    return resolveGarageService(authToken, garageInfo).action()
}
