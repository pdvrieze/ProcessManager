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

package nl.adaptivity.ws

import nl.adaptivity.messaging.MessagingException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Created by pdvrieze on 28/11/15.
 */
abstract class WsMethodWrapper(protected val owner: Any, protected val method: Method) {
    protected lateinit var params: Array<Any?>
    protected open var result: Any? = null

    protected val paramsInitialised: Boolean get() = ::params.isInitialized

    open fun exec() {
        if (! ::params.isInitialized)throw IllegalArgumentException("Argument unmarshalling has not taken place yet")
        val params = params

        try {
            result = method.invoke(owner, *params)
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            throw MessagingException(cause ?: e)
        } catch (e: MessagingException) {
            throw e
        } catch (e: Exception) {
            throw MessagingException(e)
        }

    }
}