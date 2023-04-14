package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.models.ANYSCOPE
import nl.adaptivity.process.engine.pma.models.AuthScope


inline fun <R> AutoService.withGarage(
    authToken: PmaAuthToken,
    garageInfo: GarageInfo?,
    requestedScope: AuthScope = ANYSCOPE,
    action: ServiceInvocationContext<GarageService>.() -> R
): R {
    val garageName = requireNotNull(garageInfo) { "No garage assigned to claim" }.service
    return withService(garageName, authToken, requestedScope, action)
}
