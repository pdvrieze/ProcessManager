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

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


// Suppresses instanceof chain checks. The OO failure is in the Java library, not this code.


public final class Types {

  private static final Type[] NO_TYPES = new Type[0];

  private Types() {}

  /**
   * Determine the type parameters that are given as the concrete type in the
   * implementation of targetType by the basetype.
   *
   * @param targetType The type that has the type parameters that should be resolved.
   * @param baseType The type to use for resolving.
   */
  public static Type[] getTypeParametersFor(final Class<?> targetType, final Type baseType) {
    final Class<?> rawBaseType = rawType(baseType);
    if (rawBaseType == targetType) {
      return getTypeParameters(baseType);
    } else {
      Type sup = null;
      if (!rawBaseType.isInterface() && targetType.isAssignableFrom(rawBaseType.getSuperclass())) {
        sup = rawBaseType.getGenericSuperclass();
      } else if (targetType.isInterface()) {
        for (final Type t : rawBaseType.getGenericInterfaces()) {
          if (targetType.isAssignableFrom(rawType(t))) {
            sup = t;
            break;
          }
        }
      }
      if (sup == null) {
        throw new IllegalArgumentException("Target type is not assignable from the parameter type");
      }

      final Type[] param = getTypeParametersFor(targetType, sup);
      final Type[] result = new Type[param.length];
      for (int i = 0; i < param.length; ++i) {
        final Type par = param[i];
        if (par instanceof TypeVariable<?>) {
          final String name = ((TypeVariable<?>) par).getName();
          final TypeVariable<?>[] myParams = rawBaseType.getTypeParameters();
          int j;
          for (j = 0; j < myParams.length; ++j) {
            if (myParams[j].getName().equals(name)) {
              break;
            }
          }
          if (j >= myParams.length) {
            throw new RuntimeException("Unbound type parameter");
          }
          result[i] = ((ParameterizedType) baseType).getActualTypeArguments()[j];

        } else {
          result[i] = par;
        }
      }
      return result;
    }
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  private static Type[] getTypeParameters(final Type pType) {
    if (pType instanceof Class) {
      return NO_TYPES;
    }
    if (pType instanceof ParameterizedType) {
      return ((ParameterizedType) pType).getActualTypeArguments();
    }
    throw new UnsupportedOperationException("Other type parameters not supported");
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  private static Class<?> rawType(final Type pType) {
    if (pType instanceof Class) {
      return (Class<?>) pType;
    }
    if (pType instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) pType).getRawType();
    }
    if (pType instanceof WildcardType) {
      final WildcardType wct = (WildcardType) pType;
      Class<?> result = null;
      for (final Type t : wct.getUpperBounds()) {
        final Class<?> c = rawType(t);
        if ((result == null) || result.isAssignableFrom(c)) {
          result = c;
        }
      }
      return result;
    }
    if (pType instanceof GenericArrayType) {
      final GenericArrayType gat = (GenericArrayType) pType;
      final Class<?> component = rawType(gat.getGenericComponentType());
      return Array.newInstance(component, 0).getClass();
    }
    if (pType instanceof TypeVariable) {
      final TypeVariable<?> tv = (TypeVariable<?>) pType;
      Class<?> result = null;
      for (final Type t : tv.getBounds()) {
        final Class<?> c = rawType(t);
        if ((result == null) || result.isAssignableFrom(c)) {
          result = c;
        }
      }
      return result;
    }
    return null; // should not occur
  }

  public static Class<?> commonAncestor(final Iterable<?> pResult) {
    final Set<Class<?>> classes = new HashSet<>();
    for (final Object o : pResult) {
      classes.add(o.getClass());
    }
    return commonAncestorClass(classes);
  }

