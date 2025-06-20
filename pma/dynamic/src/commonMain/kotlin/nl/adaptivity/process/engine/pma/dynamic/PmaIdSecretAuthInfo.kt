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

package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.random.Random

class PmaIdSecretAuthInfo(
    override val principal: PrincipalCompat,
    override val secret: String = Random.nextString()
): PmaAuthInfo(), AuthorizationInfo.IdSecret {
    override fun toString(): String {
        return "PW(${principal.name})=${this.secret}"
    }

    override val id: String get() = principal.name
}
