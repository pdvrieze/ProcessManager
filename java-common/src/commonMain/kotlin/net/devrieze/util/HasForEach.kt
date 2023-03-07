package nl.adaptivity.util.net.devrieze.util

import kotlin.jvm.JvmSynthetic

public interface HasForEach<V> {
    public fun forEach(body: ForEachReceiver<V>)

    public interface IteratingContext {
        fun stopIterationAfter()

        fun continueWithNext(): Nothing

        fun breakIteration(): Nothing {
            stopIterationAfter()
            continueWithNext()
        }
    }

    fun interface ForEachReceiver<V>: MutableHasForEach.ForEachReceiver<V> {
        fun runBody(context: IteratingContext, v: V)

        @JvmSynthetic
        override fun runBody(context: MutableHasForEach.IteratingContext, v: V) {
            return runBody((context as IteratingContext), v)
        }
    }

    companion object {
        fun <V> forEach(iterable: Iterator<V>, body: IteratingContext.(V) -> Unit) {
            val context = ForEachContextImpl()
            val iterator = iterable.iterator()
            while (iterator.hasNext() && context.continueIteration) {
                try {
                    context.body(iterator.next())
                } catch (e: ForEachContextImpl.ContinueException) { // Don't need to do anything }
                }
            }
        }
    }
}

interface MutableHasForEach<V> : HasForEach<V> {
    fun forEach(body: ForEachReceiver<V>)

    public interface IteratingContext : HasForEach.IteratingContext {
        fun delete()
    }

    fun interface ForEachReceiver<V> {
        fun runBody(context: IteratingContext, v: V)
    }

}

internal class ForEachContextImpl(private val onDelete: () -> Unit = { throw UnsupportedOperationException("Deletion is not supported") }) : MutableHasForEach.IteratingContext {
    var continueIteration = true
        private set

    override fun stopIterationAfter() {
        continueIteration = false
    }

    override fun continueWithNext(): Nothing {
        throw ContinueException()
    }

    override fun delete() {
        onDelete()
    }

    internal class ContinueException() : RuntimeException()
}
