package nl.adaptivity.process.engine.pma.dynamic.runtime.impl.test

import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.ValueHolder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals

class TestValueHolder {
    @Test
    fun testValueHolderJsonStringSerialization() {
        val value = ValueHolder(QName("foo"), "bar")
        val json = Json.encodeToString(value)
        assertEquals(STRING_JSON, json)
    }

    @Test
    fun testValueHolderJsonClassSerialization() {
        val value = ValueHolder(QName("foo"), Data("foo2", 42))
        val json = Json.encodeToString(value)
        assertEquals(DATA_JSON, json)
    }

    @Test
    fun testValueHolderXmlStringSerialization() {
        val value = ValueHolder(QName("foo"), "bar")
        val json = XML.encodeToString(value)
        assertEquals(STRING_XML, json)
    }

    @Test
    fun testValueHolderXmlClassSerialization() {
        val value = ValueHolder(QName("foo"), Data("foo2", 42))
        val encoded = XML.encodeToString(value)
        assertEquals(DATA_XML, encoded)
    }

    @Test
    fun testValueHolderJsonStringDeserialization() {
        val expected = ValueHolder(QName("kotlin.String"), "bar")
        val actual = Json.decodeFromString<ValueHolder<String>>(STRING_JSON)
        assertEquals(expected, actual)
    }

    @Test
    fun testValueHolderJsonDataDeserialization() {
        val expected = ValueHolder(QName(Data.serializer().descriptor.serialName), Data("foo2", 42))
        val actual = Json.decodeFromString<ValueHolder<Data>>(DATA_JSON)
        assertEquals(expected, actual)
    }

    @Test
    fun testValueHolderXmlStringDeserialization() {
        val expected = ValueHolder(QName("foo"), "bar")
        val actual = XML.decodeFromString<ValueHolder<String>>(STRING_XML)
        assertEquals(expected, actual)
    }

    @Test
    fun testValueHolderXmlDataDeserialization() {
        val expected = ValueHolder(QName("foo"), Data("foo2", 42))
        val actual = XML.decodeFromString<ValueHolder<Data>>(DATA_XML)
        assertEquals(expected, actual)
    }


    @Serializable
    data class Data(val tmp: String, val foo: Int)

    companion object {
        const val STRING_JSON = """{"value":"bar"}"""
        const val DATA_JSON = "{\"value\":{\"tmp\":\"foo2\",\"foo\":42}}"
        const val STRING_XML = "<foo>bar</foo>"
        const val DATA_XML = "<foo><Data tmp=\"foo2\" foo=\"42\"/></foo>"
    }
}
