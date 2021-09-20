/*
 * Copyright (c) 2021.
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

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableModelCommon
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel

interface IProcessInstance: ReadableHandleAware<SecureObject<ProcessInstance>> {
    val processModel: ExecutableModelCommon

    val inputs: List<ProcessData>

    fun getChildNodeInstance(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): IProcessNodeInstance

    fun allChildNodeInstances(): Sequence<IProcessNodeInstance>
}
