package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CustomerId(val id: ULong) {}
