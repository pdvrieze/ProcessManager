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

import nl.adaptivity.process.engine.pma.AuthInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.dynamic.UIServiceImpl
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanEvaluation
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanProductBundle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.PricedLoanProductBundle

class PricingEngine(authService: AuthService): UIServiceImpl(authService, "Pricing_Engine"), AutomatedService {

    override fun getServiceState(): String = ""

    fun priceLoan(authInfo: AuthInfo, chosenProduct: LoanProductBundle, loanEval: LoanEvaluation):
        PricedLoanProductBundle {
        logMe(chosenProduct)

        validateAuthInfo(authInfo, LoanPermissions.PRICE_LOAN.context(loanEval.customerId, loanEval.application.amount))

        return chosenProduct.withPrice(loanEval.customerId, loanEval.application.amount, 0.05)
    }

}
