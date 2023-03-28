package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate

object EvalMessageScope: AuthScopeTemplate<ActivityInstanceContext>, AuthScope {
    override fun instantiateScope(context: ActivityInstanceContext): AuthScope = this
}
