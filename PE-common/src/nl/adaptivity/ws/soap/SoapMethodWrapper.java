package nl.adaptivity.ws.soap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.w3.soapEnvelope.Body;
import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Node;

import net.devrieze.util.Annotations;
import net.devrieze.util.JAXBCollectionWrapper;
import net.devrieze.util.Tripple;
import net.devrieze.util.Types;

import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.util.activation.Sources;


public class SoapMethodWrapper {

  public static final URI SOAP_ENCODING = URI.create(ProcessConsts.Soap.SOAP_ENCODING_NS);

  private final Object aOwner;

  private final Method aMethod;

  private Object[] aParams;

  private Object aResult;

  public SoapMethodWrapper(final Object pOwner, final Method pMethod) {
    aOwner = pOwner;
    aMethod = pMethod;
  }

  public void unmarshalParams(final Source pSource, final Map<String, DataSource> pAttachments) {
    final Envelope envelope = JAXB.unmarshal(pSource, Envelope.class);
    unmarshalParams(envelope, pAttachments);

  }

  public void unmarshalParams(final Envelope pEnvelope, final Map<String, DataSource> pAttachments) {
    if (aParams != null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }

    ensureNoUnunderstoodHeaders(pEnvelope);
    processSoapHeader(pEnvelope.getHeader());
    final URI es = pEnvelope.getEncodingStyle();
    if ((es == null) || es.equals(SOAP_ENCODING)) {
      processSoapBody(pEnvelope, pAttachments);
    } else {
      throw new MyMessagingException("Ununderstood message body");
    }
  }

  private void ensureNoUnunderstoodHeaders(final Envelope pEnvelope) {
    // TODO Auto-generated method stub
    //
  }

  private void processSoapHeader(final Header pHeader) {
    // TODO Auto-generated method stub
    //
    /* For now just ignore headers, i.e. none understood */
  }

