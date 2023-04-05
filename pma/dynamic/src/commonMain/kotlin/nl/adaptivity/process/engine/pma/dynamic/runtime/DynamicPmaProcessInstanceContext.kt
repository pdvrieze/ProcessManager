package nl.adaptivity.process.engine.pma.dynamic.runtime

import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.OutputRef
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.runtime.PmaProcessInstanceContext
import nl.adaptivity.process.engine.processModel.applyData
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface DynamicPmaProcessInstanceContext<A : DynamicPmaActivityContext<A, *>> : PmaProcessInstanceContext<A> {
    val processInstance: IProcessInstance
    val authService: AuthService
    val engineService: EngineService
    override val contextFactory: DynamicPmaProcessContextFactory<A>


    fun <I: Any, O: Any, C : DynamicPmaActivityContext<C, *>> nodeResult(node: RunnablePmaActivity<I, O, C>, reference: OutputRef<O>): I {
        val defines = node.defines.map {
            // TODO the cast shouldn't be needed
            it.applyData(processInstance, this as ActivityInstanceContext)
        }

        return node.getInputData(defines)

    }

    fun resolveBrowser(principal: PrincipalCompat): Browser

}
