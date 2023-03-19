package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext

object EvalMessageScope: AuthScopeTemplate<PMAActivityContext<*>>, AuthScope {
    override fun instantiateScope(context: PMAActivityContext<*>): AuthScope = this
}
