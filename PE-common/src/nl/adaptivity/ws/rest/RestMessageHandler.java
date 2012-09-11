package nl.adaptivity.ws.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.transform.TransformerException;

import net.devrieze.util.PrefixMap;

import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;


public class RestMessageHandler {

  private static volatile Map<Object, RestMessageHandler> aInstances;

  private Map<Class<?>,EnumMap<HttpMethod, PrefixMap<Method>>> cache;

  private Object aTarget;


  public static RestMessageHandler newInstance(Object pTarget) {
    if (aInstances == null) {
      aInstances = new ConcurrentHashMap<Object, RestMessageHandler>();
      RestMessageHandler instance = new RestMessageHandler(pTarget);
      aInstances.put(pTarget, instance);
      return instance;
    }
    if (!aInstances.containsKey(pTarget)) {
      synchronized (aInstances) {
        RestMessageHandler instance = aInstances.get(pTarget);
        if (instance==null) {
          instance = new RestMessageHandler(pTarget);
          aInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return aInstances.get(pTarget);
    }
  }

  private RestMessageHandler(Object pTarget) { aTarget = pTarget; }

  public boolean processRequest(HttpMethod pMethod, HttpMessage pRequest, HttpServletResponse pResponse) throws IOException {
    // TODO this will not work.
    HttpMessage httpMessage = pRequest;

    RestMethodWrapper method = getMethodFor(pMethod, httpMessage);

    if (method !=null) {
      method.unmarshalParams(httpMessage);
      method.exec();
      try {
        method.marshalResult(pRequest, pResponse);
      } catch (TransformerException e) {
        throw new IOException(e);
      }
      return true;
    }
    return false;
  }

  /**
   * TODO This could actually be cached, so reflection only needs to be done once!
   */
  private RestMethodWrapper getMethodFor(HttpMethod pHttpMethod, HttpMessage pHttpMessage) {
    Collection<Method> candidates = getCandidatesFor(pHttpMethod, pHttpMessage.getPathInfo());
    RestMethodWrapper result = null;
    RestMethod resultAnnotation = null;
    for(Method candidate:candidates) {
      RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      Map<String, String> pathParams = new HashMap<String, String>();

      if (annotation !=null &&
          annotation.method()==pHttpMethod &&
          pathFits(pathParams, annotation.path(), pHttpMessage.getPathInfo()) &&
          conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), pHttpMessage)) {
        if (resultAnnotation==null || isMoreSpecificThan(resultAnnotation, annotation)) {
          result = new RestMethodWrapper(aTarget, candidate);
          result.setPathParams(pathParams);
          resultAnnotation = annotation;
        }
      }

    }
    return result;
  }


  private boolean isMoreSpecificThan(RestMethod pBaseAnnotation, RestMethod pAnnotation) {
    // TODO more sophisticated filtering
    return (pBaseAnnotation.path().length()<pAnnotation.path().length());
  }

  private Collection<Method> getCandidatesFor(HttpMethod pHttpMethod, String pPathInfo) {
    Class<? extends Object> targetClass = aTarget.getClass();
    if (cache == null) { cache = new HashMap<Class<?>, EnumMap<HttpMethod,PrefixMap<Method>>>(); }
    EnumMap<HttpMethod,PrefixMap<Method>> v = cache.get(targetClass);
    if (v==null) {
      v = createCacheElem(targetClass);
      cache.put(targetClass, v);
    }
    PrefixMap<Method> w = v.get(pHttpMethod);
    if (w == null) { return Collections.emptyList(); }

    return w.getPrefixValues(pPathInfo);
  }

  private EnumMap<HttpMethod, PrefixMap<Method>> createCacheElem(Class<? extends Object> pClass) {
    EnumMap<HttpMethod, PrefixMap<Method>> result = new EnumMap<HttpMethod, PrefixMap<Method>>(HttpMethod.class);
    final Method[] methods = pClass.getDeclaredMethods();

    for(Method m: methods) {
      RestMethod annotation = m.getAnnotation(RestMethod.class);
      if (annotation != null) {
        String prefix = getPrefix(annotation.path());
        HttpMethod operation = annotation.method();
        PrefixMap<Method> x = result.get(operation);
        if (x==null) {
          x = new PrefixMap<Method>();
          result.put(operation, x);
        }
        x.put(prefix, m);
      }
    }
    return result;
  }

