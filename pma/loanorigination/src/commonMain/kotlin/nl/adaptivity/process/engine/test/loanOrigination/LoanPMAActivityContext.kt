package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance

class LoanPMAActivityContext(
    override val processContext: LoanPmaProcessContext,
    processNode: IProcessNodeInstance
) : AbstractDynamicPmaActivityContext<LoanPMAActivityContext, LoanBrowserContext>(processNode) {
    /*
    @Suppress("UNCHECKED_CAST")
    override val processNode: PMAActivityInstance<LoanPMAActivityContext>
        get() = super.processNode as PMAActivityInstance<LoanPMAActivityContext>
*/
    override fun browserContext(browser: Browser): LoanBrowserContext {
        return LoanBrowserContext(this, browser)
    }

}
