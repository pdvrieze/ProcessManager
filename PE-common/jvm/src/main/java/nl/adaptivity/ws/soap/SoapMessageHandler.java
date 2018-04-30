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

import net.devrieze.util.PrefixMap;
import net.devrieze.util.PrefixMap.Entry;
import net.devrieze.util.ValueCollection;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.xml.ICompactFragment;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.w3.soapEnvelope.Envelope;

import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class SoapMessageHandler {

  public static class ClassLoadedSoapMessageHandler extends SoapMessageHandler {

    private final Object mTarget;

    private final Map<Class<?>, PrefixMap<Method>> cache = new Hashtable<>();

    public ClassLoadedSoapMessageHandler(final Object target) {
      mTarget = target;
    }


    @Override
    public boolean processRequest(final HttpMessage pRequest, final HttpServletResponse pResponse) throws IOException, XmlException {
      final ICompactFragment source = pRequest.getBody();

      Source result;
      try {
        result = processMessage(source, pRequest.getAttachments());
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


    private Source processMessage(final ICompactFragment source, final Map<String, DataSource> pAttachments) throws XmlException {
      XmlReader reader = source.getXmlReader();
      return processMessage(reader, pAttachments);
    }

    @Override
    @NotNull
    public Source processMessage(final Reader reader, final Map<String, DataSource> pAttachments) throws XmlException {
      return processMessage(XmlStreaming.newReader(reader), pAttachments);
    }

    @NotNull
    private Source processMessage(final XmlReader source, final Map<String, DataSource> pAttachments) {
      final Envelope<? extends ICompactFragment> envelope = Envelope.Companion.deserialize(source);
      XmlReader                        reader   = envelope.getBody().getBodyContent().getXmlReader();
      loop: while(reader.hasNext()) {
        switch (reader.next()) {
          case START_ELEMENT:
            break loop;
          default:
            XmlReaderUtil.unhandledEvent(reader);
        }
      }
      if (reader.getEventType() != EventType.START_ELEMENT) {
        throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "Operation element not found");
      }

      final QName operation = reader.getName();

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
      final Method[] methods = pClass.getMethods();

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

  }

  private static class SoapMessageHandlerAccessor extends SoapMessageHandler {

    private final Object mOther;
    private final Method mProcessRequest;
    private final Method mProcessMessage;

    public SoapMessageHandlerAccessor(final Object target) {
      try {
        Class<?> otherClass = Class.forName(ClassLoadedSoapMessageHandler.class.getName(), true, target.getClass().getClassLoader());

        Constructor<?> constructor = otherClass.getConstructor(Object.class);
        mOther = constructor.newInstance(target);

        Method processRequest = null;
        Method processMessage = null;
        for (Method m: otherClass.getDeclaredMethods()) {
          if (!Modifier.isPublic(m.getModifiers())) { continue; }
          int methodIdx=0;
          if (m.getName().equals("processRequest")) {
            methodIdx = 1;
          } else if (m.getName().equals("processMessage")) {
            methodIdx = 2;
          } else { continue; }
          m.setAccessible(true); // force accessible
          if (methodIdx==1) {
            processRequest = m;
          } else {
            processMessage = m;
          }
        }
        if (processRequest==null || processMessage==null) { throw new IllegalStateException("Required methods not found"); }

        mProcessRequest = processRequest;
        mProcessMessage = processMessage;
      } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean processRequest(final HttpMessage pRequest, final HttpServletResponse pResponse) throws IOException {
      try {
        try {
          return (Boolean) mProcessRequest.invoke(mOther, pRequest, pResponse);
        } catch (InvocationTargetException i) {
          throw i.getCause();
        }
      } catch (RuntimeException|IOException e) {
        throw e;
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }

    @NotNull
    @Override
    public Source processMessage(final Reader source, final Map<String, DataSource> pAttachments) throws XmlException {
      try {
        try {
          return (Source) mProcessMessage.invoke(mOther, source, pAttachments);
        } catch (InvocationTargetException i) {
          throw i.getCause();
        }
      } catch (RuntimeException|XmlException e) {
        throw e;
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  private static volatile Map<Object, SoapMessageHandlerAccessor> mInstances;

  public abstract boolean processRequest(HttpMessage pRequest, HttpServletResponse pResponse) throws IOException, XmlException;

  @Deprecated
  public Source processMessage(final DataSource pDataSource, final Map<String, DataSource> pAttachments) throws IOException, XmlException {
    return processMessage(new InputStreamReader(pDataSource.getInputStream(), "UTF8"), pAttachments);
  }

  @NotNull
  public abstract Source processMessage(Reader source, Map<String, DataSource> pAttachments) throws XmlException;


  public static SoapMessageHandler newInstance(final Object pTarget) {
    if (mInstances == null) {
      mInstances = new ConcurrentHashMap<>();
    }
    if (!mInstances.containsKey(pTarget)) {
      synchronized (mInstances) {
        SoapMessageHandlerAccessor instance = mInstances.get(pTarget);
        if (instance == null) {
          instance = new SoapMessageHandlerAccessor(pTarget);
          mInstances.put(pTarget, instance);
        }
        return instance;
      }
    } else {
      return mInstances.get(pTarget);
    }
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
