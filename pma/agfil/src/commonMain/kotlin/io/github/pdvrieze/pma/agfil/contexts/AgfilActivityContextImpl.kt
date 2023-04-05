package io.github.pdvrieze.pma.agfil.contexts

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.random.Random

class AgfilActivityContextImpl(
    override val processContext: AgfilProcessContext,
    processNode: IProcessNodeInstance,
    private val random: Random
) : AbstractDynamicPmaActivityContext<AgfilActivityContext, AgfilBrowserContext>(processNode), AgfilActivityContext {

    override fun randomEaCallHandler(): PrincipalCompat {
        TODO("not implemented")
    }

    override fun randomAccidentDetails(): String {
        processContext.contextFactory
        return random.nextString()
    }

    override fun browserContext(browser: Browser): AgfilBrowserContext {
        return AgfilBrowserContext(this, browser)
    }
}
