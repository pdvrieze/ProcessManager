package nl.adaptivity.process.engine.pma.dynamic.runtime

import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivityInstance
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.dynamic.RunnablePmaActivity
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.util.multiplatform.PrincipalCompat

class RunnablePmaActivityInstance<I : Any, O : Any, C : DynamicPMAActivityContext<I, O, C, *>>(
    builder: Builder<I, O, C>
) : AbstractRunnableActivityInstance<I, O, C, RunnablePmaActivity<I, O, *>, RunnablePmaActivityInstance<I,O,C>>(builder) {

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<I, O, C> {
        return RunnablePmaActivityInstance.ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<I : Any, O : Any, C : DynamicPMAActivityContext<I, O, C, *>> :
        AbstractRunnableActivityInstance.Builder<I, O, C, RunnablePmaActivity<I, O, *>, RunnablePmaActivityInstance<I,O,C>>


    class BaseBuilder<I : Any, O : Any, C : DynamicPMAActivityContext<I, O, C, *>>(
        node: RunnablePmaActivity<I, O, C>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : AbstractRunnableActivityInstance.BaseBuilder<I, O, C, RunnablePmaActivity<I, O, *>, RunnablePmaActivityInstance<I, O, C>>(
        node, predecessor, processInstanceBuilder, owner,
        entryNo, assignedUser, handle, state
    ), Builder<I, O, C> {

        override fun build(): RunnablePmaActivityInstance<I, O, C> {
            return RunnablePmaActivityInstance(this)
        }
    }

    class ExtBuilder<I : Any, O : Any, C : DynamicPMAActivityContext<I, O, C, *>>(
        base: RunnablePmaActivityInstance<I, O, C>,
        processInstanceBuilder: ProcessInstance.Builder
    ) : AbstractRunnableActivityInstance.ExtBuilder<I, O, C, RunnablePmaActivity<I, O, *>, RunnablePmaActivityInstance<I, O, C>>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override fun build(): RunnablePmaActivityInstance<I, O, C> {
            return if (changed) RunnablePmaActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }
}