  private static Class<?> commonAncestorClass(final Iterable<Class<?>> pResult) {
    final Set<Class<?>> result = new HashSet<>();
    for (final Class<?> candidate : pResult) {
      if (result.isEmpty()) {
        result.add(candidate);
      } else {
        final Set<Class<?>> toAdd = new HashSet<>();
        for (final Iterator<Class<?>> it = result.iterator(); it.hasNext();) {
          final Class<?> r = it.next();
          if (r.isAssignableFrom(candidate)) {
            // do nothing
          } else {
            it.remove();
            final Iterable<Class<?>> classIterable = Iterators.<Class<?>> merge(Arrays.<Class<?>> asList(candidate, r.getSuperclass()), Arrays.<Class<?>> asList(r.getInterfaces()));
            toAdd.add(commonAncestorClass(classIterable));
          }
        }
        result.addAll(toAdd);
      }
    }

    final Iterator<Class<?>> iterator = result.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    Class<?> r = iterator.next();
    while (r.isInterface() && iterator.hasNext()) {
      r = iterator.next();
    }
    return r;
  }

  public static Class<?> toRawType(final Type pType) {
    return rawType(pType);
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static boolean isInstanceForReflection(final Class<?> pClass, final Object pResult) {
    if (pClass.isInstance(pResult)) {
      return true;
    }
    if (pClass == int.class) {
      return pResult instanceof Integer;
    }
    if (pClass == long.class) {
      return pResult instanceof Long;
    }
    if (pClass == short.class) {
      return pResult instanceof Short;
    }
    if (pClass == char.class) {
      return pResult instanceof Character;
    }
    if (pClass == boolean.class) {
      return pResult instanceof Boolean;
    }
    if (pClass == byte.class) {
      return pResult instanceof Byte;
    }
    if (pClass == float.class) {
      return pResult instanceof Float;
    }
    if (pClass == double.class) {
      return pResult instanceof Double;
    }
    return false;
  }

  public static boolean isPrimitiveWrapper(final Class<?> pClass) {
    return (pClass == Integer.class) || (pClass == Long.class) || (pClass == Short.class) || (pClass == Character.class)
        || (pClass == Byte.class) || (pClass == Boolean.class) || (pClass == Double.class) || (pClass == Float.class);
  }

  public static boolean isPrimitive(final Class<?> pClass) {
    return (pClass == int.class) || (pClass == long.class) || (pClass == short.class) || (pClass == char.class) || (pClass == byte.class)
        || (pClass == boolean.class) || (pClass == double.class) || (pClass == float.class);
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static Object parsePrimitive(final Class<?> pClass, final String pString) {
    if ((pClass == Long.class) || (pClass == long.class)) {
      return Long.valueOf(pString);
    }
    if ((pClass == Integer.class) || (pClass == int.class)) {
      return Integer.valueOf(pString);
    }
    if ((pClass == Character.class) || (pClass == char.class)) {
      return Character.valueOf((char) Integer.parseInt(pString));
    }
    if ((pClass == Short.class) || (pClass == short.class)) {
      return Short.valueOf(pString);
    }
    if ((pClass == Byte.class) || (pClass == byte.class)) {
      return Byte.valueOf(pString);
    }
    if ((pClass == Boolean.class) || (pClass == boolean.class)) {
      return Boolean.valueOf(pString);
    }
    if ((pClass == Float.class) || (pClass == float.class)) {
      return Float.valueOf(pString);
    }
    if ((pClass == Double.class) || (pClass == double.class)) {
      return Double.valueOf(pString);
    }
    return null;
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static Class<?> classForReflection(final Class<?> pClass) {
    if (pClass == int.class) {
      return Integer.class;
    }
    if (pClass == long.class) {
      return Long.class;
    }
    if (pClass == short.class) {
      return Short.class;
    }
    if (pClass == char.class) {
      return Character.class;
    }
    if (pClass == boolean.class) {
      return Boolean.class;
    }
    if (pClass == byte.class) {
      return Byte.class;
    }
    if (pClass == float.class) {
      return Float.class;
    }
    if (pClass == double.class) {
      return Double.class;
    }
    return pClass;
  }

}
