package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class Invoice(
    val invoiceId: InvoiceId,
    val claimId: ClaimId,
    val garage: GarageInfo,
    val amount: Money
)

@Serializable
@JvmInline
value class InvoiceId(val id: Int)
