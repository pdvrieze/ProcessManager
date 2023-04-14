package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo

interface AuthServiceClient<InfoT: AuthorizationInfo, TokenT: AuthorizationInfo.Token, CodeT> {

    fun validateAuthInfo(
        authInfoToCheck: InfoT,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    )

    fun exchangeAuthCode(authorizationCode: CodeT): TokenT

    fun requestPmaAuthCode(
        client: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        requestedScope: AuthScope
    ): CodeT

    fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): TokenT

}

