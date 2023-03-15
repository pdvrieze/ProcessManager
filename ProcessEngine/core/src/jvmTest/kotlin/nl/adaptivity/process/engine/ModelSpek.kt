/*
 * Copyright (c) 2018.
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

import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel

data class ModelData(
    val engineData: () -> EngineTestData<ActivityInstanceContext>,
    val model: ExecutableProcessModel,
    val valid: List<Trace>,
    val invalid: List<Trace>
) {

    internal constructor(
        model: TestConfigurableModel,
        valid: List<Trace>,
        invalid: List<Trace>
    ) : this(
        engineData = { EngineTestData.defaultEngine() },
        model = model.rootModel,
        valid = valid,
        invalid = invalid
    )


    constructor(model: ProcessModel<*>, valid: List<Trace>, invalid: List<Trace>) :
        this(
            engineData = { EngineTestData.defaultEngine() },
            model = model.rootModel as? ExecutableProcessModel ?: ExecutableProcessModel(model.rootModel.builder()),
            valid = valid,
            invalid = invalid
        )

    constructor(
        engineData: () -> EngineTestData<ActivityInstanceContext>,
        generalModel: ProcessModel<*>,
        valid: List<Trace>,
        invalid: List<Trace>
    ) : this(
        engineData,
        model = generalModel.rootModel as? ExecutableProcessModel ?: ExecutableProcessModel(generalModel.rootModel.builder()),
        valid,
        invalid
    )
}

/**
 * This function takes n elements from the list by sampling. This differs from [List.take] as that function will take
 * the first n elements where this function will sample.
 */
fun <T> List<T>.selectN(max: Int): List<T> {
    val origSize = size
    if (max >= origSize) return this
    return filterIndexed { idx, _ ->
        idx == 0 || (((idx - 1) * max) / origSize < (idx * max) / origSize)
    }
}

