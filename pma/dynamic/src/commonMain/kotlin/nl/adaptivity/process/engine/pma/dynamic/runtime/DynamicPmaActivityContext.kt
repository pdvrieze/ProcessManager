package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface DynamicPmaActivityContext<AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>> :
    PmaActivityContext<AIC> {

    fun browserContext(browser: Browser): BIC
    fun resolveBrowser(principal: PrincipalCompat): Browser
}
