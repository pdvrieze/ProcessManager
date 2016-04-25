/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.soap;

import net.devrieze.util.Annotations;
import net.devrieze.util.StringUtil;
import net.devrieze.util.Tripple;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.engine.MessagingFormatException;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.ws.WsMethodWrapper;
import nl.adaptivity.xml.AbstractXmlReader;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;
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
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;


public class SoapMethodWrapper extends WsMethodWrapper {

  public static final URI SOAP_ENCODING = URI.create(ProcessConsts.Soap.SOAP_ENCODING_NS);

  public SoapMethodWrapper(final Object owner, final Method method) {
    super(owner, method);
  }

  public void unmarshalParams(@NotNull final Source source, final Map<String, DataSource> attachments) {
    final Envelope envelope = JAXB.unmarshal(source, Envelope.class);
    unmarshalParams(envelope, attachments);

  }

  public void unmarshalParams(@NotNull final Envelope envelope, final Map<String, DataSource> attachments) {
    if (mParams != null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }

    ensureNoUnunderstoodHeaders(envelope);
    processSoapHeader(envelope.getHeader());
    final URI es = envelope.getEncodingStyle();
    if ((es == null) || es.equals(SOAP_ENCODING)) {
      try {
        processSoapBody(envelope, attachments);
      } catch (XmlException e) {
        throw new MessagingFormatException("Failure to process message body", e);
      }
    } else {
      throw new MessagingFormatException("Ununderstood message body");
    }
  }

  private void ensureNoUnunderstoodHeaders(@NotNull @SuppressWarnings("unused") final Envelope envelope) {
    if (envelope.getHeader()!=null && envelope.getHeader().getAny().size()>0) {
      throw new MessagingFormatException("Soap header not understood");
    }
  }

  @SuppressWarnings("EmptyMethod")
  private void processSoapHeader(@SuppressWarnings("unused") final Header header) {
    // TODO Auto-generated method stub
    //
    /* For now just ignore headers, i.e. none understood. Principal is recorded but not handled */
  }

