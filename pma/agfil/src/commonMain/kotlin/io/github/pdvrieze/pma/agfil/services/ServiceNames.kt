package io.github.pdvrieze.pma.agfil.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.ServiceName

object ServiceNames {
    val authService = ServiceName<AuthService>("authService")
    val engineService = ServiceName<EngineService>("engineService")
    val taskListService = ServiceName<TaskList>("engineService")
    val europAssistService = ServiceName<EuropAssistService>("europAssistService")
    val garageServices = arrayOf(
        ServiceName<GarageService>("Fix'R'Us"),
        ServiceName<GarageService>("Dentagone"),
        ServiceName<GarageService>("Scrapemall"),
    )
    val agfilService = ServiceName<AgfilService>("agfilService")
    val leeCsService = ServiceName<LeeCsService>("leeCsService")
}
