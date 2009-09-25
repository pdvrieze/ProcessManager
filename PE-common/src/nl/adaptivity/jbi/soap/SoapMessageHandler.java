package nl.adaptivity.jbi.soap;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jws.WebMethod;
import javax.xml.namespace.QName;

import net.devrieze.util.PrefixMap;
import net.devrieze.util.ValueCollection;
import net.devrieze.util.PrefixMap.Entry;

import nl.adaptivity.jbi.util.AttachmentMap;


public class SoapMessageHandler {

  private static SoapMessageHandler aInstance;

  private Map<Class<?>,PrefixMap<Method>> cache;


  public static SoapMessageHandler newInstance() {
    if (aInstance == null) {
      aInstance = new SoapMessageHandler();
    }
    return aInstance;
  }

  private SoapMessageHandler() {}

  public boolean processRequest(QName operation, NormalizedMessage message, NormalizedMessage reply, Object target) throws MessagingException{
    SoapMethodWrapper method = getMethodFor(operation, target);

    if (method !=null) {
      method.unmarshalParams(message.getContent(), new AttachmentMap(message));
      method.exec();
      if (reply!=null) {
        method.marshalResult(reply);
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

      if (annotation !=null &&
          annotation.operationName()==pOperation.getLocalPart()) {
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
        result.put(operation, m);
      }
    }
    return result;
  }


}