  private void processSoapBody(final org.w3.soapEnvelope.Envelope pEnvelope, final Map<String, DataSource> pAttachments) {
    final Body body = pEnvelope.getBody();
    if (body.getAny().size() != 1) {
      throw new MyMessagingException("Multiple body elements not expected");
    }
    final Node root = (Node) body.getAny().get(0);
    assertRootNode(root);

    final LinkedHashMap<String, Node> params = SoapHelper.getParamMap(root);
    final Map<String, Node> headers = SoapHelper.getHeaderMap(pEnvelope.getHeader());

    final Class<?>[] parameterTypes = aMethod.getParameterTypes();
    final Annotation[][] parameterAnnotations = aMethod.getParameterAnnotations();

    aParams = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; ++i) {
      final WebParam annotation = Annotations.getAnnotation(parameterAnnotations[i], WebParam.class);
      String name;
      if (annotation == null) {
        name = params.keySet().iterator().next();
      } else {
        name = annotation.name();
      }
      Node value;
      if ((annotation != null) && annotation.header()) {
        value = headers.remove(name);
      } else {
        value = params.remove(name);
      }

      if (value == null) {
        throw new MyMessagingException("Parameter \"" + name + "\" not found");
      }
      aParams[i] = SoapHelper.unMarshalNode(aMethod, parameterTypes[i], value);

    }
    if (params.size() > 0) {
      Logger.getLogger(getClass().getCanonicalName()).warning("Extra parameters in message: " + params.keySet().toString());
    }
  }

  private void assertRootNode(final Node pRoot) {
    final WebMethod wm = aMethod.getAnnotation(WebMethod.class);
    if ((wm == null) || wm.operationName().equals("")) {
      if (!pRoot.getLocalName().equals(aMethod.getName())) {
        throw new MyMessagingException("Root node does not correspond to operation name");
      }
    } else {
      if (!pRoot.getLocalName().equals(wm.operationName())) {
        throw new MyMessagingException("Root node does not correspond to operation name");
      }
    }
    final WebService ws = aMethod.getDeclaringClass().getAnnotation(WebService.class);
    if (!((ws == null) || ws.targetNamespace().equals(""))) {
      if (!ws.targetNamespace().equals(pRoot.getNamespaceURI())) {
        throw new MyMessagingException("Root node does not correspond to operation namespace");
      }
    }
  }

  private Object getAttachment(final Class<?> pClass, final String pName, final Map<String, DataHandler> pAttachments) {
    final DataHandler handler = pAttachments.get(pName);
    if (handler != null) {
      if (DataHandler.class.isAssignableFrom(pClass)) {
        return handler;
      }
      if (InputStream.class.isAssignableFrom(pClass)) {
        try {
          return handler.getInputStream();
        } catch (final IOException e) {
          throw new MyMessagingException(e);
        }
      }
      if (DataSource.class.isAssignableFrom(pClass)) {
        return handler.getDataSource();
      }
      try {
        return handler.getContent();
      } catch (final IOException e) {
        throw new MyMessagingException(e);
      }

    }
    return null;
  }

  public void exec() {
    if (aParams == null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      aResult = aMethod.invoke(aOwner, aParams);
    } catch (final IllegalArgumentException e) {
      throw new MyMessagingException(e);
    } catch (final IllegalAccessException e) {
      throw new MyMessagingException(e);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MyMessagingException(cause != null ? cause : e);
    }
  }

  public static void marshalResult(final HttpServletResponse pResponse, final Source pSource) {
    pResponse.setContentType("application/soap+xml");
    try {
      Sources.writeToStream(pSource, pResponse.getOutputStream());
    } catch (final TransformerException e) {
      throw new MyMessagingException(e);
    } catch (final IOException e) {
      throw new MyMessagingException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public Source getResultSource() {
    if (aResult instanceof Source) {
      return (Source) aResult;
    }

    Tripple<String, Class<?>, Object>[] params;
    List<Object> headers;
    if (aResult instanceof ActivityResponse) {
      final ActivityResponse<?> activityResponse = (ActivityResponse<?>) aResult;
      params = new Tripple[] { Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", activityResponse.getReturnType(), activityResponse.getReturnValue()) };
      headers = Collections.<Object> singletonList(aResult);
    } else if ("nl.adaptivity.process.messaging.ActivityResponse".equals(aResult.getClass().getName())) {
      /*
       * If the ActivityResponse was created by a different classloader like
       * when we directly invoke the endpoint through DarwinMessenger shortcut
       * we will have to resort to reflection instead of direct invocation. This
       * should still beat going through tcp-ip hand out.
       */

      Class<?> returnType;
      Object returnValue;
      try {
        returnType = (Class<?>) aResult.getClass().getMethod("getReturnType").invoke(aResult);
        returnValue = aResult.getClass().getMethod("getReturnValue").invoke(aResult);
      } catch (final IllegalArgumentException e) {
        throw new MyMessagingException(e);
      } catch (final SecurityException e) {
        throw new MyMessagingException(e);
      } catch (final IllegalAccessException e) {
        throw new MyMessagingException(e);
      } catch (final InvocationTargetException e) {
        throw new MyMessagingException(e);
      } catch (final NoSuchMethodException e) {
        throw new MyMessagingException(e);
      }
      params = new Tripple[] { Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", returnType, returnValue) };
      headers = Collections.<Object> singletonList(aResult);

    } else {

      params = new Tripple[] { Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", aMethod.getReturnType(), aResult) };
      headers = Collections.emptyList();
    }
    try {
      return SoapHelper.createMessage(new QName(aMethod.getName() + "Response"), headers, params);
    } catch (final JAXBException e) {
      throw new MyMessagingException(e);
    }
  }

  public Object getResult() {
    return aResult;
  }

  private JAXBCollectionWrapper wrapCollection(final Type pType, final Collection<?> pCollection) {
    final JAXBCollectionWrapper collectionWrapper;
    {
      final Class<?> rawType;
      if (pType instanceof ParameterizedType) {
        final ParameterizedType returnType = (ParameterizedType) pType;
        rawType = (Class<?>) returnType.getRawType();
      } else if (pType instanceof Class<?>) {
        rawType = (Class<?>) pType;
      } else if (pType instanceof WildcardType) {
        final Type[] UpperBounds = ((WildcardType) pType).getUpperBounds();
        if (UpperBounds.length > 0) {
          rawType = (Class<?>) UpperBounds[0];
        } else {
          rawType = Object.class;
        }
      } else if (pType instanceof TypeVariable) {
        final Type[] UpperBounds = ((TypeVariable<?>) pType).getBounds();
        if (UpperBounds.length > 0) {
          rawType = (Class<?>) UpperBounds[0];
        } else {
          rawType = Object.class;
        }
      } else {
        throw new IllegalArgumentException("Unsupported type variable");
      }
      Class<?> elementType = null;
      if (Collection.class.isAssignableFrom(rawType)) {
        final Type[] paramTypes = Types.getTypeParametersFor(Collection.class, pType);
        elementType = Types.toRawType(paramTypes[0]);
        if (elementType.isInterface()) {
          // interfaces not supported by jaxb
          elementType = Types.commonAncestor(pCollection);
        }
      } else {
        elementType = Types.commonAncestor(pCollection);
      }
      collectionWrapper = new JAXBCollectionWrapper(pCollection, elementType);
    }
    return collectionWrapper;
  }

  private static QName getQName(final XmlElementWrapper pAnnotation) {
    String nameSpace = pAnnotation.namespace();
    if ("##default".equals(nameSpace)) {
      nameSpace = XMLConstants.NULL_NS_URI;
    }
    final String localName = pAnnotation.name();
    return new QName(nameSpace, localName, XMLConstants.DEFAULT_NS_PREFIX);
  }

}
