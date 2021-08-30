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

package nl.adaptivity.process.processModel.engine

import kotlinx.serialization.Serializable
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.xmlutil.serialization.XmlDefault


/**
 * Fix compilation by converting it properly to Kotlin.
 */
interface XmlProcessNode : ProcessNode

@Serializable
abstract class ProcessNodeSerialDelegate(
    val id: String?,
    val label: String?,
    @XmlDefault("NaN")
    val x: Double = Double.NaN,
    @XmlDefault("NaN")
    val y: Double = Double.NaN,
    @XmlDefault("false")
    val isMultiInstance: Boolean = false,
)
