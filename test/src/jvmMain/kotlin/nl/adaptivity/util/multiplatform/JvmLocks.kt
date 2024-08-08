package nl.adaptivity.util.multiplatform

import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
public actual inline fun <R> synchronizedCompat(lock: Any, block: () -> R): R = synchronized(lock, block)
