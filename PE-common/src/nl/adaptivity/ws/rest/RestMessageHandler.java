package nl.adaptivity.ws.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import net.devrieze.util.PrefixMap;

import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;


public class RestMessageHandler {

  private static volatile Map<Object, RestMessageHandler> aInstances;

  private Map<Class<?>, EnumMap<HttpMethod, PrefixMap<Method>>> cache;

  private final Object aTarget;


  public static RestMessageHandler newInstance(final Object pTarget) {
    if (aInstances == null) {
      aInstances = new ConcurrentHashMap<>();
      final RestMessageHandler instance = new RestMessageHandler(pTarget);
      aInstances.put(pTarget, instance);
      return instance;
    }
    if (!aInstances.containsKey(pTarget)) {
      synchronized (aInstances) {
        RestMessageHandler instance = aInstances.get(pTarget);
        if (instance == null) {
          instance = new RestMessageHandler(pTarget);
          aInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return aInstances.get(pTarget);
    }
  }

  private RestMessageHandler(final Object pTarget) {
    aTarget = pTarget;
  }

  public boolean processRequest(final HttpMethod pMethod, final HttpMessage pRequest, final HttpServletResponse pResponse) throws IOException {
    final HttpMessage httpMessage = pRequest;

    final RestMethodWrapper method = getMethodFor(pMethod, httpMessage);

    if (method != null) {
      method.unmarshalParams(httpMessage);
      try {
        method.exec();
      } catch (final RuntimeException e) {
        pResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        throw e;
      }
      try {
        method.marshalResult(pResponse);
      } catch (final TransformerException e) {
        throw new IOException(e);
      }
      return true;
    } else {
      pResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    return false;
  }

  /**
   * TODO This could actually be cached, so reflection only needs to be done
   * once!
   */
  private RestMethodWrapper getMethodFor(final HttpMethod pHttpMethod, final HttpMessage pHttpMessage) {
    final Collection<Method> candidates = getCandidatesFor(pHttpMethod, pHttpMessage.getRequestPath());
    RestMethodWrapper result = null;
    RestMethod resultAnnotation = null;
    for (final Method candidate : candidates) {
      final RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      final Map<String, String> pathParams = new HashMap<>();

      if ((annotation != null) && (annotation.method() == pHttpMethod)
          && pathFits(pathParams, annotation.path(), pHttpMessage.getRequestPath())
          && conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), pHttpMessage)) {
        if ((resultAnnotation == null) || isMoreSpecificThan(resultAnnotation, annotation)) {
          result = new RestMethodWrapper(aTarget, candidate);
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
    if (((postdiff < 0) && (getdiff <= 0) && (querydiff <= 0)) || ((postdiff <= 0) && (getdiff < 0) && (querydiff <= 0))
        || ((postdiff <= 0) && (getdiff <= 0) && (querydiff < 0))) {
      return true;
    }
    return false;
  }

  private Collection<Method> getCandidatesFor(final HttpMethod pHttpMethod, final String pPathInfo) {
    final Class<? extends Object> targetClass = aTarget.getClass();
    if (cache == null) {
      cache = new HashMap<>();
    }
    EnumMap<HttpMethod, PrefixMap<Method>> v = cache.get(targetClass);
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

  private static EnumMap<HttpMethod, PrefixMap<Method>> createCacheElem(final Class<? extends Object> pClass) {
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
    final String val = pRequest.getPost(param);
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

  private static boolean pathFits(final Map<String, String> pParamMatch, final String pPath, final String pPathInfo) {
    int i = 0;
    int j = 0;
    while (i < pPath.length()) {
      if (j >= pPathInfo.length()) {
        return false;
      }
      final char c = pPath.charAt(i);
      if ((c == '$') && (pPath.length() > (i + 1)) && (pPath.charAt(i + 1) == '{')) {
        i += 2;
        final int k = i;
        while ((i < pPath.length()) && (pPath.charAt(i) != '}')) {
          ++i;
        }
        if (i >= pPath.length()) {
          // Not valid parameter, treat like no parameter
          i -= 2;
          if (c != pPathInfo.charAt(j)) {
            return false;
          }
        } else {
          final String paramName = pPath.substring(k, i);
          final String paramValue;
          if (pPath.length() > (i + 1)) {
            final char delim = pPath.charAt(i + 1);
            final int l = j;
            while ((j < pPathInfo.length()) && (pPathInfo.charAt(j) != delim)) {
              ++j;
            }
            if (j >= pPathInfo.length()) {
              return false;
            }
            paramValue = pPathInfo.substring(l, j);
          } else {
            paramValue = pPathInfo.substring(j);
          }
          pParamMatch.put(paramName, paramValue);
        }
      } else {
        if (c != pPathInfo.charAt(j)) {
          return false;
        }
        if ((c == '$') && (pPath.length() > (i + 1)) && (pPath.charAt(i + 1) == '$')) {
          ++i;
        }
      }
      ++i;
      ++j;
    }
    return j >= pPathInfo.length();
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
