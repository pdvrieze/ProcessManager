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

class TestLoanOrigination : ProcessEngineTestSupport<LoanActivityContext>() {

    @Test
    fun testCreateObjectModel() {
        assertEquals("inputCustomerMasterData", LoanOriginationModel(testModelOwnerPrincipal).inputCustomerMasterData.id)
    }

/*
    @Test
    fun testCreateValModel() {
        val activity = loanModel2.getNode("inputCustomerMasterData")
        assertIs<RunnableActivity<*, *, *>>(activity)
        assertNotNull(loanModel2.uuid, "UUID is not expected to be null")
    }
*/

    @Test
    fun testRunObjectModel() {
        val model = ExecutableProcessModel(LoanOriginationModel(testModelOwnerPrincipal).configurationBuilder)
        testRunModel(model)
    }

/*
    @Test
    fun testRunValModel() {
        testRunModel(loanModel2)
    }
*/

    private fun testRunModel(model: ExecutableProcessModel) {
        val logger = Logger.getLogger(TestLoanOrigination::class.java.name)
        val pef: ProcessEngineFactory<LoanActivityContext> = { messageService, transactionFactory ->
            defaultEngineFactory(
                messageService,
                transactionFactory,
                LoanContextFactory(logger, Random(1234))
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


const val ASSERTFORBIDDENENABLED = false

@Suppress("NOTHING_TO_INLINE")
inline fun assertForbidden(noinline action: () -> Unit) {
    if (ASSERTFORBIDDENENABLED) assertThrows(AuthorizationException::class.java, action)
}
