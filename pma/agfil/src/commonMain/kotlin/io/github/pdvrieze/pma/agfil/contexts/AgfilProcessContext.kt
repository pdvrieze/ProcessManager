package io.github.pdvrieze.pma.agfil.contexts

import io.github.pdvrieze.pma.agfil.data.CarRegistration
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.random.Random

class AgfilProcessContext(
    private val engineData: ProcessEngineDataAccess,
    override val contextFactory: AgfilContextFactory,
    override val processInstanceHandle: PIHandle,
    private val random: Random
) : DynamicPmaProcessInstanceContext<AgfilActivityContext> {
    override val processInstance: IProcessInstance get() = engineData.instance(processInstanceHandle).withPermission()
    override val authService: AuthService get() = contextFactory.authService
    override val engineService: EngineService get() = contextFactory.engineService

    val carRegistration: CarRegistration by lazy { CarRegistration(random.nextString()) }

    override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
        return engineData.instance(processInstanceHandle).withPermission().allChildNodeInstances()
            .filter { it.node.id == name.id }
            .toList()
    }

    override fun resolveBrowser(principal: PrincipalCompat): Browser {
        return contextFactory.resolveBrowser(principal)
    }
}
