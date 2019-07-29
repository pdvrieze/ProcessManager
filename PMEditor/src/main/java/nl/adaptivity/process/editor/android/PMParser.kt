/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.editor.android

import android.util.Log
import nl.adaptivity.process.diagram.AbstractLayoutStepper
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.LayoutAlgorithm
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.xmlutil.AndroidXmlReader
import nl.adaptivity.xmlutil.AndroidXmlWriter
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.impl.BetterXmlSerializer
import nl.adaptivity.xmlutil.serialization.XML
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.*
import java.util.*

object PMParser {

    val MIME_TYPE = "application/x-processmodel"

    val NS_PROCESSMODEL = "http://adaptivity.nl/ProcessEngine/"

    private val serializer: BetterXmlSerializer
        @Throws(XmlPullParserException::class)
        get() {
            val serializer = BetterXmlSerializer()
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false)
            return serializer
        }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun serializeProcessModel(out: OutputStream, processModel: RootDrawableProcessModel) {
        val serializer = getSerializer(out)
        try {
            val writer = AndroidXmlWriter(serializer)
            try {
                val serModel = XmlProcessModel(processModel.builder())
                XML().toXml(target= writer, serializer=XmlProcessModel.serializer(), obj=serModel, prefix=null)
                //            processModel.serialize(writer);
            } finally {
                writer.close()
            }
        } finally {
            serializer.endDocument()
            serializer.flush()
        }
    }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun serializeProcessModel(out: Writer, processModel: RootDrawableProcessModel) {
        val serializer = getSerializer(out)
        val writer = AndroidXmlWriter(serializer)
        processModel.serialize(writer)
        writer.close()
    }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun exportProcessModel(out: OutputStream, processModel: RootProcessModel<*>) {
        val sanitizedModel = sanitizeForExport(processModel)
        serializeProcessModel(out, sanitizedModel)
    }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun exportProcessModel(out: Writer, processModel: RootProcessModel<*>) {
        val sanitizedModel = sanitizeForExport(processModel)
        serializeProcessModel(out, sanitizedModel)
    }

    private fun sanitizeForExport(processModel: RootProcessModel<*>): RootDrawableProcessModel {
        val result = RootDrawableProcessModel.get(processModel)!!

        result.setHandleValue(-1)
        return result
    }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun getSerializer(out: OutputStream): XmlSerializer {
        val serializer = serializer
        try {
            serializer.setOutput(out, "UTF-8")
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        } catch (e: IllegalStateException) {
            throw IOException(e)
        } catch (e: IOException) {
            throw IOException(e)
        }

        return serializer
    }

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun getSerializer(out: Writer): XmlSerializer {
        val serializer = serializer
        try {
            serializer.setOutput(out)
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        } catch (e: IllegalStateException) {
            throw IOException(e)
        } catch (e: IOException) {
            throw IOException(e)
        }

        return serializer
    }

    @JvmStatic
    fun parseProcessModel(reader: Reader,
                          simpleLayoutAlgorithm: LayoutAlgorithm,
                          advancedAlgorithm: LayoutAlgorithm): RootDrawableProcessModel.Builder? {
        try {
            return parseProcessModel(AndroidXmlReader(reader), simpleLayoutAlgorithm, advancedAlgorithm)
        } catch (e: Exception) {
            Log.e(PMEditor::class.java.simpleName, e.message, e)
            return null
        }

    }

    @JvmStatic
    fun parseProcessModel(input: InputStream,
                          simpleLayoutAlgorithm: LayoutAlgorithm,
                          advancedAlgorithm: LayoutAlgorithm): RootDrawableProcessModel.Builder? {
        try {
            return parseProcessModel(AndroidXmlReader(input, "UTF-8"), simpleLayoutAlgorithm, advancedAlgorithm)
        } catch (e: Exception) {
            Log.e(PMEditor::class.java.simpleName, e.message, e)
            return null
        }

    }

    @JvmStatic
    @Throws(XmlException::class)
    fun parseProcessModel(input: XmlReader,
                          simpleLayoutAlgorithm: LayoutAlgorithm,
                          advancedAlgorithm: LayoutAlgorithm): RootDrawableProcessModel.Builder {
        val result: RootDrawableProcessModel.Builder = RootDrawableProcessModel.Builder(XML().parse(XmlProcessModel.serializer(), input))
        if (result.uuid == null) {
            result.uuid = UUID.randomUUID()
        }
        if (result.hasUnpositioned()) {
            result.layoutAlgorithm = advancedAlgorithm
            result.layout(AbstractLayoutStepper())
        } else {
            result.layoutAlgorithm = simpleLayoutAlgorithm
        }
        return result
    }

}
