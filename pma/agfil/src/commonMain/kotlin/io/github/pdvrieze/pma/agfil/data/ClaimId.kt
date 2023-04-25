package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ClaimId(val id: Long)

@Serializable
data class InsurancePolicy(val policyNumber: Long, val customer: CustomerId, val carCoverage: List<CoverageInfo>) {

    @Serializable
    data class CoverageInfo(val car: CarRegistration, val coverage: Coverage)

    @Serializable enum class Coverage {
        LIABILITY,
        PARTIAL,
        COMPREHENSIVE
    }
}

