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

/*
 * Created on Oct 15, 2004
 */

package net.devrieze.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask


/**
 * This class implements a thread that writes everything in an
 * [InputStream]to an [OutputStream]. The thread is automatically
 * stopped when the InputStream is closed. The main function of this class is
 * for implementing pipes between external commands.
 *
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
class InputStreamOutputStream
/**
 * Create a new InputStreamOutputStream. This class is private to ensure that
 * the [.getInputStreamOutputStream]factory
 * method is used. That is needed as the constructor can not bring the class
 * in the desired consistent state.
 *
 * @param pInputStream The InputStream to read from.
 * @param pOutputStream The OutputStream to write to.
 */
private constructor(
        /**
         * The InputStream that must be read.
         */
        private val inputStream: InputStream,
        /**
         * The OutputStream that must be written to.
         */
        private val outputStream: OutputStream) : Callable<Boolean> {

    /**
     * The call function for this object. This function needs to be public for the
     * object to be callable. The private nature of the constructor should ensure
     * that it can not actually be used except through the factory method.
     *
     * @return [Boolean.TRUE]if nothing went wrong, else it throws an
     * exception.
     * @throws IOException if something went wrong in the reading and writing
     */
    @Throws(IOException::class)
    override fun call(): Boolean? {
        try {
            val buffer = ByteArray(BUFFERSIZE)
            generateSequence { inputStream.read(buffer) }
                .takeWhile { it>=0 }
                .forEach { outputStream.write(buffer, 0, it) }
        } finally {
            inputStream.close()
            outputStream.close()
        }
        return java.lang.Boolean.TRUE
    }

    companion object {

        private val BUFFERSIZE = 4048

        /**
         * This static factory method will create an InputStreamOutputStream and start
         * it.
         *
         * @param pInputStream The stream to read from.
         * @param pOutputStream The stream to write to.
         * @return A [Future]object that will return [Boolean.TRUE]when no
         * errors occurred. Else the Future will contain an exception.
         */
        fun getInputStreamOutputStream(pInputStream: InputStream, pOutputStream: OutputStream): Future<Boolean> {
            DebugTool.ensureParamNotNull(pInputStream)
            DebugTool.ensureParamNotNull(pOutputStream)

            val s = InputStreamOutputStream(pInputStream, pOutputStream)
            val f = FutureTask(s)
            Thread(f).start()

            return f
        }

        @Throws(IOException::class)
        fun writeToOutputStream(pInputStream: InputStream, pOutputStream: OutputStream) {
            DebugTool.ensureParamNotNull(pInputStream)
            DebugTool.ensureParamNotNull(pOutputStream)
            val s = InputStreamOutputStream(pInputStream, pOutputStream)
            s.call()
        }
    }

}
