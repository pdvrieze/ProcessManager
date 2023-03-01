package nl.adaptivity.util.multiplatform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
public expect inline fun <R> synchronizedCompat(lock: Any, block: () -> R): R
