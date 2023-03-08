package net.devrieze.util

import java.util.concurrent.CopyOnWriteArraySet

internal actual fun <V> createCachingMapHandleSet():MutableSet<Handle<V>> = CopyOnWriteArraySet()
