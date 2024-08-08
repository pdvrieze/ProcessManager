package nl.adaptivity.util.multiplatform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * JS has no explicit threads so skip that
 */
@OptIn(ExperimentalContracts::class)
public actual inline fun <R> synchronizedCompat(lock: Any, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return block()
}
