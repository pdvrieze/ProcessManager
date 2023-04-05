package nl.adaptivity.process.engine.pma.data

import kotlinx.serialization.Serializable

@Serializable
data class AccidentInfo(val callerInfo: CallerInfo, val accidentDetails: String)
