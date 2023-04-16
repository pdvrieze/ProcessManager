package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.GeneralClientService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.systems.*

object ServiceNames {
    val accountManagementSystem: ServiceName<AccountManagementSystem> = ServiceName("accountManagementSystem")
    val authService: ServiceName<AuthService> = ServiceName("authService")
    val engineService: ServiceName<EngineService> = ServiceName("engineService")

    //    val clerk1 : ServiceId = ServiceId("clerk1")
    val creditApplication: ServiceName<CreditApplication> = ServiceName("creditApplication")
    val creditBureau: ServiceName<CreditBureau> = ServiceName("creditBureau")

    //    val customer : ServiceId = ServiceId("customer")
//    val customerData : ServiceId<CustomerData> = ServiceId("customerData")
    val customerFile: ServiceName<CustomerInformationFile> = ServiceName("customerFile")
    val generalClientService: ServiceName<GeneralClientService> = ServiceName("generalClientService")
    val outputManagementSystem: ServiceName<OutputManagementSystem> = ServiceName("outputManagementSystem")

    //    val postProcClerk : ServiceId = ServiceId("postProcClerk")
    val pricingEngine: ServiceName<PricingEngine> = ServiceName("pricingEngine")
    val signingService: ServiceName<SigningService> = ServiceName("signingService")
}
