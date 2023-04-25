package io.github.pdvrieze.pma.agfil.contexts

import io.github.pdvrieze.pma.agfil.data.Money
import io.github.pdvrieze.pma.agfil.services.GarageService
import io.github.pdvrieze.process.simulator.utils.nextGaussians
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.math.roundToLong
import kotlin.random.Random

class AgfilActivityContextImpl(
    override val processContext: AgfilProcessContext,
    processNode: IProcessNodeInstance,
    private val random: Random
) : AbstractDynamicPmaActivityContext<AgfilActivityContext, AgfilBrowserContext>(processNode), AgfilActivityContext {

    override fun randomEaCallHandler(): PrincipalCompat {
        return processContext.contextFactory.europAssistService.randomCallHandler()
    }

    override fun randomGarageReceptionist(garageService: ServiceId<GarageService>): PrincipalCompat {
        return processContext.contextFactory.garageServices
            .single { it.serviceInstanceId == garageService }
            .randomGarageReceptionist()
    }

    override fun randomMechanic(): PrincipalCompat {
        TODO("not implemented")
    }

    override fun randomRepairCosts(): Money {
        while(true) {
            val (r1, r2) = random.nextGaussians(100000.0, 50000.0)
            when {
                r1>0.0 -> return Money(r1.roundToLong())
                r2>0.0 -> return Money(r1.roundToLong())
            }
        }
    }

    override fun randomAccidentDetails(): String {
        processContext.contextFactory
        return random.nextString()
    }

    override fun browserContext(browser: Browser): AgfilBrowserContext {
        return AgfilBrowserContext(this, browser)
    }
}
