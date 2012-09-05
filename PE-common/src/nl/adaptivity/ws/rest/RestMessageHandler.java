package nl.adaptivity.ws.rest;

import java.lang.reflect.Method;
import java.util.*;

import javax.xml.bind.JAXB;

import net.devrieze.util.PrefixMap;

import nl.adaptivity.process.engine.NormalizedMessage;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.activation.AttachmentMap;


public class RestMessageHandler {

  private static RestMessageHandler aInstance;

  private Map<Class<?>,EnumMap<HttpMethod, PrefixMap<Method>>> cache;


  public static RestMessageHandler newInstance() {
    if (aInstance == null) {
      aInstance = new RestMessageHandler();
    }
    return aInstance;
  }

  private RestMessageHandler() {}

  public boolean processRequest(HttpMethod operation, NormalizedMessage message, NormalizedMessage reply, Object target) {
    HttpMessage httpMessage = JAXB.unmarshal(message.getContent(),HttpMessage.class);

    RestMethodWrapper method = getMethodFor(operation, httpMessage, target);

    if (method !=null) {
      method.unmarshalParams(httpMessage, new AttachmentMap(message));
      method.exec();
      if (reply!=null) {
        method.marshalResult(reply);
      }
      return true;
    }
    return false;
  }

  private RestMethodWrapper getMethodFor(HttpMethod pHttpMethod, HttpMessage httpMessage, Object target) {
//    final Method[] candidates = target.getClass().getDeclaredMethods();
    Collection<Method> candidates = getCandidatesFor(target.getClass(), pHttpMethod, httpMessage.getPathInfo());
    RestMethodWrapper result = null;
    RestMethod resultAnnotation = null;
    for(Method candidate:candidates) {
      RestMethod annotation = candidate.getAnnotation(RestMethod.class);
      Map<String, String> pathParams = new HashMap<String, String>();

      if (annotation !=null &&
          annotation.method()==pHttpMethod &&
          pathFits(pathParams, annotation.path(), httpMessage.getPathInfo()) &&
          conditionsSatisfied(annotation.get(), annotation.post(), annotation.query(), httpMessage)) {
        if (resultAnnotation==null || isMoreSpecificThan(resultAnnotation, annotation)) {
          result = new RestMethodWrapper(target, candidate);
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

  private Collection<Method> getCandidatesFor(Class<? extends Object> pClass, HttpMethod pHttpMethod, String pPathInfo) {
    if (cache == null) { cache = new HashMap<Class<?>, EnumMap<HttpMethod,PrefixMap<Method>>>(); }
    EnumMap<HttpMethod,PrefixMap<Method>> v = cache.get(pClass);
    if (v==null) {
      v = createCacheElem(pClass);
      cache.put(pClass, v);
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

  private static boolean conditionsSatisfied(String[] pGet, String[] pPost, String[] pQuery, HttpMessage pHttpMessage) {
    for (String condition: pGet) {
      if (! conditionGetSatisfied(condition, pHttpMessage)) {
        return false;
      }
    }
    for (String condition: pPost) {
      if (! conditionPostSatisfied(condition, pHttpMessage)) {
        return false;
      }
    }
    for (String condition: pQuery) {
      if (! conditionParamSatisfied(condition, pHttpMessage)) {
        return false;
      }
    }
    return true;
  }

  private static boolean conditionGetSatisfied(String pCondition, HttpMessage pHttpMessage) {
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
    String val = pHttpMessage.getQuery(param);
    return (val != null) && (value == null || value.equals(val));
  }

  private static boolean conditionPostSatisfied(String pCondition, HttpMessage pHttpMessage) {
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
    String val = pHttpMessage.getPost(param);
    return (val != null) && (value == null || value.equals(val));
  }

  private static boolean conditionParamSatisfied(String pCondition, HttpMessage pHttpMessage) {
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
    String val = pHttpMessage.getParam(param);
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


}
