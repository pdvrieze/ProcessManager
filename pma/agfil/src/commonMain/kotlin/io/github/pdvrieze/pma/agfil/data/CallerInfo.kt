package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class CallerInfo(val name: String, val phoneNumber: String)

