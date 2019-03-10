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

package net.devrieze.util

import java.lang.reflect.*
import java.util.Arrays
import java.util.HashSet


object Types {

    private val NO_TYPES = emptyArray<Type>()

    /**
     * Determine the type parameters that are given as the concrete type in the
     * implementation of targetType by the basetype.
     *
     * @param targetType The type that has the type parameters that should be resolved.
     * @param baseType The type to use for resolving.
     */
    @JvmStatic
    fun getTypeParametersFor(targetType: Class<*>, baseType: Type): Array<Type> {
        val rawBaseType = rawType(baseType)
        if (rawBaseType == targetType) {
            return getTypeParameters(baseType)
        } else {
            var sup: Type? = null
            if (!rawBaseType!!.isInterface && targetType.isAssignableFrom(rawBaseType.superclass)) {
                sup = rawBaseType.genericSuperclass
            } else if (targetType.isInterface) {
                for (t in rawBaseType.genericInterfaces) {
                    if (targetType.isAssignableFrom(rawType(t)!!)) {
                        sup = t
                        break
                    }
                }
            }
            if (sup == null) {
                throw IllegalArgumentException("Target type is not assignable from the parameter type")
            }

            val param = getTypeParametersFor(targetType, sup)

            return Array<Type>(param.size) {
                when (val par = param[it]) {
                    is TypeVariable<*> -> {
                        val name = par.name
                        val paramIdx = rawBaseType.typeParameters.indexOfFirst { it.name == name }
                        if (paramIdx < 0) {
                            throw RuntimeException("Unbound type parameter")
                        }
                        (baseType as ParameterizedType).actualTypeArguments[paramIdx]

                    }
                    else               -> par
                }

            }
        }
    }

    private fun getTypeParameters(pType: Type): Array<Type> {
        if (pType is Class<*>) {
            return NO_TYPES
        }
        if (pType is ParameterizedType) {
            return pType.actualTypeArguments
        }
        throw UnsupportedOperationException("Other type parameters not supported")
    }

    private fun rawType(type: Type): Class<*>? {
        if (type is Class<*>) {
            return type
        }
        if (type is ParameterizedType) {
            return type.rawType as Class<*>
        }
        if (type is WildcardType) {
            var result: Class<*>? = null
            for (t in type.upperBounds) {
                val c = rawType(t)
                if (result == null || result.isAssignableFrom(c!!)) {
                    result = c
                }
            }
            return result
        }
        if (type is GenericArrayType) {
            val component = rawType(type.genericComponentType)
            return java.lang.reflect.Array.newInstance(component, 0).javaClass
        }
        if (type is TypeVariable<*>) {
            var result: Class<*>? = null
            for (t in type.bounds) {
                val c = rawType(t)
                if (result == null || result.isAssignableFrom(c!!)) {
                    result = c
                }
            }
            return result
        }
        return null // should not occur
    }

    @JvmStatic
    fun commonAncestor(result: Iterable<Any>): Class<*>? {
        val classes = HashSet<Class<*>>()
        for (o in result) {
            classes.add(o.javaClass)
        }
        return commonAncestorClass(classes)
    }

    private fun commonAncestorClass(pResult: Iterable<Class<*>>): Class<*>? {
        val result = HashSet<Class<*>>()
        for (candidate in pResult) {
            if (result.isEmpty()) {
                result.add(candidate)
            } else {
                val toAdd = HashSet<Class<*>>()
                val it = result.iterator()
                while (it.hasNext()) {
                    val r = it.next()
                    if (r.isAssignableFrom(candidate)) {
                        // do nothing
                    } else {
                        it.remove()
                        val classIterable = Iterators.merge(Arrays.asList(candidate, r.superclass),
                                                            Arrays.asList(*r.interfaces))
                        commonAncestorClass(classIterable)?.let { toAdd.add(it) }
                    }
                }
                result.addAll(toAdd)
            }
        }

        val iterator = result.iterator()
        if (!iterator.hasNext()) {
            return null
        }
        var r = iterator.next()
        while (r.isInterface && iterator.hasNext()) {
            r = iterator.next()
        }
        return r
    }

