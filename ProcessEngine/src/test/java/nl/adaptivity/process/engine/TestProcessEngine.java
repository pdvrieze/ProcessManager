package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.InputStreamOutputStream;
import net.devrieze.util.MemHandleMap;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessInstance.State;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlWriter;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;


/**
 * Created by pdvrieze on 18/08/15.
 */
public class TestProcessEngine {

  private class StubMessageService implements IMessageService<IXmlMessage,ProcessNodeInstance> {


    List<IXmlMessage> mMessages=new ArrayList<>();
    private List<Handle<? extends ProcessNodeInstance>> mMessageNodes = new ArrayList<>();

    @Override
    public IXmlMessage createMessage(final IXmlMessage message) {
      return message;
    }

    public void clear() {
      mMessageNodes.clear();
      mMessages.clear();
    }

    @Override
    public EndpointDescriptor getLocalEndpoint() {
      return mLocalEndpoint;
    }

    @Override
    public boolean sendMessage(final Transaction transaction, final IXmlMessage protoMessage, final ProcessNodeInstance instance) throws
            SQLException {
      CompactFragment instantiatedContent = null;
      XmlMessage processedMessage = null;
      try {
        instantiatedContent = instance.instantiateXmlPlaceholders(transaction, XMLFragmentStreamReader.from(protoMessage.getMessageBody()), false);
        processedMessage = new XmlMessage(protoMessage.getService(), protoMessage.getEndpoint(), protoMessage.getOperation(), protoMessage.getUrl(), protoMessage.getMethod(), protoMessage.getContentType(), instantiatedContent);
      } catch (XmlException e) {
        throw new RuntimeException(e);
      }

      ((XmlMessage) processedMessage).setContent(instantiatedContent.getNamespaces(), instantiatedContent.getContent());
      mMessages.add(processedMessage);
      mMessageNodes.add(new HProcessNodeInstance(instance.getHandle()));
      return true;
    }
  }

  private static class StubTransaction implements Transaction {

    @Override
    public void close() { }

    @Override
    public void commit() throws SQLException { }

    @Override
    public void rollback() throws SQLException {
      System.err.println("Rollback needed (but not supported on the stub");
    }

    @Override
    public <T> T commit(final T value) throws SQLException {
      return value;
    }
  }

  private static class StubTransactionFactory implements net.devrieze.util.TransactionFactory<StubTransaction> {

    private StubTransaction mTransaction = new StubTransaction();

    @Override
    public StubTransaction startTransaction() {
      return mTransaction;
    }


    @Override
    public Connection getConnection() throws SQLException {
      throw new UnsupportedOperationException("No connections in the stub");
    }

    @Override
    public boolean isValidTransaction(final StubTransaction transaction) {
      return mTransaction==transaction;
    }
  }

  private class MemTransactionedHandleMap<T> extends MemHandleMap<T> implements net.devrieze.util.TransactionedHandleMap<T,StubTransaction> {

    @Override
    public long put(final StubTransaction transaction, final T value) throws SQLException {
      return put(value);
    }

    @Override
    public T get(final StubTransaction transaction, final long handle) throws SQLException {
      return get(handle);
    }

    @Override
    public T get(final StubTransaction transaction, final Handle<? extends T> handle) throws SQLException {
      return get(handle);
    }

    @Override
    public T castOrGet(final StubTransaction transaction, final Handle<? extends T> handle) throws SQLException {
      return get(handle);
    }

    @Override
    public T set(final StubTransaction transaction, final long handle, final T value) throws SQLException {
      return set(handle, value);
    }

    @Override
    public T set(final StubTransaction transaction, final Handle<? extends T> handle, final T value) throws
            SQLException {
      return set(handle, value);
    }

    @Override
    public Iterable<T> iterable(final StubTransaction transaction) {
      return this;
    }

    @Override
    public boolean contains(final StubTransaction transaction, final Object o) throws SQLException {
      return contains(o);
    }

    @Override
    public boolean contains(final StubTransaction transaction, final Handle<? extends T> handle) throws SQLException {
      return contains(handle);
    }

    @Override
    public boolean contains(final StubTransaction transaction, final long handle) throws SQLException {
      return contains(handle);
    }

    @Override
    public boolean remove(final StubTransaction transaction, final Handle<? extends T> object) throws SQLException {
      return remove(object);
    }

    @Override
    public boolean remove(final StubTransaction transaction, final long handle) throws SQLException {
      return remove(handle);
    }

    @Override
    public void invalidateCache(final Handle<? extends T> handle) { /* No-op */ }

    @Override
    public void clear(final StubTransaction transaction) throws SQLException {
      clear();
    }
  }

