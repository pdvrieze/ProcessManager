/*
 * Copyright (c) 2017.
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

package nl.adaptivity.spek

import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.InstanceFactory
import kotlin.reflect.KClass

/**
 * Created by pdvrieze on 26/01/17.
 */
object lenientFactory : InstanceFactory {
  override fun <T : Spek> create(spek: KClass<T>): T {
    return spek.objectInstance ?: spek.constructors.first { it.parameters.isEmpty() }.call()
  }
}
