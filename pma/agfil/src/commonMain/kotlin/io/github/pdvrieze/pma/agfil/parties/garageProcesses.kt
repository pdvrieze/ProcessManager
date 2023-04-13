package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess

val estimateRepairProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("estimate repair") {

}

val repairProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("estimate repair") {

}
