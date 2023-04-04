@file:OptIn(ExperimentalTypeInference::class)

package nl.adaptivity.process.engine.pma.dynamic.model

import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext.BrowserContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import kotlin.experimental.ExperimentalTypeInference


fun <AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>> runnablePmaProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: PmaModelBuilderContext<AIC, BIC>.() -> Unit
): ExecutableProcessModel {
    val context = RootPmaModelBuilderContext<AIC, BIC>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}


