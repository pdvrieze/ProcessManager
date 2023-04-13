package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilProcessContext
import nl.adaptivity.process.engine.pma.dynamic.ServiceActivityContext

val ServiceActivityContext<AgfilActivityContext, *>.agfilProcessContext: AgfilProcessContext
    get() = activityContext.processContext
