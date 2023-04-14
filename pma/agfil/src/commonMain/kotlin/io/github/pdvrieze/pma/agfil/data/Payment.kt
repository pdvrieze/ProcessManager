package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class Payment(val invoiceId: InvoiceId, val money: Money) {
}