  private static DocumentBuilder _documentBuilder;

  ProcessEngine mProcessEngine;
  private StubMessageService mStubMessageService;
  private final EndpointDescriptor mLocalEndpoint = new EndpointDescriptorImpl(QName.valueOf("processEngine"),"processEngine", URI.create("http://localhost/"));
  private net.devrieze.util.TransactionFactory<nl.adaptivity.process.engine.TestProcessEngine.StubTransaction> mStubTransactionFactory;
  private SimplePrincipal mPrincipal;

  public TestProcessEngine() {
    mStubMessageService = new StubMessageService();
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
      assertTrue("Some bytes in the xml files are expected", byteArray.length>0);
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

  @Before
  public void beforeTest() {
    mProcessEngine = ProcessEngine.newTestInstance(mStubMessageService, mStubTransactionFactory, new MemTransactionedHandleMap<ProcessModelImpl>(), new MemTransactionedHandleMap<ProcessInstance>(), new MemTransactionedHandleMap<ProcessNodeInstance>());
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

    assertEquals(1, mStubMessageService.mMessages.size());
    assertEquals(1L, mStubMessageService.mMessageNodes.get(0).getHandle());

    InputStream expected = getXml("testModel1_task1.xml");

    char[] receivedChars = serializeToXmlCharArray(mStubMessageService.mMessages.get(0));

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(expected), new CharArrayReader(receivedChars));

    ProcessInstance processInstance = mProcessEngine.getProcessInstance(transaction,instanceHandle ,mPrincipal);
    assertEquals(State.STARTED, processInstance.getState());

    assertEquals(1, processInstance.getActive().size());
    assertEquals(1, processInstance.getFinished().size());
    Handle<? extends ProcessNodeInstance> hfinished = processInstance.getFinished().iterator().next();
    ProcessNodeInstance finished = mProcessEngine.getNodeInstance(transaction, hfinished, mPrincipal);
    assertTrue(finished.getNode() instanceof StartNodeImpl);
    assertEquals("start", finished.getNode().getId());

    assertEquals(0, processInstance.getResults().size());

    ProcessNodeInstance taskNode = mProcessEngine.getNodeInstance(transaction, mStubMessageService.mMessageNodes.get(0), mPrincipal);
    assertEquals(TaskState.Pending, taskNode.getState()); // Our messenger does not do delivery notification

    assertEquals(TaskState.Complete, mProcessEngine.finishTask(transaction, taskNode, null, mPrincipal));
    assertEquals(0, processInstance.getActive().size());
    assertEquals(2, processInstance.getFinished().size());
    assertEquals(1, processInstance.getResults().size());

    assertEquals(State.FINISHED, processInstance.getState());
  }

  @Test
  public void testGetDataFromTask() throws Exception {
    ProcessModelImpl model = getProcessModel("testModel2.xml");
    StubTransaction transaction = mStubTransactionFactory.startTransaction();
    IProcessModelRef modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(1, mStubMessageService.mMessages.size());

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(getXml("testModel2_task1.xml")), new CharArrayReader(serializeToXmlCharArray(mStubMessageService.mMessages.get(0))));
    ProcessNodeInstance ac1 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.mMessageNodes.get(0), mPrincipal);// This should be 0 as it's the first activity


    mStubMessageService.clear(); // (Process the message)
    assertEquals(0, ac1.getResults().size());
    assertEquals(TaskState.Complete,mProcessEngine.finishTask(transaction, ac1, getDocument("testModel2_response1.xml"), mPrincipal));
    assertEquals(2, ac1.getResults().size());
    ProcessData result1 = ac1.getResults().get(0);
    ProcessData result2 = ac1.getResults().get(1);
    assertEquals("name", result1.getName());
    assertEquals("Paul", result1.getContent().getContentString());
    assertEquals("user", result2.getName());
    assertXMLEqual("<user><fullname>Paul</fullname></user>", result2.getContent().getContentString());

    assertEquals(1, mStubMessageService.mMessages.size());
    assertEquals(2L, mStubMessageService.mMessageNodes.get(0).getHandle()); //We should have a new message with the new task (with the data)
    ProcessNodeInstance ac2=mProcessEngine.getNodeInstance(transaction, mStubMessageService.mMessageNodes.get(0), mPrincipal);

    List<ProcessData> ac2Defines = ac2.getDefines(transaction);
    assertEquals(1, ac2Defines.size());


    ProcessData define = ac2Defines.get(0);
    assertEquals("mylabel", define.getName());
    assertEquals("Hi Paul. Welcome!", define.getContent().getContentString());

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
