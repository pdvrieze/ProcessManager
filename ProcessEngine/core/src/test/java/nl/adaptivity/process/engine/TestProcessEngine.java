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

import net.devrieze.util.*;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.MemTransactionedHandleMap;
import nl.adaptivity.process.StubTransaction;
import nl.adaptivity.process.StubTransactionFactory;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.xml.*;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
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

  private static DocumentBuilder _documentBuilder;

  ProcessEngine<Transaction> mProcessEngine;
  private StubMessageService mStubMessageService;
  private final EndpointDescriptor mLocalEndpoint = new EndpointDescriptorImpl(QName.valueOf("processEngine"),"processEngine", URI.create("http://localhost/"));
  private StubTransactionFactory mStubTransactionFactory;
  private SimplePrincipal mPrincipal;

  public TestProcessEngine() {
    mStubMessageService = new StubMessageService(mLocalEndpoint);
    mStubTransactionFactory = new StubTransactionFactory();
    mPrincipal = new SimplePrincipal("pdvrieze");
  }

  private InputStream getXml(String name) {
    try (InputStream in = getClass().getResourceAsStream("/nl/adaptivity/process/engine/test/"+name)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (!InputStreamOutputStream.getInputStreamOutputStream(in, out).get().booleanValue()) {
        return null;
      }
      byte[] byteArray = out.toByteArray();
      assertTrue(byteArray.length > 0, "Some bytes in the xml files are expected");;
      ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
      return bais;
    } catch (Exception e) {
      if (e instanceof  RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }

  private XmlReader getStream(String name) throws XmlException {
    return XmlStreaming.newReader(getXml(name), "UTF-8");
  }

  private ProcessModelImpl getProcessModel(String name) throws XmlException {
    return ProcessModelImpl.deserialize(getStream(name));
  }

  private Document getDocument(String name) {
    try (InputStream in = getClass().getResourceAsStream("/nl/adaptivity/process/engine/test/"+name)) {
      return getDocumentBuilder().parse(in);
    } catch (Exception e) {
      if (e instanceof  RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }

  private static <V> TransactionedHandleMap<V,Transaction> cache(TransactionedHandleMap<V,Transaction> base, int count) {
    return new CachingHandleMap<>(base, count);
  }

  private static <V> IProcessModelMap<Transaction> cache(IProcessModelMap<Transaction> base, int count) {
    return new CachingProcessModelMap<>(base, count);
  }

  @BeforeMethod
  public void beforeTest() {
    mStubMessageService.clear();
    mProcessEngine = ProcessEngine.newTestInstance(mStubMessageService, mStubTransactionFactory, cache(new MemProcessModelMap(), 1), cache(new MemTransactionedHandleMap<ProcessInstance<Transaction>>(), 1), cache(new MemTransactionedHandleMap<ProcessNodeInstance<Transaction>>(), 2), true);
  }

  private char[] serializeToXmlCharArray(final Object object) throws XmlException {
    char[] receivedChars;
    {
      CharArrayWriter caw = new CharArrayWriter();
      if (object instanceof XmlSerializable) {
        XmlWriter writer = XmlStreaming.newWriter(caw);
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
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
    ProcessModelImpl model = getProcessModel("testModel1.xml");
    StubTransaction transaction = mStubTransactionFactory.startTransaction();
    IProcessModelRef modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(mStubMessageService.getMMessages().size(), 1);
    assertEquals(mStubMessageService.getMessageNode(0).getHandleValue(), 1L);

    InputStream expected = getXml("testModel1_task1.xml");

    char[] receivedChars = serializeToXmlCharArray(mStubMessageService.getMMessages().get(0));

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

    ProcessInstance<Transaction> processInstance = mProcessEngine.getProcessInstance(transaction,instanceHandle ,mPrincipal);
    assertEquals(processInstance.getState(), STARTED);

    assertEquals(processInstance.getActive().size(), 1);
    assertEquals(processInstance.getFinished().size(), 1);
    Handle<? extends ProcessNodeInstance<Transaction>> hfinished = processInstance.getFinished().iterator().next();
    ProcessNodeInstance<Transaction> finished = mProcessEngine.getNodeInstance(transaction, hfinished, mPrincipal);
    assertTrue(finished.getNode() instanceof StartNodeImpl);;
    assertEquals(finished.getNode().getId(), "start");

    assertEquals(processInstance.getResults().size(), 0);

    ProcessNodeInstance taskNode = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);
    assertEquals(taskNode.getState(), Pending); // Our messenger does not do delivery notification

    assertEquals(mProcessEngine.finishTask(transaction, taskNode.getHandle(), null, mPrincipal), Complete);
    assertEquals(processInstance.getActive().size(), 0);
    assertEquals(processInstance.getFinished().size(), 2);
    assertEquals(processInstance.getResults().size(), 1);

    assertEquals(processInstance.getState(), FINISHED);
  }

  @Test
  public void testGetDataFromTask() throws Exception {
    ProcessModelImpl model = getProcessModel("testModel2.xml");
    StubTransaction transaction = mStubTransactionFactory.startTransaction();
    IProcessModelRef modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(mStubMessageService.getMMessages().size(), 1);

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(getXml("testModel2_task1.xml")), new CharArrayReader(serializeToXmlCharArray(mStubMessageService
                                                                                                                              .getMMessages()
                                                                                                                              .get(0))));
    ProcessNodeInstance<Transaction> ac1 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);// This should be 0 as it's the first activity


    mStubMessageService.clear(); // (Process the message)
    assertEquals(ac1.getResults().size(), 0);
    assertEquals(mProcessEngine.finishTask(transaction, ac1.getHandle(), getDocument("testModel2_response1.xml"), mPrincipal), Complete);
    assertEquals(ac1.getResults().size(), 2);
    ProcessData result1 = ac1.getResults().get(0);
    ProcessData result2 = ac1.getResults().get(1);
    assertEquals(result1.getName(), "name");
    assertEquals(result1.getContent().getContentString(), "Paul");
    assertEquals(result2.getName(), "user");
    assertXMLEqual("<user><fullname>Paul</fullname></user>", result2.getContent().getContentString());

    assertEquals(mStubMessageService.getMMessages().size(), 1);
    assertEquals(mStubMessageService.getMessageNode(0).getHandleValue(), 2L); //We should have a new message with the new task (with the data)
    ProcessNodeInstance ac2=mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal);

    List<ProcessData> ac2Defines = ac2.getDefines(transaction);
    assertEquals(ac2Defines.size(), 1);


    ProcessData define = ac2Defines.get(0);
    assertEquals(define.getName(), "mylabel");
    assertEquals(define.getContent().getContentString(), "Hi Paul. Welcome!");

  }

  private static void assertXMLSimilar(final Document expected, final Document actual) {
    Diff diff = XMLUnit.compareXML(expected, actual);
    DetailedDiff detailedDiff = new DetailedDiff(diff);
    if(! detailedDiff.similar()) {
      fail(detailedDiff.toString());
    }
  }

  private static Document toDocument(final Node node) throws TransformerException {
    Document result = getDocumentBuilder().newDocument();
    Sources.writeToResult(new DOMSource(node), new DOMResult(result));
    return result;
  }


}
