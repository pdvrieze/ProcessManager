package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.CarRegistration
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess

val repairProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("estimate repair") {

    val start by startNode

    val onReceiveCar by eventNode(start, CarRegistration.serializer())

    val receiveCar by taskActivity(onReceiveCar) {
        acceptTask({  randomGarageReceptionist() }) {

        }
    }

}
