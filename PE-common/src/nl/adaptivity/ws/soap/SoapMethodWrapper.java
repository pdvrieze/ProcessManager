package nl.adaptivity.ws.soap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import net.devrieze.util.Annotations;
import net.devrieze.util.Tripple;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.engine.MessagingFormatException;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.util.activation.Sources;

import org.w3.soapEnvelope.Body;
import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Node;


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
      throw new MessagingFormatException("Ununderstood message body");
    }
  }

  private void ensureNoUnunderstoodHeaders(@SuppressWarnings("unused") final Envelope pEnvelope) {
    // TODO Auto-generated method stub
    //
  }

  private void processSoapHeader(@SuppressWarnings("unused") final Header pHeader) {
    // TODO Auto-generated method stub
    //
    /* For now just ignore headers, i.e. none understood */
  }

  private void processSoapBody(final org.w3.soapEnvelope.Envelope pEnvelope, @SuppressWarnings("unused") final Map<String, DataSource> pAttachments) {
    final Body body = pEnvelope.getBody();
    if (body.getAny().size() != 1) {
      throw new MessagingFormatException("Multiple body elements not expected");
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
        throw new MessagingFormatException("Parameter \"" + name + "\" not found");
      }

      final SoapSeeAlso seeAlso = Annotations.getAnnotation(parameterAnnotations[i], SoapSeeAlso.class);
      aParams[i] = SoapHelper.unMarshalNode(aMethod, parameterTypes[i], seeAlso==null ? new Class<?>[0] : seeAlso.value(), value);

    }
    if (params.size() > 0) {
      Logger.getLogger(getClass().getCanonicalName()).warning("Extra parameters in message: " + params.keySet().toString());
    }
  }

  private void assertRootNode(final Node pRoot) {
    final WebMethod wm = aMethod.getAnnotation(WebMethod.class);
    if ((wm == null) || wm.operationName().equals("")) {
      if (!pRoot.getLocalName().equals(aMethod.getName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    } else {
      if (!pRoot.getLocalName().equals(wm.operationName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    }
    final WebService ws = aMethod.getDeclaringClass().getAnnotation(WebService.class);
    if (!((ws == null) || ws.targetNamespace().equals(""))) {
      if (!ws.targetNamespace().equals(pRoot.getNamespaceURI())) {
        throw new MessagingFormatException("Root node does not correspond to operation namespace");
      }
    }
  }

  public void exec() {
    if (aParams == null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      aResult = aMethod.invoke(aOwner, aParams);
    } catch (final IllegalArgumentException e) {
      throw new MessagingException(e);
    } catch (final IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MessagingException(cause != null ? cause : e);
    }
  }

  public static void marshalResult(final HttpServletResponse pResponse, final Source pSource) {
    pResponse.setContentType("application/soap+xml");
    try {
      Sources.writeToStream(pSource, pResponse.getOutputStream());
    } catch (final TransformerException e) {
      throw new MessagingException(e);
    } catch (final IOException e) {
      throw new MessagingException(e);
    }
  }

  public Source getResultSource() {
    if (aResult instanceof Source) {
      return (Source) aResult;
    }

    List<Tripple<String, ? extends Class<? extends Object>, ?>> params;
    List<Object> headers;
    if (aResult instanceof ActivityResponse) {
      final ActivityResponse<?> activityResponse = (ActivityResponse<?>) aResult;
      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", activityResponse.getReturnType(), activityResponse.getReturnValue()) );
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
        throw new MessagingException(e);
      } catch (final SecurityException e) {
        throw new MessagingException(e);
      } catch (final IllegalAccessException e) {
        throw new MessagingException(e);
      } catch (final InvocationTargetException e) {
        throw new MessagingException(e);
      } catch (final NoSuchMethodException e) {
        throw new MessagingException(e);
      }
      params = Arrays.asList( Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", returnType, returnValue) );
      headers = Collections.<Object> singletonList(aResult);

    } else {

      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", aMethod.getReturnType(), aResult));
      headers = Collections.emptyList();
    }
    try {
      return SoapHelper.createMessage(new QName(aMethod.getName() + "Response"), headers, params);
    } catch (final JAXBException e) {
      throw new MessagingException(e);
    }
  }

  public Object getResult() {
    return aResult;
  }

}
