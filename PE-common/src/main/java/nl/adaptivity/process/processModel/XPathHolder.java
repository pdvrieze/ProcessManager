package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.SimpleNamespaceContext;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


// TODO make this actually work by not using JAXB to parse (but statically generated xml parsing instead)
@XmlAccessorType(XmlAccessType.NONE)
public class XPathHolder {

  private static final XPathExpression SELF_PATH;
  private XPathExpression path;
  private String pathString;
  private NamespaceContext aNamespaceContext;
  private volatile static MethodHandle _getContext;
  private volatile static boolean _failedReflection = false;
  private static MethodHandle _getAllDeclaredPrefixes;
  private static MethodHandle _getNamespaceURI;

  static {
    try {
      SELF_PATH = XPathFactory.newInstance().newXPath().compile(".");
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public XPathHolder() {
    super();
  }

  public static void addXpathUsedPrefixes(final Map<String, String> pNamespaceMap, final String pPath, final NamespaceContext pNamespaceContext) throws
          XPathExpressionException {
    XPathFactory xpf = XPathFactory.newInstance();
    XPath xpath = xpf.newXPath();
    xpath.setNamespaceContext(new NamespaceContext() {
      @Override
      public String getNamespaceURI(final String prefix) {
        String namespaceURI = pNamespaceContext.getNamespaceURI(prefix);
        if (namespaceURI!=null) {
          pNamespaceMap.put(prefix, namespaceURI);
        }
        return namespaceURI;
      }

      @Override
      public String getPrefix(final String namespaceURI) {
        String prefix = pNamespaceContext.getNamespaceURI(namespaceURI);
        if (prefix!=null) {
          pNamespaceMap.put(prefix, namespaceURI);
        }
        return prefix;
      }

      @Override
      public Iterator<String> getPrefixes(final String namespaceURI) {
        for(Iterator<String> it = pNamespaceContext.getPrefixes(namespaceURI); it.hasNext();) {
          pNamespaceMap.put(it.next(), namespaceURI);
        }
        return pNamespaceContext.getPrefixes(namespaceURI);
      }
    });
    xpath.compile(pPath);
  }

  @XmlAttribute(name="xpath")
  public String getPath() {
    return pathString;
  }

  public void setPath(final String value) {
    if (pathString!=null && pathString.equals(value)) { return; }
    path = null;
    pathString = value;
    if (value==null) {
      path = null;
    } else {
      XPathFactory f = XPathFactory.newInstance();
      try {
        path = f.newXPath().compile(value);
      } catch (XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public NamespaceContext getNamespaceContext() {
    return aNamespaceContext;
  }

  public void setNamespaceContext(NamespaceContext pNamespaceContext) {
    aNamespaceContext = pNamespaceContext;
  }

  // Compatibility attribute for reading old models
  @XmlAttribute(name="path")
  String getPathAttr() { return null; }

  void setPathAttr(String path) {
    setPath(path);
  }

  public XPathExpression getXPath() {
    // TODO support a functionresolver
    if (path==null) {
      if (pathString==null) {
        path = SELF_PATH;
      } else {
        XPathFactory f = XPathFactory.newInstance();
        try {
          XPath xPath = f.newXPath();
          if (aNamespaceContext!=null) {
            xPath.setNamespaceContext(aNamespaceContext);
          }
          return xPath.compile(pathString);
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return path;
  }

  void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (_failedReflection) { return; }
    Object context;
    try {
      if (_getContext==null) {
        synchronized (getClass()) {
          Lookup lookup = MethodHandles.lookup();
          _getContext = lookup.unreflect(unmarshaller.getClass().getMethod("getContext"));
          context = _getContext.invoke(unmarshaller);
          _getAllDeclaredPrefixes = lookup.unreflect(context.getClass().getMethod("getAllDeclaredPrefixes"));
          _getNamespaceURI = lookup.unreflect(context.getClass().getMethod("getNamespaceURI", String.class));

        }
      } else {
        context = _getContext.invoke(unmarshaller);
      }

      if (context!=null) {
        String[] prefixes = (String[]) _getAllDeclaredPrefixes.invoke(context);
        if (prefixes != null && prefixes.length > 0) {
          String[] namespaces = new String[prefixes.length];
          for (int i = prefixes.length - 1; i >= 0; --i) {
            namespaces[i] = (String) _getNamespaceURI.invoke(context, prefixes[i]);
          }
          aNamespaceContext = new SimpleNamespaceContext(prefixes, namespaces);
        }
      }

    } catch (Throwable e) {
      Logger.getAnonymousLogger().log(Level.FINE, "Could not retrieve namespace context from marshaller", e);
      _failedReflection=true;
    }

  }
}