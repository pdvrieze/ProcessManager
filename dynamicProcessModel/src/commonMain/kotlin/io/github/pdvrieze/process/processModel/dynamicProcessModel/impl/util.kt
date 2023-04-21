package io.github.pdvrieze.process.processModel.dynamicProcessModel.impl

import kotlinx.serialization.*
import kotlinx.serialization.builtins.NothingSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import net.devrieze.util.Tripple
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.util.CompactFragment


inline fun <reified T: Any> payload(name: String, value: T): CompactFragment {
    return payload(Tripple(QName(name), value, serializer<T>()))
}

inline fun <reified T1: Any, reified T2: Any> payload(name1: String, value1: T1, name2: String, value2: T2): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer<T1>()),
        Tripple(QName(name2), value2, serializer<T2>()),
    )
}

inline fun <reified T1: Any, reified T2: Any, reified T3: Any> payload(
    name1: String, value1: T1,
    name2: String, value2: T2,
    name3: String, value3: T3,
): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer<T1>()),
        Tripple(QName(name2), value2, serializer<T2>()),
        Tripple(QName(name3), value3, serializer<T3>()),
    )
}

inline fun <reified T1: Any, reified T2: Any, reified T3: Any, reified T4: Any> payload(
    name1: String, value1: T1,
    name2: String, value2: T2,
    name3: String, value3: T3,
    name4: String, value4: T4,
): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer<T1>()),
        Tripple(QName(name2), value2, serializer<T2>()),
        Tripple(QName(name3), value3, serializer<T3>()),
        Tripple(QName(name4), value4, serializer<T4>()),
    )
}

fun payload(vararg values: Tripple<QName, Any, out KSerializer<*>>): CompactFragment {
    return CompactFragment { out ->
        XML { autoPolymorphic=true }.run {
            for((name, value, ser) in values) {
                val valueHolder = ValueHolder(name, value)
                @Suppress("UNCHECKED_CAST")
                val typedSer: KSerializer<Any> = ser as KSerializer<Any>
                encodeToWriter(out, ValueHolder.Serializer(typedSer), valueHolder)
            }
        }
    }
}

@Serializable(ValueHolder.Serializer::class)
class ValueHolder<T> constructor(val name: QName, val value: T) {

    class Serializer<T>(val childSerializer: KSerializer<T>): KSerializer<ValueHolder<T>> {

        constructor(serializationStrategy: SerializationStrategy<T>): this(serializationStrategy.toSerializer())

        constructor(deserializationStrategy: DeserializationStrategy<T>): this(deserializationStrategy.toSerializer())

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ValueHolder", childSerializer.descriptor) {
            element("value", childSerializer.descriptor)
        }

        override fun serialize(encoder: Encoder, value: ValueHolder<T>) {
            if (encoder is XML.XmlOutput) {
                val xml = encoder.delegateFormat()

                when (childSerializer.descriptor.kind) {
                    SerialKind.ENUM, is PrimitiveKind ->
                        xml.encodeToWriter(encoder.target, childSerializer, value.value, value.name)

                    else ->
                        encoder.target.smartStartTag(value.name) {
                            xml.encodeToWriter(encoder.target, childSerializer, value.value)
                        }
                }

            } else {
                encoder.encodeStructure(descriptor) {
                    encodeSerializableElement(descriptor, 0, childSerializer, value.value)
                }
            }
        }

        override fun deserialize(decoder: Decoder): ValueHolder<T> {
            if(decoder is XML.XmlInput) {
                val xml  = decoder.delegateFormat()
                val input = decoder.input
                while(input.eventType.isIgnorable) { input.nextTag() }
                input.require(EventType.START_ELEMENT, null)
                val name = input.name
                val innerValue = when (childSerializer.descriptor.kind) {
                    SerialKind.ENUM, is PrimitiveKind -> {
                        xml.decodeFromReader(childSerializer, decoder.input, name)
                    }

                    else -> {
                        input.next() // Go to element conent
                        val value = xml.decodeFromReader(childSerializer, input)
                        input.next()
                        input.require(EventType.END_ELEMENT, name)
                        value
                    }

                }
                return ValueHolder(name, innerValue)
            } else {
                return decoder.decodeStructure(descriptor) {
                    if(decodeElementIndex(descriptor)!=0) throw SerializationException("Value holders have only one element")
                    val value = decodeSerializableElement(descriptor, 0, childSerializer)
                    if (decodeElementIndex(descriptor)!= CompositeDecoder.DECODE_DONE) throw SerializationException("failing to end element")
                    ValueHolder(QName(childSerializer.descriptor.serialName), value)
                }
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueHolder<*>
        if(! name.isEquivalent(other.name)) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "ValueHolder(name=$name, value=$value)"
    }
}

fun <T> SerializationStrategy<T>.toSerializer(): KSerializer<T> {
    if (this is KSerializer<T>) return this
    return object : KSerializer<T> {
        override val descriptor: SerialDescriptor
            get() = this@toSerializer.descriptor

        override fun deserialize(decoder: Decoder): T {
            throw UnsupportedOperationException("This wrapper wraps a single serialization strategy, it does not support deserialization")
        }

        override fun serialize(encoder: Encoder, value: T) {
            this@toSerializer.serialize(encoder, value)
        }
    }
}

fun <T> DeserializationStrategy<T>.toSerializer(): KSerializer<T> {
    if (this is KSerializer<T>) return this
    return object : KSerializer<T> {
        override val descriptor: SerialDescriptor
            get() = this@toSerializer.descriptor

        override fun deserialize(decoder: Decoder): T {
            return this@toSerializer.deserialize(decoder)
        }

        override fun serialize(encoder: Encoder, value: T) {
            throw UnsupportedOperationException("This wrapper wraps a single deserialization strategy, it does not support derialization")
        }
    }
}
