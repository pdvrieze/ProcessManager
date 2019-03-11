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

package nl.adaptivity.ws.rest

import net.devrieze.util.PrefixMap
import nl.adaptivity.rest.annotations.HttpMethod
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.util.HttpMessage
import nl.adaptivity.xmlutil.XmlException

import javax.servlet.http.HttpServletResponse
import javax.xml.transform.TransformerException

import java.io.IOException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class RestMessageHandler private constructor(private val target: Any) {

    private var cache: MutableMap<Class<*>, EnumMap<HttpMethod, PrefixMap<Method>>>? = null

    @Throws(IOException::class)
    fun processRequest(method: HttpMethod, request: HttpMessage, response: HttpServletResponse): Boolean {

        val methodWrapper = getMethodFor(method, request)

        if (methodWrapper != null) {
            try {
                methodWrapper.unmarshalParams(request)
                methodWrapper.exec()
                methodWrapper.marshalResult(response)
            } catch (e: XmlException) {
                throw IOException(e)
            } catch (e: TransformerException) {
                throw IOException(e)
            }

            return true
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
        return false
    }

    private fun getMethodFor(httpMethod: HttpMethod, httpMessage: HttpMessage): RestMethodWrapper? {
        val candidates = getCandidatesFor(httpMethod, httpMessage.requestPath)
        var result: RestMethodWrapper? = null
        var resultAnnotation: RestMethod? = null

        for (candidate in candidates) {
            val annotation = candidate.getAnnotation(RestMethod::class.java)
            val pathParams = HashMap<String, String>()

            if (annotation != null && annotation.method === httpMethod
                && pathFits(pathParams, annotation.path, httpMessage.requestPath)
                && conditionsSatisfied(annotation.get, annotation.post, annotation.query, httpMessage)) {
                if (resultAnnotation == null || isMoreSpecificThan(resultAnnotation, annotation)) {
                    result = RestMethodWrapper[target, candidate]
                    result.setPathParams(pathParams)
                    resultAnnotation = annotation
                }
            }

        }
        return result
    }

    private fun getCandidatesFor(pHttpMethod: HttpMethod, pPathInfo: String?): Collection<Method> {
        val targetClass = target.javaClass
        var v: EnumMap<HttpMethod, PrefixMap<Method>>?
        if (cache == null) {
            cache = HashMap()
            v = null
        } else {
            v = cache!![targetClass]
        }
        if (v == null) {
            v = createCacheElem(targetClass)
            cache!![targetClass] = v
        }
        val w = v[pHttpMethod] ?: return emptyList()

        return w.getPrefixValues(pPathInfo!!)
    }

    // XXX Determine whether this request is a rest request for this source or not
    fun isRestRequest(pHttpMethod: HttpMethod, pRequest: HttpMessage): Boolean {
        val candidates = getCandidatesFor(pHttpMethod, pRequest.requestPath)
        for (candidate in candidates) {
            val annotation = candidate.getAnnotation(RestMethod::class.java)
            val pathParams = HashMap<String, String>()

            if (annotation != null && annotation.method === pHttpMethod
                && pathFits(pathParams, annotation.path, pRequest.requestPath)
                && conditionsSatisfied(annotation.get, annotation.post, annotation.query, pRequest)) {
                return true
            }

        }
        return false
    }

    companion object {

        @Volatile
        private var instances: MutableMap<Any, RestMessageHandler>? = null


        fun newInstance(target: Any): RestMessageHandler {
            val instances = instances ?: (ConcurrentHashMap<Any, RestMessageHandler>().also { instances ->
                this.instances = instances
                return RestMessageHandler(target).also { instances[target] = it }
            })

            return if (!instances.containsKey(target)) {
                synchronized(instances) {
                    return instances.getOrPut(target) { RestMessageHandler(target) }
                }
            } else {
                instances[target]!!
            }
        }


        private fun isMoreSpecificThan(pBaseAnnotation: RestMethod, pAnnotation: RestMethod): Boolean {
            // TODO more sophisticated filtering
            if (pBaseAnnotation.path.length < pAnnotation.path.length) {
                return true
            }
            val postdiff = pBaseAnnotation.post.size - pAnnotation.post.size
            val getdiff = pBaseAnnotation.get.size - pAnnotation.get.size
            val querydiff = pBaseAnnotation.query.size - pAnnotation.query.size
            return (postdiff < 0 && getdiff <= 0 && querydiff <= 0 || postdiff <= 0 && getdiff < 0 && querydiff <= 0
                    || postdiff <= 0 && getdiff <= 0 && querydiff < 0)
        }

        private fun createCacheElem(pClass: Class<*>): EnumMap<HttpMethod, PrefixMap<Method>> {
            val result = EnumMap<HttpMethod, PrefixMap<Method>>(HttpMethod::class.java)
            val methods = pClass.declaredMethods

            for (m in methods) {
                val annotation = m.getAnnotation(RestMethod::class.java)
                if (annotation != null) {
                    val prefix = getPrefix(annotation.path)
                    val operation = annotation.method
                    var x: PrefixMap<Method>? = result[operation]
                    if (x == null) {
                        x = PrefixMap()
                        result[operation] = x
                    }
                    x.put(prefix, m)
                }
            }
            return result
        }

        private fun getPrefix(pPath: String): String {
            var i = pPath.indexOf('$')
            var j = 0
            var result: StringBuilder? = null
            while (i >= 0) {
                if (i + 1 < pPath.length && pPath[i + 1] == '$') {
                    if (result == null) {
                        result = StringBuilder()
                    }
                    result.append(pPath.substring(j, i + 1))
                    j = i + 2
                    i = pPath.indexOf('$', i + 2)
                } else {
                    return pPath.substring(0, i)
                }
            }
            if (result != null) {
                if (j + 1 < pPath.length) {
                    result.append(pPath.substring(j))
                }
                return result.toString()
            }
            return pPath
        }

        private fun conditionsSatisfied(pGet: Array<String>,
                                        pPost: Array<String>,
                                        pQuery: Array<String>,
                                        pRequest: HttpMessage): Boolean {
            for (condition in pGet) {
                if (!conditionGetSatisfied(condition, pRequest)) {
                    return false
                }
            }
            for (condition in pPost) {
                if (!conditionPostSatisfied(condition, pRequest)) {
                    return false
                }
            }
            for (condition in pQuery) {
                if (!conditionParamSatisfied(condition, pRequest)) {
                    return false
                }
            }
            return true
        }

        private fun conditionGetSatisfied(pCondition: String, pRequest: HttpMessage): Boolean {
            val i = pCondition.indexOf('=')
            val param: String
            val value: String?
            if (i > 0) {
                param = pCondition.substring(0, i)
                value = pCondition.substring(i + 1)
            } else {
                param = pCondition
                value = null
            }
            val `val` = pRequest.getParam(param)
            return `val` != null && (value == null || value == `val`)
        }

        private fun conditionPostSatisfied(pCondition: String, pRequest: HttpMessage): Boolean {
            val i = pCondition.indexOf('=')
            val param: String
            val value: String?
            if (i > 0) {
                param = pCondition.substring(0, i)
                value = pCondition.substring(i + 1)
            } else {
                param = pCondition
                value = null
            }
            val `val` = pRequest.getPosts(param)
            return `val` != null && (value == null || value == `val`)
        }

        private fun conditionParamSatisfied(pCondition: String, pRequest: HttpMessage): Boolean {
            val i = pCondition.indexOf('=')
            val param: String
            val value: String?
            if (i > 0) {
                param = pCondition.substring(0, i)
                value = pCondition.substring(i + 1)
            } else {
                param = pCondition
                value = null
            }
            val `val` = pRequest.getParam(param)
            return `val` != null && (value == null || value == `val`)
        }

        private fun pathFits(paramMatch: MutableMap<String, String>, pattern: String, query: String?): Boolean {
            var i = 0
            var j = 0
            while (i < pattern.length) {
                if (j >= query!!.length) {
                    return false
                }
                val c = pattern[i]
                if (c == '$' && pattern.length > i + 1 && pattern[i + 1] == '{') {
                    i += 2
                    val k = i
                    while (i < pattern.length && pattern[i] != '}') {
                        ++i
                    }
                    if (i >= pattern.length) {
                        // Not valid parameter, treat like no parameter
                        i -= 2
                        if (c != query[j]) {
                            return false
                        }
                    } else {
                        val paramName = pattern.substring(k, i)
                        val paramValue: String
                        if (pattern.length > i + 1) {
                            val delim = pattern[i + 1]
                            val l = j
                            while (j < query.length && query[j] != delim) {
                                ++j
                            }
                            if (j >= query.length) {
                                return false
                            }
                            paramValue = query.substring(l, j)
                        } else {
                            paramValue = query.substring(j)
                            j = query.length
                        }
                        paramMatch[paramName] = paramValue
                    }
                } else {
                    if (c != query[j]) {
                        return false
                    }
                    if (c == '$' && pattern.length > i + 1 && pattern[i + 1] == '$') {
                        ++i
                    }
                }
                ++i
                ++j
            }
            return if (j + 1 == query!!.length) query[j] == '/' else j >= query.length
        }

        fun canHandle(pClass: Class<*>): Boolean {
            for (m in pClass.methods) {
                val an = m.getAnnotation(RestMethod::class.java)
                if (an != null) {
                    return true
                }
            }
            return false
        }
    }


}
