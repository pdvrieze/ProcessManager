package nl.adaptivity.process.engine.pma.data

import kotlinx.serialization.Serializable

@Serializable
data class CallerInfo(val name: String, val phoneNumber: String)

