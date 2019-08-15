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

import nl.adaptivity.process.engine.test.loanOrigination.auth.Service
import nl.adaptivity.process.engine.test.loanOrigination.auth.ServiceImpl
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.PricedLoanProductBundle
import java.util.*
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import kotlin.random.Random

class OutputManagementSystem(authService: AuthService): ServiceImpl(authService, "Output_Manamgent_System") {

    private val _contracts = mutableMapOf<UUID, Contract>()
    val contracts: Map<UUID, Contract> get() = _contracts

    private val _offers = mutableMapOf<UUID, PricedLoanProductBundle>()
    val offers: Map<UUID, PricedLoanProductBundle> get() = _offers

    fun registerAndPrintOffer(approvedOffer: PricedLoanProductBundle): Offer {
        val offerUuid = UUID.randomUUID()
        _offers.put(offerUuid, approvedOffer)
        return Offer(offerUuid.toString(), approvedOffer.customerId)
    }

    fun signAndRegisterContract(offer: Offer, signature: String): Contract {
        val contractId = UUID.randomUUID()
        return Contract(contractId.toString(), offer.id, offer.customerId, offer.customerSignature!!, signature).also {
            _contracts[contractId] = it
        }
    }

}
