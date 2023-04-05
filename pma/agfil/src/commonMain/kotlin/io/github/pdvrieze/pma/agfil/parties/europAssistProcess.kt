package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.process.processModel.dynamicProcessModel.ActivityHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RoleRestriction
import nl.adaptivity.process.engine.pma.data.AccidentInfo
import nl.adaptivity.process.engine.pma.data.CallerInfo
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess

val europAssistProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("EuropAssistHandleCall") {
    val callInfo = input<CallerInfo>("callerInfo")
    val start by startNode


    val gatherInfo: ActivityHandle<AccidentInfo> by taskActivity(
        predecessor = start,
        input = callInfo,
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) {callInfo ->
            AccidentInfo(callInfo, randomAccidentDetails())
        }
        TODO()

    }
}
