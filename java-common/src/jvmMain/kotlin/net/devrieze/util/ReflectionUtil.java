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

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;


/**
 * A class with some methods to help reflection.
 * Created by pdvrieze on 18/04/16.
 */
public final class ReflectionUtil {

  private ReflectionUtil() {}

  public static Class<?>[] concreteTypeParams(@NotNull final Type source, @NotNull final Class<?> target) {
    final Type[] in = typeParams(source, target);
    if (in==null) { return null; }
    final Class<?>[] result = new Class<?>[in.length];
    for (int i = 0; i < in.length; i++) {
      result[i] = getClass(in[i]);
    }
    return result;
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static Type[] typeParams(@NotNull final Type source, @NotNull final Class<?> target) {
    if (target.equals(source)) {
      return target.getTypeParameters();
    }
    if (source instanceof ParameterizedType) {
      final ParameterizedType type = (ParameterizedType) source;
      if (type.getRawType()==target) {
        return type.getActualTypeArguments();
      }
//
//      Class<?> rawType = getClass(type.getRawType());
//      for(Type intf: rawType.getGenericInterfaces()) {
//        Type[] result = resolveTypeParams(typeParams(intf, target), type);
//        if (result!=null) { return result; }
//      }
      return resolveTypeParams(typeParams(type.getRawType(), target), type);
    }
    //    TypeVariable<D>, WildcardType
    if (source instanceof GenericArrayType) {
      if (! (target.isArray())) { return null; }
      final GenericArrayType type = (GenericArrayType) source;
      return new Type[] { type.getGenericComponentType() };
    }
    if (source instanceof TypeVariable<?>) {
      final TypeVariable<?> type = (TypeVariable<?>) source;
      for(final Type bound: type.getBounds()) {
        final Type[] result = typeParams(bound, target);
        if (result!=null) { return result; }
      }
    }
    if (source instanceof WildcardType) {
      final WildcardType type = (WildcardType) source;
      for(final Type bound: type.getUpperBounds()) {
        final Type[] result = typeParams(bound, target);
        if (result!=null) { return result; }
      }
    }
    if (source instanceof Class<?>) {
      final Class<?> type = (Class<?>) source;
      for(final Type bound: type.getGenericInterfaces()) {
        final Type[] result = typeParams(bound, target);
        if (result!=null) { return result; }
      }
      return typeParams(type.getGenericSuperclass(), target);
    }
    return null;
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static Class<?> getClass(final Type type) {
    if (type instanceof Class) return (Class<?>) type;
    if (type instanceof ParameterizedType) return getClass(((ParameterizedType)type).getRawType());
    if (type instanceof TypeVariable) return getClass(((TypeVariable)type).getBounds()[0]);
    if (type instanceof WildcardType) return getClass(((WildcardType)type).getUpperBounds()[0]);
    if (type instanceof GenericArrayType) return Array.newInstance(getClass(((GenericArrayType)type).getGenericComponentType()),0).getClass();
    throw new IllegalArgumentException("Unknown subtype of type");
  }

  private static Type[] resolveTypeParams(final Type[] source, final ParameterizedType reference) {
    if (source==null) { return source; }
    final Type[] result = new Type[source.length];
    outer: for (int i = 0; i < source.length; i++) {
      if (source[i] instanceof TypeVariable) {
        final TypeVariable<?>[] v = ((Class<?>) reference.getRawType()).getTypeParameters();
        for (int j = 0; j < v.length; j++) {
          if (source[i]==v[j]) {
            result[i] = reference.getActualTypeArguments()[j];
            continue outer;
          }
        }
      }
      result[i] = source[i];
    }
    return result;
  }

}
