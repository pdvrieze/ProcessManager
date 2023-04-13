package nl.adaptivity.process.processModel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Base interface for all event nodes (start, event, end) */
interface IEventNode : ProcessNode {
    override fun builder(): Builder
    val eventType: Type?

    interface Builder: ProcessNode.Builder {
        var eventType: Type?
    }


    @Serializable
    enum class Type {
        @SerialName("message") MESSAGE,
        @SerialName("timer") TIMER,
        @SerialName("error") ERROR,
        @SerialName("escalation") ESCALATION,
        @SerialName("cancel") CANCEL,
        @SerialName("compensation") COMPENSATION,
        @SerialName("conditional") CONDITIONAL,
        @SerialName("link") LINK,
        @SerialName("signal") SIGNAL,
        @SerialName("terminate") TERMINATE,
        @SerialName("multiple") MULTIPLE,
        @SerialName("parallelMultiple") PARALLEL_MULTIPLE,
    }

}
