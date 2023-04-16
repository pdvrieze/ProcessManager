package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AuthServiceClient<InfoT: AuthorizationInfo, TokenT: AuthorizationInfo.Token, CodeT> {

    val principal: PrincipalCompat

    fun validateAuthInfo(
        authInfoToCheck: InfoT,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    )

    fun exchangeAuthCode(authorizationCode: CodeT): TokenT

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param authorizedService The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param tokenTargetService The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        authorizedService: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): CodeT

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param client The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param serviceId The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        identifiedUser: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        authorizedService: ServiceId<*>,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): CodeT

    fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): TokenT

    fun invalidateActivityTokens(
        hNodeInstance: PNIHandle
    )

}

