package nl.adaptivity.process.util

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.isPrefixDeclaredInElement
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlEncode

@OptIn(ExperimentalXmlUtilApi::class)
class PrefixCompactFragmentSerializer: XmlSerializer<CompactFragment> {
    private val delegate = CompactFragmentSerializer
    override val descriptor: SerialDescriptor = SerialDescriptor("nl.adaptivity.process.util.PrefixCompactFragmentSerializer", delegate.descriptor)

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: CompactFragment, isValueChild: Boolean) =
        delegate.serializeXML(encoder, output, value, isValueChild)

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: CompactFragment?,
        isValueChild: Boolean
    ): CompactFragment {
        return when {
            isValueChild -> input.extSiblingsToFragment()

            else -> decoder.decodeStructure(CompactFragmentSerializer.descriptor) {
                input.next()
                input.extSiblingsToFragment()
            }
        }
    }

    private fun XmlReader.extSiblingsToFragment(): CompactFragment {
        val appendable: Appendable = StringBuilder()
        when {
            isStarted -> {}
            hasNext() -> next()
            else -> return CompactFragment("")
        }

        val missingNamespaces: MutableMap<String, String> = mutableMapOf()
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0


        var type: EventType? = eventType

        // record the default namespace if it is not the null namespace
        if (type == EventType.START_ELEMENT) {
            getNamespaceURI("")?.let {
                if (it.isNotEmpty()) missingNamespaces[""] = it }
        }

        while (type !== EventType.END_DOCUMENT && (type !== EventType.END_ELEMENT || depth > initialDepth)) {
            when (type) {
                EventType.START_ELEMENT -> {
                    val out = KtXmlWriter(appendable, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None)
                    try {
                        out.indentString = "" // disable indents
                        writeCurrent(out) // writes the start tag
                        out.extAddUndeclaredNamespaces(this, missingNamespaces)
                        out.extWriteElementContent(missingNamespaces, this) // writes the children and end tag
                    } finally { out.close() }
                }
                EventType.IGNORABLE_WHITESPACE ->
                    if (text.isNotEmpty()) appendable.append(text.xmlEncode())

                EventType.TEXT,
                EventType.CDSECT -> {
                    @OptIn(XmlUtilInternal::class)
                    extractNamespacesFromText(text, this, SimpleNamespaceContext(), missingNamespaces)
                    appendable.append(text.xmlEncode())
                }

                else -> {
                } // ignore
            }
            type = if (hasNext()) next() else break
        }

        if (missingNamespaces[""] == "") missingNamespaces.remove("")

        @OptIn(XmlUtilInternal::class)
        return CompactFragment(SimpleNamespaceContext(missingNamespaces), appendable.toString())

    }

    private fun XmlWriter.extWriteElementContent(missingNamespaces: MutableMap<String, String>, reader: XmlReader) {
        reader.forEach { type ->
            // We already moved to the next event. Add the namespaces before writing as for a DOM implementation
            // it is too late to do it afterwards.
            if (reader.eventType == EventType.START_ELEMENT) {
                @Suppress("DEPRECATION")
                extAddUndeclaredNamespaces(reader, missingNamespaces)
            }

            reader.writeCurrent(this)

            when (type) {
                EventType.START_ELEMENT -> extWriteElementContent(missingNamespaces, reader)

                EventType.TEXT,
                EventType.CDSECT -> extractNamespacesFromText(reader.text, reader, namespaceContext as IterableNamespaceContext, missingNamespaces)


                EventType.END_ELEMENT -> return

                else -> { }
            }
        }
    }

    private fun XmlWriter.extAddUndeclaredNamespaces(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
        assert(reader.eventType === EventType.START_ELEMENT)
        val prefix = reader.prefix
        if (!missingNamespaces.containsKey(prefix)) {
            val uri = reader.namespaceURI
            // the uri must be non-empty and also not already present in the output.
            if (uri.isNotEmpty() && getNamespaceUri(prefix) != uri) missingNamespaces[prefix] = uri
        }

        for (attrIdx in 0 until reader.attributeCount) {
            val prefix = reader.getAttributePrefix(attrIdx)

            when (prefix) {
                "", "xmlns" -> {}

                else -> if (!missingNamespaces.containsKey(prefix)) {
                    val uri = reader.getAttributeNamespace(attrIdx)
                    if (getNamespaceUri(prefix) != uri || !reader.isPrefixDeclaredInElement(prefix)) {
                        missingNamespaces[prefix] = uri
                    }
                }
            }

            extractNamespacesFromText(reader.getAttributeValue(attrIdx), reader, namespaceContext as IterableNamespaceContext, missingNamespaces)
        }

        for ((prefix, _) in reader.namespaceDecls) {
            missingNamespaces.remove(prefix)
        }
    }

    private fun extractNamespacesFromText(text: String, reader: XmlReader, outContext: IterableNamespaceContext, missingNamespaces: MutableMap<String, String>) {
        for (candidate in PREFIX_EXTRACTOR.findAll(text)) {
            val prefix = candidate.groupValues[1]
            val ns = reader.getNamespaceURI(prefix) ?: continue
            if (prefix !in missingNamespaces && outContext.getNamespaceURI(prefix) != ns) {
                missingNamespaces[prefix] = ns
            }
        }
    }


    override fun serialize(encoder: Encoder, value: CompactFragment) =
        delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): CompactFragment = delegate.deserialize(decoder)

    companion object {
        private val PREFIX_EXTRACTOR=Regex("\\b([a-zA-Z]+):")
    }
}
