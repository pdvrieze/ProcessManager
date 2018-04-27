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

package nl.adaptivity.ws.rest;

import net.devrieze.util.PrefixMap;
import nl.adaptivity.rest.annotations.HttpMethod;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.xml.XmlException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class RestMessageHandler {

  private static volatile Map<Object, RestMessageHandler> mInstances;

  private Map<Class<?>, EnumMap<HttpMethod, PrefixMap<Method>>> cache;

  private final Object target;


  public static RestMessageHandler newInstance(final Object pTarget) {
    if (mInstances == null) {
      mInstances = new ConcurrentHashMap<>();
      final RestMessageHandler instance = new RestMessageHandler(pTarget);
      mInstances.put(pTarget, instance);
      return instance;
    }
    if (!mInstances.containsKey(pTarget)) {
      synchronized (mInstances) {
        RestMessageHandler instance = mInstances.get(pTarget);
        if (instance == null) {
          instance = new RestMessageHandler(pTarget);
          mInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return mInstances.get(pTarget);
    }
  }

  private RestMessageHandler(final Object pTarget) {
    target = pTarget;
  }

  public boolean processRequest(final HttpMethod pMethod, final HttpMessage pRequest, final HttpServletResponse pResponse) throws IOException {

    final RestMethodWrapper method = getMethodFor(pMethod, pRequest);

    if (method != null) {
      try {
        method.unmarshalParams(pRequest);
        method.exec();
        method.marshalResult(pResponse);
      } catch (final XmlException | TransformerException e) {
        throw new IOException(e);
      }
      return true;
    } else {
      pResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    return false;
  }

  private RestMethodWrapper getMethodFor(final HttpMethod httpMethod, final HttpMessage httpMessage) {
    final Collection<Method> candidates = getCandidatesFor(httpMethod, httpMessage.getRequestPath());
    RestMethodWrapper result = null;
    RestMethod resultAnnotation = null;

    for (final Method candidate : candidates) {
      final RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      final Map<String, String> pathParams = new HashMap<>();

      if ((annotation != null) && (annotation.method() == httpMethod)
          && pathFits(pathParams, annotation.path(), httpMessage.getRequestPath())
          && conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), httpMessage)) {
        if ((resultAnnotation == null) || isMoreSpecificThan(resultAnnotation, annotation)) {
          result = RestMethodWrapper.Companion.get(target, candidate);
          result.setPathParams(pathParams);
          resultAnnotation = annotation;
        }
      }

    }
    return result;
  }


  private static boolean isMoreSpecificThan(final RestMethod pBaseAnnotation, final RestMethod pAnnotation) {
    // TODO more sophisticated filtering
    if (pBaseAnnotation.path().length() < pAnnotation.path().length()) {
      return true;
    }
    final int postdiff = pBaseAnnotation.post().length - pAnnotation.post().length;
    final int getdiff = pBaseAnnotation.get().length - pAnnotation.get().length;
    final int querydiff = pBaseAnnotation.query().length - pAnnotation.query().length;
    return ((postdiff < 0) && (getdiff <= 0) && (querydiff <= 0)) || ((postdiff <= 0) && (getdiff < 0) && (querydiff <= 0))
            || ((postdiff <= 0) && (getdiff <= 0) && (querydiff < 0));
  }

  private Collection<Method> getCandidatesFor(final HttpMethod pHttpMethod, final String pPathInfo) {
    final Class<?> targetClass = target.getClass();
    EnumMap<HttpMethod, PrefixMap<Method>> v;
    if (cache == null) {
      cache = new HashMap<>();
      v = null;
    } else {
      v = cache.get(targetClass);
    }
    if (v == null) {
      v = createCacheElem(targetClass);
      cache.put(targetClass, v);
    }
    final PrefixMap<Method> w = v.get(pHttpMethod);
    if (w == null) {
      return Collections.emptyList();
    }

    return w.getPrefixValues(pPathInfo);
  }

  private static EnumMap<HttpMethod, PrefixMap<Method>> createCacheElem(final Class<?> pClass) {
    final EnumMap<HttpMethod, PrefixMap<Method>> result = new EnumMap<>(HttpMethod.class);
    final Method[] methods = pClass.getDeclaredMethods();

    for (final Method m : methods) {
      final RestMethod annotation = m.getAnnotation(RestMethod.class);
      if (annotation != null) {
        final String prefix = getPrefix(annotation.path());
        final HttpMethod operation = annotation.method();
        PrefixMap<Method> x = result.get(operation);
        if (x == null) {
          x = new PrefixMap<>();
          result.put(operation, x);
        }
        x.put(prefix, m);
      }
    }
    return result;
  }

  private static String getPrefix(final String pPath) {
    int i = pPath.indexOf('$');
    int j = 0;
    StringBuilder result = null;
    while (i >= 0) {
      if (((i + 1) < pPath.length()) && (pPath.charAt(i + 1) == '$')) {
        if (result == null) {
          result = new StringBuilder();
        }
        result.append(pPath.substring(j, i + 1));
        j = i + 2;
        i = pPath.indexOf('$', i + 2);
      } else {
        return pPath.substring(0, i);
      }
    }
    if (result != null) {
      if ((j + 1) < pPath.length()) {
        result.append(pPath.substring(j));
      }
      return result.toString();
    }
    return pPath;
  }

  private static boolean conditionsSatisfied(final String[] pGet, final String[] pPost, final String[] pQuery, final HttpMessage pRequest) {
    for (final String condition : pGet) {
      if (!conditionGetSatisfied(condition, pRequest)) {
        return false;
      }
    }
    for (final String condition : pPost) {
      if (!conditionPostSatisfied(condition, pRequest)) {
        return false;
      }
    }
    for (final String condition : pQuery) {
      if (!conditionParamSatisfied(condition, pRequest)) {
        return false;
      }
    }
    return true;
  }

  private static boolean conditionGetSatisfied(final String pCondition, final HttpMessage pRequest) {
    final int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i > 0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i + 1);
    } else {
      param = pCondition;
      value = null;
    }
    final String val = pRequest.getParam(param);
    return (val != null) && ((value == null) || value.equals(val));
  }

  private static boolean conditionPostSatisfied(final String pCondition, final HttpMessage pRequest) {
    final int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i > 0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i + 1);
    } else {
      param = pCondition;
      value = null;
    }
    final String val = pRequest.getPosts(param);
    return (val != null) && ((value == null) || value.equals(val));
  }

  private static boolean conditionParamSatisfied(final String pCondition, final HttpMessage pRequest) {
    final int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i > 0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i + 1);
    } else {
      param = pCondition;
      value = null;
    }
    final String val = pRequest.getParam(param);
    return (val != null) && ((value == null) || value.equals(val));
  }

  private static boolean pathFits(final Map<String, String> paramMatch, final String pattern, final String query) {
    int i = 0;
    int j = 0;
    while (i < pattern.length()) {
      if (j >= query.length()) {
        return false;
      }
      final char c = pattern.charAt(i);
      if ((c == '$') && (pattern.length() > (i + 1)) && (pattern.charAt(i + 1) == '{')) {
        i += 2;
        final int k = i;
        while ((i < pattern.length()) && (pattern.charAt(i) != '}')) {
          ++i;
        }
        if (i >= pattern.length()) {
          // Not valid parameter, treat like no parameter
          i -= 2;
          if (c != query.charAt(j)) {
            return false;
          }
        } else {
          final String paramName = pattern.substring(k, i);
          final String paramValue;
          if (pattern.length() > (i + 1)) {
            final char delim = pattern.charAt(i + 1);
            final int l = j;
            while ((j < query.length()) && (query.charAt(j) != delim)) {
              ++j;
            }
            if (j >= query.length()) {
              return false;
            }
            paramValue = query.substring(l, j);
          } else {
            paramValue = query.substring(j);
            j= query.length();
          }
          paramMatch.put(paramName, paramValue);
        }
      } else {
        if (c != query.charAt(j)) {
          return false;
        }
        if ((c == '$') && (pattern.length() > (i + 1)) && (pattern.charAt(i + 1) == '$')) {
          ++i;
        }
      }
      ++i;
      ++j;
    }
    return j+1==query.length() ? query.charAt(j)=='/' : j >= query.length();
  }

  public static boolean canHandle(final Class<?> pClass) {
    for (final Method m : pClass.getMethods()) {
      final RestMethod an = m.getAnnotation(RestMethod.class);
      if (an != null) {
        return true;
      }
    }
    return false;
  }

  // XXX Determine whether this request is a rest request for this source or not
  public boolean isRestRequest(final HttpMethod pHttpMethod, final HttpMessage pRequest) {
    final Collection<Method> candidates = getCandidatesFor(pHttpMethod, pRequest.getRequestPath());
    for (final Method candidate : candidates) {
      final RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      final Map<String, String> pathParams = new HashMap<>();

      if ((annotation != null) && (annotation.method() == pHttpMethod)
          && pathFits(pathParams, annotation.path(), pRequest.getRequestPath())
          && conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), pRequest)) {
        return true;
      }

    }
    return false;
  }


}