  private void processSoapBody(@NotNull final org.w3.soapEnvelope.Envelope<CompactFragment> envelope, @SuppressWarnings("unused") final Map<String, DataSource> attachments) throws XmlException {
    final Body<? extends CompactFragment> body   = envelope.getBody();
    XmlReader                             reader = XMLFragmentStreamReader.from(body.getBodyContent());
    reader.nextTag();
    assertRootNode(reader);


    final LinkedHashMap<String, Node> params = SoapHelper.unmarshalWrapper(reader);
    reader.require(EventType.END_ELEMENT, null, null);
    if (reader.hasNext()) { reader.next(); }
    while (reader.hasNext() && AbstractXmlReader.isIgnorable(reader)) { reader.next(); }
    if (reader.getEventType()== EventType.START_ELEMENT) {
      throw new MessagingFormatException("Multiple body elements not expected");
    }

    final Map<String, Node> headers = SoapHelper.getHeaderMap(envelope.getHeader());
    // TODO verify no unsupported headers that must be supported

    final Class<?>[] parameterTypes = mMethod.getParameterTypes();
    final Annotation[][] parameterAnnotations = mMethod.getParameterAnnotations();

    mParams = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; ++i) {
      final WebParam annotation = Annotations.getAnnotation(parameterAnnotations[i], WebParam.class);
      final String name;
      if (annotation == null) {
        if (params.isEmpty()) {
          throw new MessagingFormatException("Missing parameter "+(i+1)+" of type "+parameterTypes[i]+" for method "+mMethod);
        }
        name = params.keySet().iterator().next();
      } else {
        name = annotation.name();
      }
      final Node value;
      if ((annotation != null) && annotation.header()) {
        if (parameterTypes[i].isAssignableFrom(Principal.class) && envelope.getHeader().getPrincipal()!=null) {
          mParams[i] = envelope.getHeader().getPrincipal();
          continue; //Finish the parameter, we don't need to unmarshal
        } else if (parameterTypes[i].isAssignableFrom(String.class) && envelope.getHeader().getPrincipal()!=null) {
          mParams[i] = envelope.getHeader().getPrincipal().getName();
          continue;
        } else {
          value = headers.remove(name);
        }
      } else {
        value = params.remove(name);
      }

      if (value == null) {
        throw new MessagingFormatException("Parameter \"" + name + "\" not found");
      }

      final SoapSeeAlso seeAlso = Annotations.getAnnotation(parameterAnnotations[i], SoapSeeAlso.class);
      mParams[i] = SoapHelper.unMarshalNode(mMethod, parameterTypes[i], seeAlso==null ? new Class<?>[0] : seeAlso.value(), value);

    }
    if (params.size() > 0) {
      Logger.getLogger(getClass().getCanonicalName()).warning("Extra parameters in message: " + params.keySet().toString());
    }
  }

  private void assertRootNode(@NotNull final XmlReader reader) throws XmlException {
    final WebMethod wm = mMethod.getAnnotation(WebMethod.class);
    if ((wm == null) || wm.operationName().equals("")) {
      if (!StringUtil.isEqual(reader.getLocalName(),mMethod.getName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    } else {
      if (!StringUtil.isEqual(reader.getLocalName(), wm.operationName())) {
        throw new MessagingFormatException("Root node does not correspond to operation name");
      }
    }
    final WebService ws = mMethod.getDeclaringClass().getAnnotation(WebService.class);
    if (!((ws == null) || ws.targetNamespace().equals(""))) {
      if (!StringUtil.isEqual(ws.targetNamespace(), reader.getNamespaceUri())) {
        throw new MessagingFormatException("Root node does not correspond to operation namespace");
      }
    }
  }

  public static void marshalResult(@NotNull final HttpServletResponse response, final Source source) {
    response.setContentType("application/soap+xml");
    try {
      Sources.writeToStream(source, response.getOutputStream());
    } catch (@NotNull final TransformerException | IOException e) {
      throw new MessagingException(e);
    }
  }

  @NotNull
  public Source getResultSource() {
    if (mResult instanceof Source) {
      return (Source) mResult;
    }

    final List<Tripple<String, ? extends Class<?>, ?>> params;
    final List<Object> headers;
    if (mResult == null && mMethod.getReturnType()==Void.class) {
      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                             Tripple.tripple("result", Void.class, null));
      headers = Collections.emptyList();

    } else if (mResult instanceof ActivityResponse) {
      final ActivityResponse<?> activityResponse = (ActivityResponse<?>) mResult;
      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", activityResponse.getReturnType(), activityResponse.getReturnValue()) );
      headers = Collections.<Object> singletonList(mResult);
    } else if (mResult!=null && "nl.adaptivity.process.messaging.ActivityResponse".equals(mResult.getClass().getName())) {
      /*
       * If the ActivityResponse was created by a different classloader like
       * when we directly invoke the endpoint through DarwinMessenger shortcut
       * we will have to resort to reflection instead of direct invocation. This
       * should still beat going through tcp-ip hand out.
       */

      final Class<?> returnType;
      final Object returnValue;
      try {
        returnType = (Class<?>) mResult.getClass().getMethod("getReturnType").invoke(mResult);
        returnValue = mResult.getClass().getMethod("getReturnValue").invoke(mResult);
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
      headers = Collections.<Object> singletonList(mResult);

    } else {

      params = Arrays.asList(Tripple.tripple(SoapHelper.RESULT, String.class, "result"),
                              Tripple.tripple("result", mMethod.getReturnType(), mResult));
      headers = Collections.emptyList();
    }
    try {
      return SoapHelper.createMessage(new QName(mMethod.getName() + "Response"), headers, params);
    } catch (@NotNull final XmlException | JAXBException e) {
      throw new MessagingException(e);
    }
  }

  public Object getResult() {
    return mResult;
  }

}
