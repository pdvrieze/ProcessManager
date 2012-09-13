package nl.adaptivity.ws.soap;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebMethod;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;

import net.devrieze.util.PrefixMap;
import net.devrieze.util.PrefixMap.Entry;
import net.devrieze.util.ValueCollection;
import nl.adaptivity.util.HttpMessage;

import org.w3.soapEnvelope.Envelope;


public class SoapMessageHandler {

  private static volatile Map<Object, SoapMessageHandler> aInstances;

  private Map<Class<?>,PrefixMap<Method>> cache;

  private Object aTarget;


  public static SoapMessageHandler newInstance(Object pTarget) {
    if (aInstances == null) {
      aInstances = new ConcurrentHashMap<Object, SoapMessageHandler>();
      SoapMessageHandler instance = new SoapMessageHandler(pTarget);
      aInstances.put(pTarget, instance);
      return instance;
    }
    if (!aInstances.containsKey(pTarget)) {
      synchronized (aInstances) {
        SoapMessageHandler instance = aInstances.get(pTarget);
        if (instance==null) {
          instance = new SoapMessageHandler(pTarget);
          aInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return aInstances.get(pTarget);
    }
  }

  private SoapMessageHandler(Object pTarget) { aTarget = pTarget; }

  public boolean processRequest(HttpMessage pRequest, HttpServletResponse pResponse) {
    Envelope envelope = JAXB.unmarshal(pRequest.getContent(), Envelope.class);
    QName operation = pRequest.getOperation();
    
    SoapMethodWrapper method = getMethodFor(operation, aTarget);

    if (method !=null) {
      method.unmarshalParams(pRequest.getContent(), null);
      method.exec();
      if (pResponse!=null) {
        method.marshalResult(pResponse);
      }
      return true;
    }
    return false;
  }

  private SoapMethodWrapper getMethodFor(QName pOperation, Object target) {
//    final Method[] candidates = target.getClass().getDeclaredMethods();
    Collection<Method> candidates = getCandidatesFor(target.getClass(), pOperation);
    for(Method candidate:candidates) {
      WebMethod annotation = candidate.getAnnotation(WebMethod.class);

      if (annotation !=null && ((annotation.operationName().length()==0 && candidate.getName().equals(pOperation.getLocalPart())) ||
          annotation.operationName().equals(pOperation.getLocalPart()))) {
        SoapMethodWrapper result = new SoapMethodWrapper(target, candidate);
        return result;
      }

    }
    return null;
  }


  private Collection<Method> getCandidatesFor(Class<? extends Object> pClass, QName pOperation) {
    if (cache == null) { cache = new HashMap<Class<?>, PrefixMap<Method>>(); }
    PrefixMap<Method> v = cache.get(pClass);
    if (v==null) {
      v = createCacheElem(pClass);
      cache.put(pClass, v);
    }
    Collection<Entry<Method>> w = v.get(pOperation.getLocalPart());
    if (w == null) { return Collections.emptyList(); }

    return new ValueCollection<Method>(w);
  }

  private PrefixMap<Method> createCacheElem(Class<? extends Object> pClass) {
    PrefixMap<Method> result = new PrefixMap<Method>();
    final Method[] methods = pClass.getDeclaredMethods();

    for(Method m: methods) {
      WebMethod annotation = m.getAnnotation(WebMethod.class);
      if (annotation != null) {
        String operation = annotation.operationName();
        if (operation.length()==0) {
          operation = m.getName();
        }
        result.put(operation, m);
      }
    }
    return result;
  }

  public static boolean canHandle(Class<?> pClass) {
    for (Method m: pClass.getMethods()) {
      WebMethod an = m.getAnnotation(WebMethod.class);
      if (an!=null) { return true; }
    }
    return false;
  }

  public static boolean isSoapMessage(HttpServletRequest pRequest) {
    return "application/soap+xml".equals(pRequest.getContentType());
  }


}
