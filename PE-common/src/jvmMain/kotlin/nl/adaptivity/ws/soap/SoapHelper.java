/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.soap;

import net.devrieze.util.StringUtil;
import net.devrieze.util.Tripple;
import net.devrieze.util.Types;
import net.devrieze.util.security.SYSTEMPRINCIPAL;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.io.Writable;
import nl.adaptivity.io.WritableReader;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.xmlutil.util.CompactFragment;
import nl.adaptivity.xmlutil.util.ICompactFragment;
import nl.adaptivity.util.DomUtil;
import nl.adaptivity.xmlutil.*;
import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;


/**
 * Static helper method that helps with handling soap requests and responses.
 *
 * TODO fix this big mess by refactoring it.
 *
 * @author Paul de Vrieze
 */
public class SoapHelper {
  // TODO should no longer be needed as DarwinMessenger will do the class loader shenanigans.
  public static class XmlDeserializationHelper {
    public static Class<? extends XmlDeserializerFactory<?>> deserializationTarget(Class<?> clazz, Annotation[] paramContext) {
      XmlDeserializer annotation = getFactoryAnnotation(clazz, paramContext);
      if (annotation!=null) { return annotation.value(); }
      else return null;
    }

    private static XmlDeserializer getFactoryAnnotation(final Class<?> clazz, final Annotation[] paramContext) {
      {
        XmlDeserializer result = clazz.getAnnotation(XmlDeserializer.class);
        if (result!=null) { return result; }
      }
      if (paramContext!=null) {
        for (final Annotation a : paramContext) {
          if (a instanceof SoapSeeAlso) {
            for (final Class<?> refClass : ((SoapSeeAlso) a).value()) {
              final XmlDeserializer result = refClass.getAnnotation(XmlDeserializer.class);
              if (result != null) { return result; }
            }
          }
        }
      }
      return null;
    }

    public static <T> T deserialize(Class<T> target, Class<? extends XmlDeserializerFactory<? extends T>> deserializer, Node value) throws XmlException {
      try {
        XmlDeserializerFactory<? extends T> factory = deserializer.newInstance();
        return target.cast(factory.deserialize(XmlStreaming.INSTANCE.newReader(new DOMSource(value))));
      } catch (InstantiationException | IllegalAccessException e) {
        throw new XmlException(e);
      }

    }
  }

  public static final String SOAP_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope";

  public static final QName SOAP_RPC_RESULT = new QName("http://www.w3.org/2003/05/soap-rpc", "result");

  public static final String RESULT = "!@#$Result_MARKER::";

  private SoapHelper() {}

  public static Source createMessage(final QName pOperationName, final List<Tripple<String, ? extends Class<?>, ?>> pParams) throws JAXBException, XmlException {
    return createMessage(pOperationName, null, pParams);
  }

