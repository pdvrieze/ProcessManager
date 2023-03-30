package nl.adaptivity.process.engine.pma.dynamic

import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputCombiner
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableAction
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.util.Identified

class RunnablePmaActivity<I: Any, O: Any, C: DynamicPMAActivityContext<I, O, C, *>>(
    builder: Builder<I, O, C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : RunnableActivity<I, O, C>(builder, newOwner, otherNodes) {


    class Builder<I: Any, O: Any, C: DynamicPMAActivityContext<I, O, C, *>>: RunnableActivity.Builder<I, O, C> {
        constructor(
            predecessor: Identified,
            refNode: Identified?,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>?,
            action: RunnableAction<I, O, C>
        ) : super(predecessor, refNode, refName, inputSerializer, outputSerializer, action)

        constructor(
            predecessor: Identified,
            inputCombiner: InputCombiner<I>,
            outputSerializer: SerializationStrategy<O>?,
            action: RunnableAction<I, O, C>
        ) : super(predecessor, inputCombiner, outputSerializer, action)

        constructor(activity: RunnablePmaActivity<I, O, C>) : super(activity)
    }
}
