package nl.adaptivity.ws.soap;

import net.devrieze.util.Annotations;
import net.devrieze.util.Tripple;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.engine.MessagingFormatException;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.util.activation.Sources;
import org.jetbrains.annotations.NotNull;
import org.w3.soapEnvelope.Body;
import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Node;

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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;


public class SoapMethodWrapper {

  public static final URI SOAP_ENCODING = URI.create(ProcessConsts.Soap.SOAP_ENCODING_NS);

  private final Object aOwner;

  private final Method aMethod;

  private Object[] aParams;

  private Object aResult;

  public SoapMethodWrapper(final Object owner, final Method method) {
    aOwner = owner;
    aMethod = method;
  }

  public void unmarshalParams(@NotNull final Source source, final Map<String, DataSource> attachments) {
    final Envelope envelope = JAXB.unmarshal(source, Envelope.class);
    unmarshalParams(envelope, attachments);

  }

  public void unmarshalParams(@NotNull final Envelope envelope, final Map<String, DataSource> attachments) {
    if (aParams != null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }

    ensureNoUnunderstoodHeaders(envelope);
    processSoapHeader(envelope.getHeader());
    final URI es = envelope.getEncodingStyle();
    if ((es == null) || es.equals(SOAP_ENCODING)) {
      processSoapBody(envelope, attachments);
    } else {
      throw new MessagingFormatException("Ununderstood message body");
    }
  }

  private void ensureNoUnunderstoodHeaders(@NotNull @SuppressWarnings("unused") final Envelope envelope) {
    if (envelope.getHeader().getAny().size()>0) {
      throw new MessagingFormatException("Soap header not understood");
    }
  }

  @SuppressWarnings("EmptyMethod")
  private void processSoapHeader(@SuppressWarnings("unused") final Header header) {
    // TODO Auto-generated method stub
    //
    /* For now just ignore headers, i.e. none understood */
  }

  private void processSoapBody(@NotNull final org.w3.soapEnvelope.Envelope envelope, @SuppressWarnings("unused") final Map<String, DataSource> attachments) {
    final Body body = envelope.getBody();
    if (body.getAny().size() != 1) {
      throw new MessagingFormatException("Multiple body elements not expected");
    }
    final Node root = (Node) body.getAny().get(0);
    assertRootNode(root);

    final LinkedHashMap<String, Node> params = SoapHelper.getParamMap(root);
    final Map<String, Node> headers = SoapHelper.getHeaderMap(envelope.getHeader());

    final Class<?>[] parameterTypes = aMethod.getParameterTypes();
    final Annotation[][] parameterAnnotations = aMethod.getParameterAnnotations();

    aParams = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; ++i) {
      final WebParam annotation = Annotations.getAnnotation(parameterAnnotations[i], WebParam.class);
      final String name;
      if (annotation == null) {
        if (params.isEmpty()) {
          throw new MessagingFormatException("Missing parameter "+(i+1)+" of type "+parameterTypes[i]+" for method "+aMethod);
        }
        name = params.keySet().iterator().next();
      } else {
        name = annotation.name();
      }
      final Node value;
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

  private void assertRootNode(@NotNull final Node root) {
    final WebMethod wm = aMethod.getAnnotation(WebMethod.class);
    if ((wm == null) || wm.operationName().equals("")) {
      if (!root.getLocalName().equals(aMethod.getName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    } else {
      if (!root.getLocalName().equals(wm.operationName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    }
    final WebService ws = aMethod.getDeclaringClass().getAnnotation(WebService.class);
    if (!((ws == null) || ws.targetNamespace().equals(""))) {
      if (!ws.targetNamespace().equals(root.getNamespaceURI())) {
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
    } catch (@NotNull final IllegalArgumentException e) {
      throw new MessagingException(e);
    } catch (@NotNull final IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (@NotNull final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MessagingException(cause != null ? cause : e);
    }
  }

  public static void marshalResult(@NotNull final HttpServletResponse response, final Source source) {
    response.setContentType("application/soap+xml");
    try {
      Sources.writeToStream(source, response.getOutputStream());
    } catch (@NotNull final TransformerException e) {
      throw new MessagingException(e);
    } catch (@NotNull final IOException e) {
      throw new MessagingException(e);
    }
  }

  @NotNull
  public Source getResultSource() {
    if (aResult instanceof Source) {
      return (Source) aResult;
    }

    final List<Tripple<String, ? extends Class<?>, ?>> params;
    final List<Object> headers;
    if (aResult == null && aMethod.getReturnType()==Void.class) {
      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                             Tripple.tripple("result", Void.class, null));
      headers = Collections.emptyList();

    } else if (aResult instanceof ActivityResponse) {
      final ActivityResponse<?> activityResponse = (ActivityResponse<?>) aResult;
      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", activityResponse.getReturnType(), activityResponse.getReturnValue()) );
      headers = Collections.<Object> singletonList(aResult);
    } else if (aResult!=null && "nl.adaptivity.process.messaging.ActivityResponse".equals(aResult.getClass().getName())) {
      /*
       * If the ActivityResponse was created by a different classloader like
       * when we directly invoke the endpoint through DarwinMessenger shortcut
       * we will have to resort to reflection instead of direct invocation. This
       * should still beat going through tcp-ip hand out.
       */

      final Class<?> returnType;
      final Object returnValue;
      try {
        returnType = (Class<?>) aResult.getClass().getMethod("getReturnType").invoke(aResult);
        returnValue = aResult.getClass().getMethod("getReturnValue").invoke(aResult);
      } catch (@NotNull final IllegalArgumentException e) {
        throw new MessagingException(e);
      } catch (@NotNull final SecurityException e) {
        throw new MessagingException(e);
      } catch (@NotNull final IllegalAccessException e) {
        throw new MessagingException(e);
      } catch (@NotNull final InvocationTargetException e) {
        throw new MessagingException(e);
      } catch (@NotNull final NoSuchMethodException e) {
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
    } catch (@NotNull final JAXBException e) {
      throw new MessagingException(e);
    }
  }

  public Object getResult() {
    return aResult;
  }

}
