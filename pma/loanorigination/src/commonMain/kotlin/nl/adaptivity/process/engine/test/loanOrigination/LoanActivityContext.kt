/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import nl.adaptivity.process.engine.pma.PMAActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanActivityContext(override val processContext: LoanProcessContext, processNode: IProcessNodeInstance) :
    PMAActivityContext<LoanActivityContext>(processNode) {

    override fun canBeAccessedBy(principal: PrincipalCompat?): Boolean {

        val restrictions = (node as? RunnableActivity<*, *, *>)
            ?.accessRestrictions ?: return true
        return principal != null && restrictions.hasAccess(this, principal)
    }

}
