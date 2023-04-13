package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class AccidentInfo(val callerInfo: CallerInfo, val accidentDetails: String)
