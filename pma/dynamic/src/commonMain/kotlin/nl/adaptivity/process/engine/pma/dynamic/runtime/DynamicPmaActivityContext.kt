package nl.adaptivity.process.engine.pma.dynamic.runtime

import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRef
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface DynamicPmaActivityContext<out AIC : DynamicPmaActivityContext<AIC, BIC>, out BIC : TaskBuilderContext.BrowserContext<AIC, BIC>> :
    PmaActivityContext<AIC> {
    override val processContext: DynamicPmaProcessInstanceContext<AIC>
    fun <T: Any> nodeData(reference: InputRef<T>): T = requireNotNull(nodeDataOrNull(reference)) { "No node data for $reference could be found" }
    fun <T: Any> nodeDataOrNull(reference: InputRef<T>): T?
    fun browserContext(browser: Browser): BIC
    fun resolveBrowser(principal: PrincipalCompat): Browser
}
