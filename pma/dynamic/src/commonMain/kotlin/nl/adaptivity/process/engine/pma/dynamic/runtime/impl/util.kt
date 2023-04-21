package nl.adaptivity.process.engine.pma.dynamic.runtime.impl

import kotlinx.serialization.*
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
import kotlin.random.Random
import kotlin.random.nextULong


fun Random.nextString() = nextULong().toString(16)

inline fun <reified T> payload(name: String, value: T): CompactFragment {
    return payload(Tripple(QName(name), value, serializer()))
}

inline fun <reified T1, reified T2> payload(name1: String, value1: T1, name2: String, value2: T2): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer()),
        Tripple(QName(name2), value2, serializer()),
    )
}

inline fun <reified T1, reified T2, reified T3> payload(
    name1: String, value1: T1,
    name2: String, value2: T2,
    name3: String, value3: T3,
): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer()),
        Tripple(QName(name2), value2, serializer()),
        Tripple(QName(name3), value3, serializer()),
    )
}

inline fun <reified T1, reified T2, reified T3, reified T4> payload(
    name1: String, value1: T1,
    name2: String, value2: T2,
    name3: String, value3: T3,
    name4: String, value4: T4,
): CompactFragment {
    return payload(
        Tripple(QName(name1), value1, serializer()),
        Tripple(QName(name2), value2, serializer()),
        Tripple(QName(name3), value3, serializer()),
        Tripple(QName(name4), value4, serializer()),
    )
}

fun <T> payload(vararg values: Tripple<QName, T, KSerializer<T>>): CompactFragment {
    return CompactFragment { out ->
        XML { autoPolymorphic=true }.run {
            for((name, value, ser) in values) {
                encodeToWriter(out, ValueHolder.Serializer(ser), ValueHolder<T>(name, value))
            }
        }
    }
}

@Serializable(ValueHolder.Serializer::class)
class ValueHolder<T> constructor(val name: QName, val value: T) {

    class Serializer<T>(val valueSerializer: KSerializer<T>) : KSerializer<ValueHolder<T>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ValueHolder", valueSerializer.descriptor) {
            element("value", valueSerializer.descriptor)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: ValueHolder<T>) {
            if (encoder is XML.XmlOutput) {
                val xml = encoder.delegateFormat()

                when (valueSerializer.descriptor.kind) {
                    SerialKind.ENUM, is PrimitiveKind ->
                        xml.encodeToWriter(encoder.target, valueSerializer, value.value, value.name)

                    else ->
                        encoder.target.smartStartTag(value.name) {
                            xml.encodeToWriter(encoder.target, valueSerializer, value.value)
                        }
                }

            } else {
                encoder.encodeStructure(descriptor) {
                    encodeSerializableElement(descriptor, 0, valueSerializer, value.value)
                }
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): ValueHolder<T> {
            if(decoder is XML.XmlInput) {
                val xml  = decoder.delegateFormat()
                val input = decoder.input
                while(input.eventType.isIgnorable) { input.nextTag() }
                input.require(EventType.START_ELEMENT, null)
                val name = input.name
                val innerValue = when (valueSerializer.descriptor.kind) {
                    SerialKind.ENUM, is PrimitiveKind -> {
                        xml.decodeFromReader(valueSerializer, decoder.input, name)
                    }
                    else -> {
                        input.next() // Go to element conent
                        val value = xml.decodeFromReader(valueSerializer, input)
                        input.next()
                        input.require(EventType.END_ELEMENT, name)
                        value
                    }

                }
                return ValueHolder(name, innerValue)
            } else {
                return decoder.decodeStructure(descriptor) {
                    if(decodeElementIndex(descriptor)!=0) throw SerializationException("Value holders have only one element")
                    val value = decodeSerializableElement(descriptor, 0, valueSerializer)
                    if (decodeElementIndex(descriptor)!= CompositeDecoder.DECODE_DONE) throw SerializationException("failing to end element")
                    ValueHolder(QName(valueSerializer.descriptor.serialName), value)
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
