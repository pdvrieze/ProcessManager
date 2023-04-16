/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.test.loanOrigination.systems

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.Contract
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.Offer
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.PricedLoanProductBundle
import kotlin.random.Random

class OutputManagementSystem(serviceName: ServiceName<OutputManagementSystem>, authService: AuthService, adminAuth: PmaAuthInfo):
    AbstractRunnableUiService<OutputManagementSystem>(authService, adminAuth, serviceName),
    AutomatedService {

    override fun getServiceState(): String = ""

    private val _contracts = mutableMapOf<String, Contract>()
    val contracts: Map<String, Contract> get() = _contracts

    private val _offers = mutableMapOf<String, PricedLoanProductBundle>()
    val offers: Map<String, PricedLoanProductBundle> get() = _offers

    fun registerAndPrintOffer(authInfo: PmaAuthInfo, approvedOffer: PricedLoanProductBundle): Offer {
        logMe(approvedOffer)
        validateAuthInfo(authInfo, LoanPermissions.PRINT_OFFER)
        val offerId = Random.nextString()
        _offers.put(offerId, approvedOffer)
        return Offer(offerId, approvedOffer.customerId)
    }

    fun signAndRegisterContract(authInfo: PmaAuthInfo, offer: Offer, signature: String): Contract {
        logMe(offer, signature)
        val offerAmount = _offers[offer.id]?.amount ?: throw IllegalArgumentException("Offer not registered")
        validateAuthInfo(authInfo, LoanPermissions.SIGN_LOAN.context(offer.customerId, offerAmount))

        val contractId = Random.nextString()
        return Contract(contractId, offer.id, offer.customerId, offer.customerSignature!!, signature).also {
            _contracts[contractId] = it
        }
    }

}
