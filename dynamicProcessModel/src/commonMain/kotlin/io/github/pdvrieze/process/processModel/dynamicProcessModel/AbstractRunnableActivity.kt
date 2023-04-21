package io.github.pdvrieze.process.processModel.dynamicProcessModel

import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.ValueHolder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import net.devrieze.util.TypecheckingCollection
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.serialutil.nonNullSerializer
import nl.adaptivity.util.CombiningReader
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.isXmlWhitespace
import nl.adaptivity.xmlutil.serialization.XML
import java.io.CharArrayReader
import java.io.StringReader

abstract class AbstractRunnableActivity<I: Any, O: Any, C: ActivityInstanceContext>(
    builder: Builder<I, O, C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
): MessageActivityBase(builder.checkDefines(), newOwner, otherNodes), ExecutableActivity {
    init {
        checkPredSuccCounts()
    }

    final override val predecessor: Identifiable get() = predecessors.single()

    final override val successor: Identifiable get() = successors.single()

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    protected val inputCombiner: InputCombiner<I> = builder.inputCombiner

    val outputSerializer: SerializationStrategy<O>? = builder.outputSerializer

    final override val condition: ExecutableCondition? = builder.condition?.toExecutableCondition()

    final override val accessRestrictions: RunnableAccessRestriction? = builder.accessRestrictions

    final val onActivityProvided: RunnableActivity.OnActivityProvided<I, O, C> = builder.onActivityProvided

    @Suppress("UNCHECKED_CAST")
    override val defines: List<RunnableActivity.DefineType<*>>
        get() = super.defines as List<RunnableActivity.DefineType<*>>

    abstract class Builder<I : Any, O : Any, C: ActivityInstanceContext> : BaseBuilder, ExecutableProcessNode.Builder,
        MessageActivity.Builder {

        var inputCombiner: InputCombiner<I> = InputCombiner()

        var outputSerializer: SerializationStrategy<O>?

        final override var message: IXmlMessage?

        final override var accessRestrictions: RunnableAccessRestriction?

        var onActivityProvided: RunnableActivity.OnActivityProvided<I, O, C>

        final override val results: MutableCollection<IXmlResultType>
            get() = super.results

        final override val defines: MutableCollection<IXmlDefineType>
            get() = TypecheckingCollection(RunnableActivity.DefineType::class, super.defines)

        constructor(
            predecessor: Identified,
            refNode: Identified?,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: RunnableActivity.OnActivityProvided<I, O, C> = RunnableActivity.OnActivityProvided.DEFAULT,
        ) : super() {
            this.predecessor = predecessor
            this.outputSerializer = outputSerializer
            this.accessRestrictions = accessRestrictions
            this.message = message
            this.onActivityProvided = onActivityProvided

            if (inputSerializer == Unit.serializer()) {
                @Suppress("UNCHECKED_CAST")
                inputCombiner = InputCombiner.UNIT as InputCombiner<I>
            } else {
                defineInput<I>("input", refNode, refName, inputSerializer)
            }

            when (outputSerializer) {
                null,
                Unit.serializer() -> {
                }
                else              -> results.add(XmlResultType("output"))
            }

        }

        constructor(
            predecessor: Identified,
            inputCombiner: InputCombiner<I> = InputCombiner(),
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: RunnableActivity.OnActivityProvided<I, O, C> = RunnableActivity.OnActivityProvided.DEFAULT
        ) : super() {
            this.predecessor = predecessor
            results.add(XmlResultType("output"))
            this.outputSerializer = outputSerializer
            this.inputCombiner = inputCombiner
            this.accessRestrictions = accessRestrictions
            this.message = message
            this.onActivityProvided = onActivityProvided
        }

        constructor(activity: AbstractRunnableActivity<I, O, C>) : super(node = activity) {
            this.inputCombiner = activity.inputCombiner
            this.outputSerializer = activity.outputSerializer
            this.accessRestrictions = activity.accessRestrictions
            this.message = activity.message
            this.onActivityProvided = activity.onActivityProvided
        }

        fun defineInput(
            refNode: Identified?,
            valueName: String,
            deserializer: DeserializationStrategy<I>
        ): InputCombiner.InputValue<I> {
            val defineType = RunnableActivity.DefineType("input", refNode, valueName, null, deserializer)
            defines.add(defineType)
            return InputValueImpl("input")
        }

        fun <T : Any> defineInput(
            name: String,
            refNode: Identified?,
            valueName: String,
            deserializer: DeserializationStrategy<T>
        ): InputCombiner.InputValue<T> {
            val defineType = RunnableActivity.DefineType(name, refNode, valueName, null, deserializer)
            defines.add(defineType)
            return InputValueImpl(name)
        }

        fun defineInput(refNode: Identified?, deserializer: DeserializationStrategy<I>): InputCombiner.InputValue<I> {
            val defineType = RunnableActivity.DefineType("input", refNode, "", null, deserializer)
            defines.add(defineType)
            return InputValueImpl("input")
        }

        fun <T : Any> defineInput(
            name: String,
            refNode: Identified?,
            deserializer: DeserializationStrategy<T>
        ): InputCombiner.InputValue<T> {
            val defineType = RunnableActivity.DefineType(name, refNode, "", null, deserializer)
            defines.add(defineType)
            return InputValueImpl(name)
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R {
            return visitor.visitGenericActivity(this)
        }

        class InputValueImpl<V>(override val name: String) : InputCombiner.InputValue<V>
    }

    fun getInputData(data: List<ProcessData>): I {
        val mappedData = defines.associate { define ->
            val ser: DeserializationStrategy<Any> = define.deserializer.nonNullSerializer()
            val elementData = data.singleOrNull() { it.name == define.name }
                ?: throw NoSuchElementException("Could not find single define with name ${define.refName}")
            val elementChars = elementData.content.content
            val firstNonWsPos = elementChars.indexOfFirst { !isXmlWhitespace(it) }
            val startsWithtag = if(firstNonWsPos<0) false else elementChars[firstNonWsPos]=='<'


            try {
                if (startsWithtag) {
                    val valueReader = elementData.contentStream
                    define.name to XML.decodeFromReader(ser, valueReader)
                } else {

                    val valueReader = KtXmlReader(CombiningReader(StringReader("<w>"),CharArrayReader(elementData.content.content), StringReader("</w>")))
                    val value = XML.decodeFromReader(ValueHolder.Serializer(ser), valueReader, QName("w")).value
                    define.name to value
                }
            } catch (e: XmlException) {
                throw ProcessException("Failure to read data for define ${id}.${define.name}. The data was: \"${elementData.content.contentString}\"", e)
            } catch (e: SerializationException) {
                throw ProcessException("Failure to read data for define ${id}.${define.name}. The data was: \"${elementData.content.contentString}\"", e)
            }
        }

        return inputCombiner(mappedData)
    }

    override fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean {
        return condition?.isOtherwise == true
    }

    override fun evalCondition(
        nodeInstanceSource: IProcessInstance,
        predecessor: IProcessNodeInstance,
        nodeInstance: IProcessNodeInstance
    ): ConditionResult {
        return condition.evalNodeStartCondition(nodeInstanceSource, predecessor, nodeInstance)
    }

    override fun <C : ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *>,
        assignedUser: PrincipalCompat?
    ): Boolean {
//        if (assignedUser == null) throw ProcessException("Message activities must have a user assigned for 'taking' them")
        if (instance.assignedUser != null) throw ProcessException("Users should not have been assigned before being taken")
        if (!activityContext.canBeAssignedTo(assignedUser)) {
            throw ProcessException("User $assignedUser is not valid for activity")
        }

        instance.assignedUser = assignedUser
        return true
    }
}

private fun <R : AbstractRunnableActivity.Builder<*, *, *>> R.checkDefines(): R = apply {
    val illegalDefine = defines.firstOrNull { it !is RunnableActivity.DefineType<*> }
    if (illegalDefine != null) {
        throw IllegalArgumentException("Invalid define $illegalDefine in runnable activity")
    }
}
