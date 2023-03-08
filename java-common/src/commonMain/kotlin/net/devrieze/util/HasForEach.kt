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
        fun eval(context: IteratingContext, v: V)

        @JvmSynthetic
        override fun eval(context: MutableHasForEach.IteratingContext, v: V) {
            return eval((context as IteratingContext), v)
        }
    }

    companion object {
        fun <V> forEach(iterable: Iterator<V>, body: ForEachReceiver<V>) {
            val context = ForEachContextImpl()
            val iterator = iterable.iterator()
            while (iterator.hasNext() && context.continueIteration) {
                try {
                    body.eval(context, iterator.next())
                } catch (e: ForEachContextImpl.ContinueException) { // Don't need to do anything }
                }
            }
        }
    }
}

interface MutableHasForEach<V> : HasForEach<V> {
    override fun forEach(body: HasForEach.ForEachReceiver<V>) {
        forEach(body as MutableHasForEach.ForEachReceiver<V>)
    }

    fun forEach(body: ForEachReceiver<V>)

    public interface IteratingContext : HasForEach.IteratingContext {
        fun delete()
    }

    fun interface ForEachReceiver<V> {
        fun eval(context: IteratingContext, v: V)
    }

    companion object {
        fun <V> forEach(iterable: Iterator<V>, body: ForEachReceiver<V>) {
            val context = ForEachContextImpl()
            val iterator = iterable.iterator()
            while (iterator.hasNext() && context.continueIteration) {
                try {
                    body.eval(context, iterator.next())
                } catch (e: ForEachContextImpl.ContinueException) { // Don't need to do anything }
                }
            }
        }
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
