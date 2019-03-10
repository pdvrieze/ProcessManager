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


/**
 * A class with some methods to help reflection.
 * Created by pdvrieze on 18/04/16.
 */
object ReflectionUtil {

    fun concreteTypeParams(source: Type, target: Class<*>): Array<Class<*>>? {
        val input = typeParams(source, target) ?: return null
        return Array(input.size) { getClass(input[it]) }
    }

    fun typeParams(source: Type, target: Class<*>): Array<out Type>? {
        if (target == source) {
            return target.typeParameters
        }
        if (source is ParameterizedType) {
            return if (source.rawType === target) {
                source.actualTypeArguments
            } else resolveTypeParams(typeParams(source.rawType, target), source)
            //
            //      Class<?> rawType = getClass(type.getRawType());
            //      for(Type intf: rawType.getGenericInterfaces()) {
            //        Type[] result = resolveTypeParams(typeParams(intf, target), type);
            //        if (result!=null) { return result; }
            //      }
        }
        //    TypeVariable<D>, WildcardType
        if (source is GenericArrayType) {
            return if (!target.isArray) {
                null
            } else arrayOf(source.genericComponentType)
        }
        if (source is TypeVariable<*>) {
            source.bounds.asSequence().mapNotNull { typeParams(it, target) }.firstOrNull()?.let { return it }
        }
        if (source is WildcardType) {
            source.upperBounds.asSequence().mapNotNull { typeParams(it, target) }.firstOrNull()?.let { return it }
        }
        if (source is Class<*>) {
            source.genericInterfaces.asSequence().mapNotNull { typeParams(it, target) }.firstOrNull()?.let { return it }
            return typeParams(source.genericSuperclass, target)
        }
        return null
    }

    fun getClass(type: Type): Class<*> {
        if (type is Class<*>) return type
        if (type is ParameterizedType) return getClass(type.rawType)
        if (type is TypeVariable<*>) return getClass(type.bounds[0])
        if (type is WildcardType) return getClass(type.upperBounds[0])
        if (type is GenericArrayType) return java.lang.reflect.Array.newInstance(getClass(type.genericComponentType), 0).javaClass
        throw IllegalArgumentException("Unknown subtype of type")
    }

    private fun resolveTypeParams(source: Array<out Type>?, reference: ParameterizedType): Array<Type>? {
        if (source == null) {
            return source
        }
        return Array<Type>(source.size) { i->
            val s = source[i]
            val v = (reference.rawType as Class<*>).typeParameters
            (s as? TypeVariable<*>)?.let { tv ->
                var r: Type? = null
                for (j in v.indices) {
                    if (source[i] === v[j]) {
                        r = reference.actualTypeArguments[j]
                        break
                    }
                }
                r
            } ?: s
        }
    }

}