  /**
   * Create a Source encapsulating a soap message for the given operation name
   * and parameters.
   *
   * @param operationName The name of the soap operation (name of the first
   *          child of the soap body)
   * @param headers A list of optional headers to add to the message.
   * @param params The parameters of the message
   * @return a Source that encapsulates the message.
   * @throws JAXBException
   */
  public static Source createMessage(final QName operationName, final List<?> headers, final List<Tripple<String, ? extends Class<?>, ?>> params) throws JAXBException, XmlException {
    DocumentBuilder db;
    {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      try {
        db = dbf.newDocumentBuilder();
      } catch (final ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    final Document resultDoc = db.newDocument();

    final Element envelope = createSoapEnvelope(resultDoc);
    if ((headers != null) && (headers.size() > 0)) {
      createSoapHeader(envelope, headers);
    }
    final Element body = createSoapBody(envelope);
    final Element message = createBodyMessage(body, operationName);
    for (final Tripple<String, ? extends Class<?>, ?> param : params) {
      addParam(message, param);
    }
    return new DOMSource(resultDoc);
  }

  /**
   * Create a SOAP envelope in the document and return the body element.
   *
   * @param pDoc The document that needs to contain the envelope.
   * @return The body element.
   */
  private static Element createSoapEnvelope(final Document pDoc) {
    final Element envelope = pDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope");
    envelope.setAttribute("encodingStyle", SoapMethodWrapper.SOAP_ENCODING.toString());
    pDoc.appendChild(envelope);
    return envelope;
  }

  private static Element createSoapHeader(final Element pEnvelope, final List<?> pHeaders) {
    final Document ownerDoc = pEnvelope.getOwnerDocument();
    final Element header = ownerDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Header");
    pEnvelope.appendChild(header);
    for (final Object headerElem : pHeaders) {
      if (headerElem instanceof Node) {
        final Node node = ownerDoc.importNode(((Node) headerElem), true);
        header.appendChild(node);
      } if (headerElem instanceof XmlSerializable) {
        try {
          XmlWriter out = XmlStreaming.INSTANCE.newWriter(new DOMResult(header));
          ((XmlSerializable) headerElem).serialize(out);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) { // XMLException
          throw new MessagingException(e);
        }
      } else {
        try {
          Marshaller marshaller;
          {
            final JAXBContext context;
            if (headerElem instanceof JAXBElement) {
              context = JAXBContext.newInstance();
            } else {
              context = JAXBContext.newInstance(headerElem.getClass());
            }
            marshaller = context.createMarshaller();
          }
          marshaller.marshal(headerElem, header);

        } catch (final JAXBException e) {
          throw new MessagingException(e);
        }
      }
    }

    return header;
  }

  private static Element createSoapBody(final Element pEnvelope) {
    final Element body = pEnvelope.getOwnerDocument().createElementNS(SOAP_ENVELOPE_NS, "soap:Body");
    pEnvelope.appendChild(body);
    return body;
  }

  /**
   * Create the actual body of the SOAP message.
   *
   * @param pBody The body element in which the body needs to be embedded.
   * @param pOperationName The name of the wrapping name (the operation name).
   * @return
   */
  private static Element createBodyMessage(final Element pBody, final QName pOperationName) {
    final Document pResultDoc = pBody.getOwnerDocument();

    final Element message = pResultDoc.createElementNS(pOperationName.getNamespaceURI(), XmlUtil.toCName(pOperationName));

    pBody.appendChild(message);
    return message;
  }

  private static Element addParam(final Element pMessage, final Tripple<String, ? extends Class<?>, ?> pParam) throws JAXBException, XmlException {
    final Document ownerDoc = pMessage.getOwnerDocument();
    String prefix;
    if ((pMessage.getPrefix() != null) && (pMessage.getPrefix().length() > 0)) {
      prefix = pMessage.getPrefix() + ':';
    } else {
      prefix = "";
    }
    if (pParam.getElem1() == RESULT) { // We need to create the wrapper that refers to the actual result.
      String rpcprefix = DomUtil.getPrefix(pMessage, SOAP_RPC_RESULT.getNamespaceURI());
      final Element wrapper;
      if (rpcprefix == null) {
        wrapper = ownerDoc.createElementNS(SOAP_RPC_RESULT.getNamespaceURI(), "rpc:" + SOAP_RPC_RESULT.getLocalPart());
        wrapper.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:rpc", SOAP_RPC_RESULT.getNamespaceURI());
      } else {
        wrapper = ownerDoc.createElementNS(SOAP_RPC_RESULT.getNamespaceURI(), rpcprefix+":" + SOAP_RPC_RESULT.getLocalPart());
      }
      pMessage.appendChild(wrapper);

      wrapper.appendChild(ownerDoc.createTextNode(prefix + pParam.getElem3().toString()));
      return wrapper;
    }

    final Element wrapper = ownerDoc.createElementNS(pMessage.getNamespaceURI(), prefix + pParam.getElem1());
    wrapper.setPrefix(pMessage.getPrefix());
    pMessage.appendChild(wrapper);

    final Class<?> paramType = pParam.getElem2();
    if (pParam.getElem3() == null) {
      // don't add anything
    } else if (Types.isPrimitive(paramType) || Types.isPrimitiveWrapper(paramType)) {
      wrapper.appendChild(ownerDoc.createTextNode(pParam.getElem3().toString()));
    } else if (XmlSerializable.class.isAssignableFrom(paramType)) {
      XmlWriter xmlWriter = XmlStreaming.INSTANCE.newWriter(new DOMResult(wrapper));
      ((XmlSerializable) pParam.getElem3()).serialize(xmlWriter);
      xmlWriter.close();
    } else if (Collection.class.isAssignableFrom(paramType)) {
      final Collection<?> params = (Collection<?>) pParam.getElem3();
      final Set<Class<?>> paramTypes = new HashSet<>();
      {
        for (final Object elem : params) {
          paramTypes.add(elem.getClass());
        }
      }
      Marshaller marshaller;
      {
        final JAXBContext context = JAXBContext.newInstance(paramTypes.toArray(new Class<?>[paramTypes.size()]));
        marshaller = context.createMarshaller();
      }
      for (final Object elem : params) {
        marshaller.marshal(elem, wrapper);
      }
    } else if (Node.class.isAssignableFrom(paramType)) {
      Node param = (Node) pParam.getElem3();
      param = ownerDoc.importNode(param, true);
      wrapper.appendChild(param);
    } else if (pParam.getElem3() == SYSTEMPRINCIPAL.INSTANCE) {
      Element tag = ownerDoc.createElementNS(SYSTEMPRINCIPAL.NS, SYSTEMPRINCIPAL.TAG);
      tag.appendChild(ownerDoc.createTextNode(Long.toString(SYSTEMPRINCIPAL.INSTANCE.getKEY())));
      wrapper.appendChild(tag);
    } else if (Principal.class.isAssignableFrom(paramType)) {
      wrapper.appendChild(ownerDoc.createTextNode(((Principal) pParam.getElem3()).getName()));
    } else {
      Marshaller marshaller;
      {
        final JAXBContext context = JAXBContext.newInstance(paramType);
        marshaller = context.createMarshaller();
      }
      marshaller.marshal(pParam.getElem3(), wrapper);
    }
    return wrapper;
  }

  public static <T> T processResponse(final Class<T> resultType, Class<?>[] context, final Annotation[] useSiteAnnotations, final Source source) throws XmlException {
    XmlReader                  in  = XmlStreaming.INSTANCE.newReader(source);
    Envelope<? extends ICompactFragment> env = Envelope.Companion.deserialize(in);
    return processResponse(resultType, context, useSiteAnnotations, env);
  }

  private static <T> T processResponse(final Class<T> resultType, final Class<?>[] context, final Annotation[] useSiteAnnotations, final Envelope<? extends ICompactFragment> env) {
    final ICompactFragment bodyContent = env.getBody().getBodyContent();
    try {
      XmlReader reader = bodyContent.getXmlReader();
      while (reader.hasNext()) {
        switch(reader.next()) {
          case TEXT:
            if (!XmlUtil.isXmlWhitespace(reader.getText())) {
              throw new XmlException("Unexpected text content");
            }
          case IGNORABLE_WHITESPACE:
            break; // whitespace is ignored
          case START_ELEMENT: {
            LinkedHashMap<String, Node> params = unmarshalWrapper(reader);
            // This is the parameter wrapper
            return unMarshalNode(null, resultType, context, useSiteAnnotations, params.get(RESULT));
          }
          default:
            XmlReaderUtil.unhandledEvent(reader, null);
        }
      }
      return null; // no nodes
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T processResponse(final Class<T> resultType, Class<?>[] context, final Annotation[] useSiteAnnotations, final Writable pContent) throws XmlException {
    final Envelope<? extends ICompactFragment> env = Envelope.Companion.deserialize(XmlStreaming.INSTANCE.newReader(new WritableReader(pContent)));
    return processResponse(resultType, context, useSiteAnnotations, env);
  }

  static <T> LinkedHashMap<String, Node> unmarshalWrapper(final XmlReader reader) throws XmlException {
    XmlReaderUtil.skipPreamble(reader);
    reader.require(EventType.START_ELEMENT, null, null);
    LinkedHashMap<String, Node> params = new LinkedHashMap<>();
    QName wrapperName = reader.getName();
    QName returnName = null;
    outer: while (reader.hasNext()) {
      switch (reader.next()) {
        case TEXT:
          if (!XmlUtil.isXmlWhitespace(reader.getText())) {
            throw new XmlException("Unexpected text content");
          }
        case IGNORABLE_WHITESPACE:
          break; // whitespace is ignored
        case START_ELEMENT: {
          if (XmlReaderUtil.isElement(reader, "http://www.w3.org/2003/05/soap-rpc", "result")) {
            String s = StringUtil.toString(XmlReaderUtil.readSimpleElement(reader));
            final int i = s.indexOf(':');
            if (i >= 0) {
              returnName = new QName(reader.getNamespaceURI(s.substring(0, i)), s.substring(i + 1));
            } else {
              String ns = reader.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
              if (ns==null || ns.isEmpty()) {
                returnName = new QName(s);
              } else {
                returnName = new QName(ns, s);
              }
            }
            if (params.containsKey(returnName)) {
              params.put(RESULT, params.remove(returnName));
            }

          } else if ((returnName != null) && XmlReaderUtil.isElement(reader, returnName)) {
            params.put(RESULT, DomUtil.childToNode(reader));
            reader.require(EventType.END_ELEMENT, returnName.getNamespaceURI(), returnName.getLocalPart());
          } else {
            params.put(reader.getLocalName().toString(), DomUtil.childToNode(reader));
            reader.require(EventType.END_ELEMENT, null, null);
          }
          break;
        }
        case END_ELEMENT:
          break outer;
        default:
          XmlReaderUtil.unhandledEvent(reader);
          // This is the parameter wrapper
      }
    }


    reader.require(EventType.END_ELEMENT, wrapperName.getNamespaceURI(), wrapperName.getLocalPart());
    return params;
  }


  private static LinkedHashMap<String, Node> getParamMap(final Node bodyParamRoot) {
    LinkedHashMap<String, Node> params = new LinkedHashMap<>();
    {

      Node child = bodyParamRoot.getFirstChild();
      String returnName = null;
      while (child != null) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
          if ("http://www.w3.org/2003/05/soap-rpc".equals(child.getNamespaceURI()) && "result".equals(child.getLocalName())) {
            returnName = child.getTextContent();
            final int i = returnName.indexOf(':');
            if (i >= 0) {
              returnName = returnName.substring(i + 1);
            }
            if (params.containsKey(returnName)) {
              final Node val = params.remove(returnName);
              params.put(RESULT, val);
            }

          } else if ((returnName != null) && child.getLocalName().equals(returnName)) {
            params.put(RESULT, child);
          } else {

            params.put(child.getLocalName(), child);
          }
        }
        child = child.getNextSibling();
      }
    }
    return params;
  }

  public static Map<String, Node> getHeaderMap(final Header pHeader) {
    if (pHeader == null) {
      return Collections.emptyMap();
    }
    final LinkedHashMap<String, Node> result = new LinkedHashMap<>();
    for (final CompactFragment frag : pHeader.getAny()) {

        Node node = DomUtil.childToNode(frag.getXmlReader());
        result.put(node.getLocalName(), node);
    }
    return result;
  }

  static <T> T unMarshalNode(final Method pMethod, final Class<T> pClass, final Class<?>[] jaxbContext, final Annotation[] useSiteAnnotations, final Node pAttrWrapper) {
    Node value = pAttrWrapper == null ? null : pAttrWrapper.getFirstChild();
    while (value !=null && value instanceof Text && XmlUtil.isXmlWhitespace(((Text) value).getData()))  { value = value.getNextSibling(); }
    Object result;
    if ((value != null) && (!pClass.isInstance(value))) {
      if (Types.isPrimitive(pClass) || (Types.isPrimitiveWrapper(pClass))) {
        result = Types.parsePrimitive(pClass, value.getTextContent());
      } else if (Enum.class.isAssignableFrom(pClass)) {
        final String val = value.getTextContent();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Object tmpResult = Enum.valueOf((Class) pClass, val);
        result = tmpResult;
      } else if (pClass.isAssignableFrom(Principal.class)
                 && (value instanceof Element)
                 && (SYSTEMPRINCIPAL.NS.equals(value.getNamespaceURI()))
                 && (SYSTEMPRINCIPAL.TAG.equals(value.getLocalName()))) {
        long key = Long.parseLong(value.getTextContent());
        if (key == SYSTEMPRINCIPAL.INSTANCE.getKEY()) {
          result = SYSTEMPRINCIPAL.INSTANCE;
        } else {
          throw new IllegalArgumentException("Invalid system principal key!! "+key);
        }
      } else if (pClass.isAssignableFrom(Principal.class) && (value instanceof Text)) {
        result = new SimplePrincipal(((Text) value).getData());
      } else if (CharSequence.class.isAssignableFrom(pClass) && (value instanceof Text)) {
        if (pClass.isAssignableFrom(String.class)) {
          result = ((Text) value).getData();
        } else if (pClass.isAssignableFrom(StringBuilder.class)) {
          final String val = ((Text) value).getData();
          result = new StringBuilder(val.length());
          ((StringBuilder) result).append(val);
        } else {
          throw new UnsupportedOperationException("Can not unmarshal other strings than to string or stringbuilder");
        }
      } else {
        Class<?> helper = null;
        Class<?> deserializabletargetType = null;
        try {
          helper = Class.forName(XmlDeserializationHelper.class.getName(), true, pClass.getClassLoader());
          Method deserializationTarget = helper.getMethod("deserializationTarget", Class.class, (new Annotation[0].getClass()));
          deserializabletargetType = (Class<?>) deserializationTarget.invoke(null, pClass, useSiteAnnotations);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        if (deserializabletargetType!=null) {
          try {
            result = helper.getMethod("deserialize", Class.class, Class.class, Node.class).invoke(null, pClass, deserializabletargetType, value);
          } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
            if (e.getCause() instanceof XmlException) {
                ((XmlException) e.getCause()).doThrow();
              throw new UnsupportedOperationException("Unreachable");
            } else if (e.getCause() instanceof RuntimeException) {
              throw (RuntimeException) e.getCause();
            } else {
              throw new RuntimeException(e);
            }
          }
        } else {

          if (value.getNextSibling() != null && (value.getNextSibling() instanceof Element)) {
            throw new UnsupportedOperationException("Collection parameters not yet supported: " + pMethod.toGenericString() + " found: '" + DomUtil.toString(value.getNextSibling()) + "' in " + DomUtil.toString(value.getParentNode()));
          }
          try {
            JAXBContext context;

            if (pClass.isInterface()) {
              context = newJAXBContext(pMethod, Arrays.asList(jaxbContext));
            } else {
              List<Class<?>> list = new ArrayList<>(1 + (jaxbContext == null ? 0 : jaxbContext.length));
              list.add(pClass);
              if (jaxbContext != null) { list.addAll(Arrays.asList(jaxbContext)); }
              context = newJAXBContext(pMethod, list);
            }
            final Unmarshaller um = context.createUnmarshaller();
            if (pClass.isInterface()) {
              if (value instanceof Text) {
                result = ((Text) value).getData();
              } else {
                result = um.unmarshal(value);

                if (result instanceof JAXBElement) {
                  result = ((JAXBElement<?>) result).getValue();
                }
              }
            } else {
              final JAXBElement<?> umresult;
              umresult = um.unmarshal(value, pClass);
              result = umresult.getValue();
            }

          } catch (final JAXBException e) {
            throw new MessagingException("Error unmarshalling node "+pAttrWrapper, e);
          }
        }
      }
    } else {
      result = value;
    }
    if (Types.isPrimitive(pClass)) {
      @SuppressWarnings("unchecked")
      final T r2 = (T) result;
      return r2;
    }

    return result == null ? null : pClass.cast(result);
  }

  private static JAXBContext newJAXBContext(final Method pMethod, final List<Class<?>> pClasses) throws JAXBException {
    Class<?>[] classList;
    XmlSeeAlso seeAlso;
    if (pMethod != null) {
      final Class<?> clazz = pMethod.getDeclaringClass();
      seeAlso = clazz.getAnnotation(XmlSeeAlso.class);
    } else {
      seeAlso = null;
    }
    if ((seeAlso != null) && (seeAlso.value().length > 0)) {
      final Class<?>[] seeAlsoClasses = seeAlso.value();
      final int seeAlsoLength = seeAlsoClasses.length;
      classList = new Class<?>[seeAlsoLength + pClasses.size()];
      System.arraycopy(seeAlsoClasses, 0, classList, 0, seeAlsoLength);
      for (int i = 0; i < pClasses.size(); ++i) {
        classList[seeAlsoLength + i] = pClasses.get(i);
      }
    } else {
      classList = pClasses.toArray(new Class[pClasses.size()]);
    }
    return JAXBContext.newInstance(classList);
  }

}
