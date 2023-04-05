package io.github.pdvrieze.pma.agfil.contexts

import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AgfilActivityContext: DynamicPmaActivityContext<AgfilActivityContext, AgfilBrowserContext> {
    override val processContext: AgfilProcessContext

    fun randomEaCallHandler(): PrincipalCompat
    fun randomAccidentDetails(): String

}

