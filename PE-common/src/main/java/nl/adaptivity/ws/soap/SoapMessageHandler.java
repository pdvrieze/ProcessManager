package nl.adaptivity.ws.soap;

import net.devrieze.util.PrefixMap;
import net.devrieze.util.PrefixMap.Entry;
import net.devrieze.util.ValueCollection;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.util.HttpMessage;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Element;

import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SoapMessageHandler {

  private static volatile Map<Object, SoapMessageHandler> mInstances;

  private Map<Class<?>, PrefixMap<Method>> cache;

  private final Object mTarget;


  public static SoapMessageHandler newInstance(final Object pTarget) {
    if (mInstances == null) {
      mInstances = new ConcurrentHashMap<>();
      final SoapMessageHandler instance = new SoapMessageHandler(pTarget);
      mInstances.put(pTarget, instance);
      return instance;
    }
    if (!mInstances.containsKey(pTarget)) {
      synchronized (mInstances) {
        SoapMessageHandler instance = mInstances.get(pTarget);
        if (instance == null) {
          instance = new SoapMessageHandler(pTarget);
          mInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return mInstances.get(pTarget);
    }
  }

  private SoapMessageHandler(final Object pTarget) {
    mTarget = pTarget;
  }

  public boolean processRequest(final HttpMessage pRequest, final HttpServletResponse pResponse) throws IOException {
    final Source source = pRequest.getContent();

    Source result;
    try {
      result = processSource(source, pRequest.getAttachments());
    } catch (final HttpResponseException e) {
      pResponse.sendError(e.getResponseCode(), e.getMessage());
      return false;
    }

    if (pResponse != null) {
      SoapMethodWrapper.marshalResult(pResponse, result);
      return true;
    }
    return false;
  }


  public Source processMessage(final DataSource pDataSource, final Map<String, DataSource> pAttachments) throws IOException {

    Source source;
    if (pDataSource instanceof Source) {
      source = (Source) pDataSource;
    } else {
      source = new StreamSource(pDataSource.getInputStream());
    }

    return processSource(source, pAttachments);
  }

  private Source processSource(final Source source, final Map<String, DataSource> pAttachments) {
    final Envelope envelope = JAXB.unmarshal(source, Envelope.class);
    final List<Object> operations = envelope.getBody().getAny();
    Element operationElem = null;
    for (final Object operationObject : operations) {
      if (operationObject instanceof Element) {
        operationElem = (Element) operationObject;
        break;
      }
    }
    if (operationElem == null) {
      throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "Operation element not found");
    }

    final QName operation = new QName(operationElem.getNamespaceURI(), operationElem.getLocalName());

    final SoapMethodWrapper method = getMethodFor(operation, mTarget);

    if (method != null) {
      method.unmarshalParams(envelope, pAttachments);
      method.exec();
      return method.getResultSource();
    }
    throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "Operation "+operation+" not found");
  }


  private SoapMethodWrapper getMethodFor(final QName pOperation, final Object target) {
    //    final Method[] candidates = target.getClass().getDeclaredMethods();
    final Collection<Method> candidates = getCandidatesFor(target.getClass(), pOperation);
    for (final Method candidate : candidates) {
      final WebMethod annotation = candidate.getAnnotation(WebMethod.class);

      if ((annotation != null)
          && (((annotation.operationName().length() == 0) && candidate.getName().equals(pOperation.getLocalPart())) || annotation.operationName().equals(pOperation.getLocalPart()))) {
        return new SoapMethodWrapper(target, candidate);
      }

    }
    return null;
  }


  private Collection<Method> getCandidatesFor(final Class<?> pClass, final QName pOperation) {
    if (cache == null) {
      cache = new HashMap<>();
    }
    PrefixMap<Method> v = cache.get(pClass);
    if (v == null) {
      v = createCacheElem(pClass);
      cache.put(pClass, v);
    }
    final Collection<Entry<Method>> w = v.get(pOperation.getLocalPart());
    if (w == null) {
      return Collections.emptyList();
    }

    return new ValueCollection<>(w);
  }

  private static PrefixMap<Method> createCacheElem(final Class<?> pClass) {
    final PrefixMap<Method> result = new PrefixMap<>();
    final Method[] methods = pClass.getDeclaredMethods();

    for (final Method m : methods) {
      final WebMethod annotation = m.getAnnotation(WebMethod.class);
      if (annotation != null) {
        String operation = annotation.operationName();
        if (operation.length() == 0) {
          operation = m.getName();
        }
        result.put(operation, m);
      }
    }
    return result;
  }

  public static boolean canHandle(final Class<?> pClass) {
    for (final Method m : pClass.getMethods()) {
      final WebMethod an = m.getAnnotation(WebMethod.class);
      if (an != null) {
        return true;
      }
    }
    return false;
  }

  public static boolean isSoapMessage(final HttpServletRequest pRequest) {
    return "application/soap+xml".equals(pRequest.getContentType());
  }


}
