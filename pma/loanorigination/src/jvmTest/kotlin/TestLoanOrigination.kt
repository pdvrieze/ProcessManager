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

package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.test.ProcessEngineFactory
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.*
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.processModel.engine.*
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLoanOrigination : ProcessEngineTestSupport() {

    @Test
    fun testCreateModel() {
        assertEquals("inputCustomerMasterData", LoanOriginationModel(modelOwnerPrincipal).inputCustomerMasterData.id)
    }

    @Test
    fun testRunModel() {
        val model = ExecutableProcessModel(LoanOriginationModel(modelOwnerPrincipal).configurationBuilder)
        val logger = Logger.getLogger(TestLoanOrigination::class.java.name)
        val pef: ProcessEngineFactory<LoanActivityContext> = { messageService, transactionFactory ->
            defaultEngineFactory(
                messageService,
                transactionFactory,
                LoanContextFactory(logger, Random)
            )
        }
        testProcess(pef, model) { processEngine, tr, model, hinstance ->
            val instance = tr.getInstance(hinstance)
            assertEquals(ProcessInstance.State.FINISHED, instance.state)
            val creditReport = instance.outputs.singleOrNull { it.name == "creditReport" }
            assertEquals(
                "<CreditReport creditRating=\"400\" maxLoan=\"20000\">John Doe (rating 400) is approved for loans up to 20000</CreditReport>",
                creditReport?.content?.contentString
            )
            val accountNumber = instance.outputs.singleOrNull { it.name == "accountNumber" }
            assertEquals("<BankAccountNumber>123456</BankAccountNumber>", accountNumber?.content?.contentString)
        }
    }

}

private inline val ActivityInstanceContext.ctx: LoanActivityContext get() = this as LoanActivityContext
private inline val ActivityInstanceContext.customerFile get() = ctx.processContext.customerFile
private inline val ActivityInstanceContext.signingService get() = ctx.processContext.signingService
private inline val ActivityInstanceContext.outputManagementSystem get() = ctx.processContext.outputManagementSystem
private inline val ActivityInstanceContext.accountManagementSystem get() = ctx.processContext.accountManagementSystem
private inline val ActivityInstanceContext.creditBureau get() = ctx.processContext.creditBureau
private inline val ActivityInstanceContext.creditApplication get() = ctx.processContext.creditApplication
private inline val ActivityInstanceContext.pricingEngine get() = ctx.processContext.pricingEngine
private inline val ActivityInstanceContext.authService get() = ctx.processContext.authService
private inline val ActivityInstanceContext.generalClientService get() = ctx.processContext.generalClientService
private inline val ActivityInstanceContext.clerk1 get() = ctx.processContext.clerk1
private inline val ActivityInstanceContext.postProcClerk get() = ctx.processContext.postProcClerk
private inline val ActivityInstanceContext.customer get() = ctx.processContext.customer


private fun ActivityInstanceContext.registerTaskPermission(service: Service, scope: PermissionScope) =
    ctx.registerTaskPermission(service, scope)

private fun ActivityInstanceContext.registerDelegatePermission(clientService: Service, service: Service, scope: PermissionScope) =
    ctx.registerDelegatePermission(clientService, service, scope)

private inline fun <R> ActivityInstanceContext.acceptBrowserActivity(
    browser: Browser,
    action: TaskList.Context.() -> R
): R =
    ctx.acceptBrowserActivity(browser, action)

const val ASSERTFORBIDDENENABLED = false

@Suppress("NOTHING_TO_INLINE")
inline fun assertForbidden(noinline action: () -> Unit) {
    if (ASSERTFORBIDDENENABLED) assertThrows(AuthorizationException::class.java, action)
}

val Random = kotlin.random.Random(1234)
