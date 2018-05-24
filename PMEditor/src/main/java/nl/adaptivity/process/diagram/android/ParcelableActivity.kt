/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.diagram.android

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor
import nl.adaptivity.process.diagram.DrawableActivity
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.STUB_DRAWABLE_BUILD_HELPER
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.tasks.EditableUserTask
import nl.adaptivity.process.tasks.PostTask
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlStreaming
import nl.adaptivity.xml.toString
import org.w3.soapEnvelope.Envelope
import java.io.StringReader

class ParcelableActivity(builder: Activity.Builder<*, *>,
                                buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?> = STUB_DRAWABLE_BUILD_HELPER) : DrawableActivity(
    builder, buildHelper), Parcelable {
    constructor(orig: Activity<*, *>, compat: Boolean) : this(builder(orig, compat))

    private constructor(source: Parcel) : this(fromParcel(source))


    fun getUserTask(): EditableUserTask? {
        val message = XmlMessage.from(message)
        if (message != null && UserTaskServiceDescriptor.SERVICENAME == message.service &&
            UserTaskServiceDescriptor.ENDPOINT == message.endpoint) {

            val envelope: Envelope<PostTask> = Envelope.deserialize(message.bodyStreamReader, PostTask.FACTORY)
            return envelope.body?.bodyContent?.task
        }
        return null
    }


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompat) 1 else 0).toByte())
        dest.writeString(id)
        dest.writeString(label)
        dest.writeString(name)
        dest.writeDouble(x)
        dest.writeDouble(y)

        dest.writeString(condition)
        dest.writeStringArray(toIdStrings(predecessors))
        dest.writeStringArray(toIdStrings(successors))

        dest.writeString(message?.let { toString(XmlMessage.from(it)) } ?: "")

        writeDefines(dest)
        writeResults(dest)
        dest.writeByte((if (isMultiInstance) 1 else 0).toByte())
    }

    private fun writeDefines(dest: Parcel) {
        val defines = defines
        dest.writeInt(defines.size)
        for (define in defines) {
            dest.writeString(toString(define))
        }
    }

    private fun writeResults(dest: Parcel) {
        val results = results
        dest.writeInt(results.size)
        for (result in results) {
            dest.writeString(toString(result))
        }
    }

    companion object {

        private const val TAG = "ParcelableActivity"

        @Suppress("unused")
        @JvmStatic
        val CREATOR: Parcelable.Creator<ParcelableActivity> = object : Parcelable.Creator<ParcelableActivity> {
            override fun createFromParcel(parcel: Parcel): ParcelableActivity {
                return ParcelableActivity(parcel)
            }

            override fun newArray(size: Int): Array<ParcelableActivity?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        @Deprecated("Use constructor directly", ReplaceWith("ParcelableActivity(orig, compat)"))
        fun newInstance(orig: Activity<*, *>, compat: Boolean): ParcelableActivity {
            return ParcelableActivity(orig, compat)
        }


        @JvmStatic
        private fun builder(orig: Activity<*, *>, compat: Boolean): DrawableActivity.Builder {
            return Builder(orig).apply { isCompat = compat }
        }

        @JvmStatic
        private fun fromParcel(source: Parcel): DrawableActivity.Builder {
            return Builder().apply {
                isCompat = source.readByte().toInt() != 0
                id = source.readString()
                label = source.readString()

                @Suppress("DEPRECATION")
                name = source.readString()

                x = source.readDouble()
                y = source.readDouble()

                condition = source.readString()

                fromIdStrings(source.createStringArray()).forEach { addPredecessor(it.identifier) }
                fromIdStrings(source.createStringArray()).forEach { addSuccessor(it.identifier) }

                val strMessage = source.readString()
                Log.d(TAG, "deserializing message:\n$strMessage")
                if (!strMessage.isNullOrEmpty()) {
                    message = XmlStreaming.deSerialize(StringReader(strMessage), XmlMessage::class.java)
                }

                setDefines(readDefines(source))
                setResults(readResults(source))
                isMultiInstance = source.readByte().toInt() != 0
            }
        }

        @JvmStatic
        private fun readDefines(source: Parcel): List<XmlDefineType> {
            val count = source.readInt()
            return (0 until count).map {
                XmlStreaming.deSerialize(StringReader(source.readString()), XmlDefineType::class.java)
            }
        }

        @JvmStatic
        private fun readResults(source: Parcel): List<XmlResultType> {
            val count = source.readInt()
            return (0 until count).map {
                XmlStreaming.deSerialize(StringReader(source.readString()), XmlResultType::class.java)
            }
        }

        private fun toIdStrings(set: Set<Identifiable>): Array<String> {
            val iter = set.iterator()
            return Array(set.size) { _ -> iter.next().id!! }
        }


        private fun fromIdStrings(stringArray: Array<String>): Collection<Identified> {
            return stringArray.map { Identifier(it) }
        }


    }

}