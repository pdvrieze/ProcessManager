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

package nl.adaptivity.process.engine.test.loanOrigination.datatypes

import kotlinx.serialization.Serializable

@Serializable
open class LoanProductBundle(val name: String, val id: String) {
    fun withPrice(customerId: String, price: Double): PricedLoanProductBundle {
        return PricedLoanProductBundle(
            name,
            id,
            customerId,
            price,
            false
                                                                                                  )
    }

}

@Serializable
class PricedLoanProductBundle : LoanProductBundle {
    val customerId: String
    val price: Double
    val approvedOffer: Boolean

    constructor(name: String, id: String, customerId: String, price: Double, approvedOffer: Boolean) : super(name, id) {
        this.customerId = customerId
        this.price = price
        this.approvedOffer = approvedOffer
    }

    fun approve(): PricedLoanProductBundle {
        return PricedLoanProductBundle(
            name,
            id,
            customerId,
            price,
            true
                                                                                                  )
    }
}


