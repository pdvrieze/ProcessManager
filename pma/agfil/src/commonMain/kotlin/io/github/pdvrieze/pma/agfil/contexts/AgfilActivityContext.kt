package io.github.pdvrieze.pma.agfil.contexts

import io.github.pdvrieze.pma.agfil.data.CallerInfo
import io.github.pdvrieze.pma.agfil.data.Money
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AgfilActivityContext: DynamicPmaActivityContext<AgfilActivityContext, AgfilBrowserContext> {
    override val processContext: AgfilProcessContext

    fun randomEaCallHandler(): PrincipalCompat
    fun randomGarageReceptionist(): PrincipalCompat
    fun randomMechanic(): PrincipalCompat
    fun randomAccidentDetails(): String
    fun randomRepairCosts(): Money
    fun callerInfo(customer: PrincipalCompat): CallerInfo {
        return processContext.contextFactory.callerInfo(customer)
    }

}

