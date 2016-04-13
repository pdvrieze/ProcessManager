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

package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by pdvrieze on 11/04/16.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class SimpleAdapter {

  private volatile static MethodHandle _getContext;
  private volatile static boolean _failedReflection = false;
  private static MethodHandle _getAllDeclaredPrefixes;
  private static MethodHandle _getNamespaceURI;

  QName name;
  private nl.adaptivity.xml.SimpleNamespaceContext namespaceContext;

  @NotNull
  public Map<QName, Object> getAttributes() {
    return attributes;
  }

  @XmlAnyAttribute final Map<QName, Object> attributes = new HashMap<>();

  @XmlAnyElement(lax = true, value = W3CDomHandler.class) final List<Object> children = new ArrayList<>();

  public void setAttributes(@NotNull final NamedNodeMap attributes) {
    for (int i = attributes.getLength() - 1; i >= 0; --i) {
      final Attr   attr   = (Attr) attributes.item(i);
      final String prefix = attr.getPrefix();
      if (prefix == null) {
        this.attributes.put(new QName(attr.getLocalName()), attr.getValue());
      } else if (!XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
        this.attributes.put(new QName(attr.getNamespaceURI(), attr.getLocalName(), prefix), attr.getValue());
      }
    }
  }

  public void beforeUnmarshal(@NotNull final Unmarshaller unmarshaller, final Object parent) {
    if (parent instanceof JAXBElement) {
      name = ((JAXBElement) parent).getName();
    }

    if (_failedReflection) {
      return;
    }
    final Object context;
    try {
      if (_getContext == null) {
        synchronized (getClass()) {
          final Lookup lookup = MethodHandles.lookup();
          _getContext = lookup.unreflect(unmarshaller.getClass().getMethod("getContext"));
          context = _getContext.invoke(unmarshaller);
          _getAllDeclaredPrefixes = lookup.unreflect(context.getClass().getMethod("getAllDeclaredPrefixes"));
          _getNamespaceURI = lookup.unreflect(context.getClass().getMethod("getNamespaceURI", String.class));

        }
      } else {
        context = _getContext.invoke(unmarshaller);
      }

      if (context != null) {
        final String[] prefixes = (String[]) _getAllDeclaredPrefixes.invoke(context);
        if (prefixes != null && prefixes.length > 0) {
          final String[] namespaces = new String[prefixes.length];
          for (int i = prefixes.length - 1; i >= 0; --i) {
            namespaces[i] = (String) _getNamespaceURI.invoke(context, prefixes[i]);
          }
          namespaceContext = new nl.adaptivity.xml.SimpleNamespaceContext(prefixes, namespaces);
        }
      }

    } catch (@NotNull final Throwable e) {
      Logger.getAnonymousLogger().log(Level.FINE, "Could not retrieve namespace context from marshaller", e);
      _failedReflection = true;
    }
  }

  nl.adaptivity.xml.SimpleNamespaceContext getNamespaceContext() {
    return namespaceContext;
  }
}