  private String getPrefix(String pPath) {
    int i =pPath.indexOf('$');
    int j = 0;
    StringBuilder result = null;
    while (i>=0) {
      if (i+1<pPath.length() && pPath.charAt(i+1)=='$') {
        if(result==null) { result = new StringBuilder(); }
        result.append(pPath.substring(j, i+1));
        j = i+2;
        i = pPath.indexOf('$', i+2);
      } else {
        return pPath.substring(0, i);
      }
    }
    if (result!=null) {
      if (j+1<pPath.length()) {
        result.append(pPath.substring(j));
      }
      return result.toString();
    }
    return pPath;
  }

  private static boolean conditionsSatisfied(String[] pGet, String[] pPost, String[] pQuery, HttpMessage pRequest) {
    for (String condition: pGet) {
      if (! conditionGetSatisfied(condition, pRequest)) {
        return false;
      }
    }
    for (String condition: pPost) {
      if (! conditionPostSatisfied(condition, pRequest)) {
        return false;
      }
    }
    for (String condition: pQuery) {
      if (! conditionParamSatisfied(condition, pRequest)) {
        return false;
      }
    }
    return true;
  }

  private static boolean conditionGetSatisfied(String pCondition, HttpMessage pRequest) {
    int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i>0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i+1);
    } else {
      param = pCondition;
      value=null;
    }
    String val = pRequest.getParam(param);
    return (val != null) && (value == null || value.equals(val));
  }

  private static boolean conditionPostSatisfied(String pCondition, HttpMessage pRequest) {
    int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i>0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i+1);
    } else {
      param = pCondition;
      value=null;
    }
    String val = pRequest.getPost(param);
    return (val != null) && (value == null || value.equals(val));
  }

  private static boolean conditionParamSatisfied(String pCondition, HttpMessage pRequest) {
    int i = pCondition.indexOf('=');
    String param;
    String value;
    if (i>0) {
      param = pCondition.substring(0, i);
      value = pCondition.substring(i+1);
    } else {
      param = pCondition;
      value=null;
    }
    String val = pRequest.getParam(param);
    return (val != null) && (value == null || value.equals(val));
  }

  private static boolean pathFits(Map<String, String> pParamMatch, String pPath, String pPathInfo) {
    int i=0;
    int j=0;
    while (i<pPath.length()) {
      if (j>=pPathInfo.length()) {
        return false;
      }
      char c = pPath.charAt(i);
      if (c=='$' && pPath.length()>(i+1) && pPath.charAt(i+1)=='{') {
        i += 2;
        int k=i;
        while (i<pPath.length() && pPath.charAt(i)!='}') {
          ++i;
        }
        if (i>=pPath.length()) {
          // Not valid parameter, treat like no parameter
          i-=2;
          if (c!=pPathInfo.charAt(j)) {
            return false;
          }
        } else {
          final String paramName = pPath.substring(k, i);
          final String paramValue;
          if (pPath.length()>(i+1)) {
            char delim = pPath.charAt(i+1);
            int l = j;
            while (j<pPathInfo.length() && pPathInfo.charAt(j)!=delim) {
              ++j;
            }
            if (j>=pPathInfo.length()) {
              return false;
            }
            paramValue = pPathInfo.substring(l, j);
          } else {
            paramValue = pPathInfo.substring(j);
          }
          pParamMatch.put(paramName, paramValue);
        }
      } else {
        if (c!=pPathInfo.charAt(j)) {
          return false;
        }
        if (c=='$' && pPath.length()>(i+1) && pPath.charAt(i+1)=='$') {
          ++i;
        }
      }
      ++i;
      ++j;
    }
    return j>=pPathInfo.length();
  }

  public static boolean canHandle(Class<?> pClass) {
    for (Method m: pClass.getMethods()) {
      RestMethod an = m.getAnnotation(RestMethod.class);
      if (an!=null) { return true; }
    }
    return false;
  }

  // XXX Determine whether this request is a rest request for this source or not
  public boolean isRestRequest(HttpMethod pHttpMethod, HttpMessage pRequest) {
    Collection<Method> candidates = getCandidatesFor(pHttpMethod, pRequest.getPathInfo());
    for(Method candidate:candidates) {
      RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      Map<String, String> pathParams = new HashMap<String, String>();

      if (annotation !=null &&
          annotation.method()==pHttpMethod &&
          pathFits(pathParams, annotation.path(), pRequest.getPathInfo()) &&
          conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), pRequest)) {
        return true;
      }

    }
    return false;
  }


}
