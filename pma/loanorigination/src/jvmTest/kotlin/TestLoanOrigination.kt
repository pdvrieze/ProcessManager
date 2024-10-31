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

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.RunningMessageService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.pma.AuthorizationException
import nl.adaptivity.process.engine.test.ProcessEngineFactory
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.Approval
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.Offer
import nl.adaptivity.process.engine.test.loanOrigination.systems.SignedDocument
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TestLoanOrigination : ProcessEngineTestSupport() {

    @Test
    fun testCreateObjectModel() {
        assertEquals("inputCustomerMasterData", LoanOriginationModel(testModelOwnerPrincipal).inputCustomerMasterData.id)
    }

    @Test
    fun testCreateValModel() {
        val activity = loanModel2.getNode("inputCustomerMasterData")
        assertIs<RunnableActivity<*, *, *>>(activity)
        assertNotNull(loanModel2.uuid, "UUID is not expected to be null")
    }

    @Test
    fun testRunObjectModel() {
        val model = ExecutableProcessModel(LoanOriginationModel(testModelOwnerPrincipal).configurationBuilder)
        testRunModel(model) { logger, random -> LoanContextFactory(logger, random) }
    }

    @Test
    fun testSerializeSignedApproval() {
        val approval = SignedDocument("admin", 5L, Approval(true))
        val offer = SignedDocument("admin", 4L, Offer("1", "2", "signature"))
        val serialized1 = CompactFragment { writer ->
            XML.defaultInstance.encodeToWriter(writer, serializer<SignedDocument<Offer>>(), offer)
        }
        val serialized2 = CompactFragment { writer ->
            XML.defaultInstance.encodeToWriter(writer, serializer<SignedDocument<Approval>>(), approval)
        }
        assertEquals("<SignedDocument signedBy=\"admin\" nodeInstanceHandle=\"4\"><Offer id=\"1\" customerId=\"2\" customerSignature=\"signature\"/></SignedDocument>", serialized1.contentString)
        assertEquals("<SignedDocument signedBy=\"admin\" nodeInstanceHandle=\"5\"><Approval>true</Approval></SignedDocument>", serialized2.contentString)
    }

    @Test
    fun testRunValModel() {
        testRunModel(loanModel2) { logger, random -> LoanContextFactory(logger, random) }
    }

    @Test
    fun testRunPmaModel() {
        testRunModel(pmaLoanModel) { logger, random ->
            LoanPMAContextFactory(logger, random)
        }
    }

    private fun testRunModel(model: ExecutableProcessModel, createContextFactory: (Logger, Random) -> AbstractLoanContextFactory<*>) {
        val logger = Logger.getLogger(TestLoanOrigination::class.java.name)
        lateinit var messageService: RunningMessageService
        val pef: ProcessEngineFactory = { baseMessageService, transactionFactory ->
            val cf = createContextFactory(logger, Random(1234))
            val engineService = cf.engineService
            messageService = RunningMessageService() { engineService }
            defaultEngineFactory(
                messageService,
                transactionFactory,
                cf
            ).also { engineService.initEngine(it) }
        }
        testProcess(pef, model) { processEngine, tr, model, hinstance ->
            messageService.processMessages()

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