    @JvmStatic
    fun toRawType(type: Type): Class<*>? {
        return rawType(type)
    }

    @JvmStatic
    fun isInstanceForReflection(clazz: Class<*>, result: Any): Boolean {
        when {
            clazz.isInstance(result)
                 -> return true

            clazz == Int::class.javaPrimitiveType
                 -> return result is Int

            clazz == Long::class.javaPrimitiveType
                 -> return result is Long

            clazz == Short::class.javaPrimitiveType
                 -> return result is Short

            clazz == Char::class.javaPrimitiveType
                 -> return result is Char

            clazz == Boolean::class.javaPrimitiveType
                 -> return result is Boolean

            clazz == Byte::class.javaPrimitiveType
                 -> return result is Byte

            clazz == Float::class.javaPrimitiveType
                 -> return result is Float

            clazz == Double::class.javaPrimitiveType
                 -> return result is Double

            else -> return false
        }
    }

    @JvmStatic
    fun isPrimitiveWrapper(clazz: Class<*>): Boolean {
        return (clazz == Int::class.java || clazz == Long::class.java || clazz == Short::class.java || clazz == Char::class.java
                || clazz == Byte::class.java || clazz == Boolean::class.java || clazz == Double::class.java || clazz == Float::class.java)
    }

    @JvmStatic
    fun isPrimitive(clazz: Class<*>): Boolean {
        return (clazz == Int::class.javaPrimitiveType || clazz == Long::class.javaPrimitiveType || clazz == Short::class.javaPrimitiveType || clazz == Char::class.javaPrimitiveType || clazz == Byte::class.javaPrimitiveType
                || clazz == Boolean::class.javaPrimitiveType || clazz == Double::class.javaPrimitiveType || clazz == Float::class.javaPrimitiveType)
    }

    @JvmStatic
    fun parsePrimitive(clazz: Class<*>, string: String): Any? {
        if (clazz == Long::class.java || clazz == Long::class.javaPrimitiveType) {
            return string.toLong()
        }
        if (clazz == Int::class.java || clazz == Int::class.javaPrimitiveType) {
            return string.toInt()
        }
        if (clazz == Char::class.java || clazz == Char::class.javaPrimitiveType) {
            return string.toInt().toChar()
        }
        if (clazz == Short::class.java || clazz == Short::class.javaPrimitiveType) {
            return string.toShort()
        }
        if (clazz == Byte::class.java || clazz == Byte::class.javaPrimitiveType) {
            return string.toByte()
        }
        if (clazz == Boolean::class.java || clazz == Boolean::class.javaPrimitiveType) {
            return string.toBoolean()
        }
        if (clazz == Float::class.java || clazz == Float::class.javaPrimitiveType) {
            return string.toFloat()
        }
        return if (clazz == Double::class.java || clazz == Double::class.javaPrimitiveType) {
            string.toDouble()
        } else null
    }

    @JvmStatic
    fun classForReflection(pClass: Class<*>): Class<*> {
        if (pClass == Int::class.javaPrimitiveType) {
            return Int::class.java
        }
        if (pClass == Long::class.javaPrimitiveType) {
            return Long::class.java
        }
        if (pClass == Short::class.javaPrimitiveType) {
            return Short::class.java
        }
        if (pClass == Char::class.javaPrimitiveType) {
            return Char::class.java
        }
        if (pClass == Boolean::class.javaPrimitiveType) {
            return Boolean::class.java
        }
        if (pClass == Byte::class.javaPrimitiveType) {
            return Byte::class.java
        }
        if (pClass == Float::class.javaPrimitiveType) {
            return Float::class.java
        }
        return if (pClass == Double::class.javaPrimitiveType) {
            Double::class.java
        } else pClass
    }

}
