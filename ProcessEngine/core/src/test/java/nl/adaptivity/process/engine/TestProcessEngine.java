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

package nl.adaptivity.process.engine;

import kotlin.jvm.functions.Function2;
import net.devrieze.util.*;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.MemTransactionedHandleMap;
import nl.adaptivity.process.StubTransaction;
import nl.adaptivity.process.engine.ProcessInstance.State;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance.Builder;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.xml.*;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.Charset.defaultCharset;
import static nl.adaptivity.process.engine.ProcessInstance.State.FINISHED;
import static nl.adaptivity.process.engine.ProcessInstance.State.STARTED;
import static nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.Complete;
import static nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.Pending;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.testng.Assert.*;


/**
 * Created by pdvrieze on 18/08/15.
 */
public class TestProcessEngine {

  private static final Function2<SecureObject<ProcessNodeInstance<StubProcessTransaction>>, Long, SecureObject<ProcessNodeInstance<StubProcessTransaction>>> PNI_SET_HANDLE = new Function2<SecureObject<ProcessNodeInstance<StubProcessTransaction>>, Long, SecureObject<ProcessNodeInstance<StubProcessTransaction>>>() {
    @Override
    public SecureObject<ProcessNodeInstance<StubProcessTransaction>> invoke(final SecureObject<ProcessNodeInstance<StubProcessTransaction>> pni, final Long handle) {
      if (pni.withPermission().getHandleValue() == handle) { return pni; }
      final Builder<StubProcessTransaction, ? extends ExecutableProcessNode> builder = pni.withPermission().builder();
      builder.setHandle(Handles.<SecureObject<? extends ProcessNodeInstance<StubProcessTransaction>>>handle(handle));
      return builder.build();
    }
  };
  private static DocumentBuilder _documentBuilder;

  ProcessEngine<StubProcessTransaction> mProcessEngine;
  private StubMessageService mStubMessageService;
  private final EndpointDescriptor mLocalEndpoint = new EndpointDescriptorImpl(QName.valueOf("processEngine"),"processEngine", URI.create("http://localhost/"));
  private ProcessTransactionFactory<StubProcessTransaction> mStubTransactionFactory;
  private SimplePrincipal mPrincipal;

  public TestProcessEngine() {
    mStubMessageService = new StubMessageService(mLocalEndpoint);
    mStubTransactionFactory = new ProcessTransactionFactory<StubProcessTransaction>() {
      @NotNull
      @Override
      public StubProcessTransaction startTransaction(@NotNull final IProcessEngineData<StubProcessTransaction> engineData) {
        return new StubProcessTransaction(engineData);
      }
    };
    mPrincipal = new SimplePrincipal("pdvrieze");
  }

