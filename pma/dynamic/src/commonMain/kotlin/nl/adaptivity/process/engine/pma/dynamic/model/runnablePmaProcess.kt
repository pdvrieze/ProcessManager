@file:OptIn(ExperimentalTypeInference::class)

package nl.adaptivity.process.engine.pma.dynamic.model

import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext.BrowserContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.processModel.path
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import kotlin.experimental.ExperimentalTypeInference


fun <AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>> runnablePmaProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: RootPmaModelBuilderContext<AIC, BIC>.() -> Unit
): ExecutableProcessModel {
    val context = RootPmaModelBuilderContext<AIC, BIC>(name, owner, uuid).apply(configureAction)
    val noPathImports = context.modelBuilder.imports
        .filter { it.path==null }
    if(noPathImports.size>1) {
        for(import in noPathImports) {
            import.setPath(import.originalNSContext.toList(), "/${import.name}/node()")
        }
    }
    return ExecutableProcessModel(context.modelBuilder, true)
}