  private InputStream getXml(final String name) {
    try (InputStream in = getClass().getResourceAsStream("/nl/adaptivity/process/engine/test/"+name)) {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (!InputStreamOutputStream.getInputStreamOutputStream(in, out).get().booleanValue()) {
        return null;
      }
      final byte[] byteArray = out.toByteArray();
      assertTrue(byteArray.length > 0, "Some bytes in the xml files are expected");;
      final ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
      return bais;
    } catch (Exception e) {
      if (e instanceof  RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }

  private XmlReader getStream(final String name) throws XmlException {
    return XmlStreaming.newReader(getXml(name), "UTF-8");
  }

  private ProcessModelImpl getProcessModel(final String name) throws XmlException {
    return ProcessModelImpl.deserialize(getStream(name));
  }

  private Document getDocument(final String name) {
    try (InputStream in = getClass().getResourceAsStream("/nl/adaptivity/process/engine/test/"+name)) {
      return getDocumentBuilder().parse(in);
    } catch (Exception e) {
      if (e instanceof  RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }

  private static <V> MutableTransactionedHandleMap<V,StubProcessTransaction> cacheInstances(final MutableTransactionedHandleMap<V,StubProcessTransaction> base, final int count) {
    return new CachingHandleMap<>(base, count);
  }

  private static <V> MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<StubProcessTransaction>>,StubProcessTransaction> cacheNodes(final MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<StubProcessTransaction>>,StubProcessTransaction> base, final int count) {
    return new CachingHandleMap<>(base, count, PNI_SET_HANDLE);
  }

  private static <V> IMutableProcessModelMap<StubProcessTransaction> cacheModels(final IMutableProcessModelMap<StubProcessTransaction> base, final int count) {
    return new CachingProcessModelMap<>(base, count);
  }

  @BeforeMethod
  public void beforeTest() {
    mStubMessageService.clear();
//    DelegateProcessEngineData<StubProcessTransaction> engineData =
//            new DelegateProcessEngineData<>(mStubTransactionFactory,
//                                            cache(new MemProcessModelMap(), 1),
//                                            cache(new MemTransactionedHandleMap<>(), 1),
//                                            cache(new MemTransactionedHandleMap<>(), 2));

    mProcessEngine = ProcessEngine.Companion.newTestInstance(
            mStubMessageService,
            mStubTransactionFactory,
            cacheModels(new MemProcessModelMap(), 1),
            cacheInstances(new MemTransactionedHandleMap<SecureObject<ProcessInstance<StubProcessTransaction>>, StubProcessTransaction>(), 1),
            cacheNodes(new MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<StubProcessTransaction>>, StubProcessTransaction>(PNI_SET_HANDLE), 2), true);
  }

  private char[] serializeToXmlCharArray(final Object object) throws XmlException {
    final char[] receivedChars;
    {
      final CharArrayWriter caw = new CharArrayWriter();
      if (object instanceof XmlSerializable) {
        final XmlWriter writer = XmlStreaming.newWriter(caw);
        ((XmlSerializable) object).serialize(writer);
        writer.close();
      } else {
        JAXB.marshal(object, caw);
      }
      receivedChars = caw.toCharArray();
    }
    return receivedChars;
  }

  private static DocumentBuilder getDocumentBuilder() {
    if (_documentBuilder==null) {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(false);
        dbf.setCoalescing(false);
        _documentBuilder = dbf.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    return _documentBuilder;
  }

  @Test
  public void testExecuteSingleActivity() throws Exception {
    final ProcessModelImpl       model       = getProcessModel("testModel1.xml");
    final StubProcessTransaction transaction = mProcessEngine.startTransaction();
    final IProcessModelRef       modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    final HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(mStubMessageService.getMMessages().size(), 1);
    assertEquals(mStubMessageService.getMessageNode(0).getHandleValue(), 1L);

    final InputStream expected = getXml("testModel1_task1.xml");

    final char[] receivedChars = serializeToXmlCharArray(mStubMessageService.getMMessages().get(0));

    XMLUnit.setIgnoreWhitespace(true);
    try {
      assertXMLEqual(new InputStreamReader(expected), new CharArrayReader(receivedChars));
    } catch (AssertionError e) {
      e.printStackTrace();
      try {
        assertEquals(new String(receivedChars), Streams.toString(getXml("testModel1_task1.xml"), defaultCharset()));
      } catch (Exception f) {
        f.initCause(e);
        throw f;
      }

    }

    ProcessInstance<StubProcessTransaction> processInstance = mProcessEngine.getProcessInstance(transaction, instanceHandle, mPrincipal);
    assertEquals(processInstance.getState(), State.STARTED);

    assertEquals(processInstance.getActive().size(), 1);
    assertEquals(processInstance.getFinished().size(), 1);
    final Handle<? extends SecureObject<ProcessNodeInstance<StubProcessTransaction>>> hfinished = processInstance.getFinished().iterator().next();
    final ProcessNodeInstance<StubProcessTransaction>                                 finished  = mProcessEngine.getNodeInstance(transaction, hfinished, mPrincipal);
    assertTrue(finished.getNode() instanceof StartNodeImpl);;
    assertEquals(finished.getNode().getId(), "start");

    assertEquals(processInstance.getResults().size(), 0);

    final ProcessNodeInstance taskNode = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);
    assertEquals(taskNode.getState(), NodeInstanceState.Pending); // Our messenger does not do delivery notification

    assertEquals(mProcessEngine.finishTask(transaction, taskNode.getHandle(), null, mPrincipal).getState(), NodeInstanceState.Complete);
    processInstance = mProcessEngine.getProcessInstance(transaction, instanceHandle, mPrincipal);
    assertEquals(processInstance.getActive().size(), 0);
    assertEquals(processInstance.getFinished().size(), 2);
    assertEquals(processInstance.getResults().size(), 1);

    assertEquals(processInstance.getState(), State.FINISHED);
  }

  @Test
  public void testGetDataFromTask() throws Exception {
    final ProcessModelImpl       model       = getProcessModel("testModel2.xml");
    final StubProcessTransaction transaction = mProcessEngine.startTransaction();
    final IProcessModelRef       modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    final HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(mStubMessageService.getMMessages().size(), 1);

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(getXml("testModel2_task1.xml")), new CharArrayReader(serializeToXmlCharArray(mStubMessageService
                                                                                                                              .getMMessages()
                                                                                                                              .get(0))));
    ProcessNodeInstance<StubProcessTransaction> ac1 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);// This should be 0 as it's the first activity


    mStubMessageService.clear(); // (Process the message)
    assertEquals(ac1.getResults().size(), 0);
    ac1 = mProcessEngine.finishTask(transaction, ac1.getHandle(), getDocument("testModel2_response1.xml"), mPrincipal);
    assertEquals(ac1.getState(), NodeInstanceState.Complete);
    ac1 = mProcessEngine.getNodeInstance(transaction, ac1.getHandle(), mPrincipal);
    assertEquals(ac1.getResults().size(), 2);
    final ProcessData result1 = ac1.getResults().get(0);
    final ProcessData result2 = ac1.getResults().get(1);
    assertEquals(result1.getName(), "name");
    assertEquals(result1.getContent().getContentString(), "Paul");
    assertEquals(result2.getName(), "user");
    assertXMLEqual("<user><fullname>Paul</fullname></user>", result2.getContent().getContentString());

    assertEquals(mStubMessageService.getMMessages().size(), 1);
    assertEquals(mStubMessageService.getMessageNode(0).getHandleValue(), 2L); //We should have a new message with the new task (with the data)
    final ProcessNodeInstance ac2 =mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);

    final List<ProcessData> ac2Defines = ac2.getDefines(transaction);
    assertEquals(ac2Defines.size(), 1);


    final ProcessData define = ac2Defines.get(0);
    assertEquals(define.getName(), "mylabel");
    assertEquals(define.getContent().getContentString(), "Hi Paul. Welcome!");

  }

  private static void assertXMLSimilar(final Document expected, final Document actual) {
    final Diff         diff         = XMLUnit.compareXML(expected, actual);
    final DetailedDiff detailedDiff = new DetailedDiff(diff);
    if(! detailedDiff.similar()) {
      fail(detailedDiff.toString());
    }
  }

  private static Document toDocument(final Node node) throws TransformerException {
    final Document result = getDocumentBuilder().newDocument();
    Sources.writeToResult(new DOMSource(node), new DOMResult(result));
    return result;
  }


}
