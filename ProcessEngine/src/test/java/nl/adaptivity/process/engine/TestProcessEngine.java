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
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;


/**
 * Created by pdvrieze on 18/08/15.
 */
public class TestProcessEngine {

  private class StubMessageService implements IMessageService<IXmlMessage,ProcessNodeInstance> {


    List<IXmlMessage> mMessages=new ArrayList<>();
    private List<Handle<? extends ProcessNodeInstance>> mMessageNodes = new ArrayList<>();

    @Override
    public IXmlMessage createMessage(final IXmlMessage pMessage) {
      return pMessage;
    }

    @Override
    public EndpointDescriptor getLocalEndpoint() {
      return mLocalEndpoint;
    }

    @Override
    public boolean sendMessage(final Transaction pTransaction, final IXmlMessage pMessage, final ProcessNodeInstance pInstance) throws
            SQLException {
      try {
        Source source = pMessage.getBodySource();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result unformattedResult = new StreamResult(out);
        pInstance.instantiateXmlPlaceholders(pTransaction, source, unformattedResult, false);

        Document resultDocument = getDocumentBuilder().newDocument();
        DOMResult formattedResult = new DOMResult(resultDocument);
        Sources.writeToResult(new StreamSource(new ByteArrayInputStream(out.toByteArray())), formattedResult, true);
        pMessage.setMessageBody(resultDocument.getDocumentElement());
        mMessages.add(pMessage);
        mMessageNodes.add(new HProcessNodeInstance(pInstance.getHandle()));
        return true;
      } catch (XMLStreamException|TransformerException e) {
        throw new RuntimeException(e);
      }
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
    public <T> T commit(final T pValue) throws SQLException {
      return pValue;
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
    public boolean isValidTransaction(final StubTransaction pTransaction) {
      return mTransaction==pTransaction;
    }
  }

  private class MemTransactionedHandleMap<T> extends MemHandleMap<T> implements net.devrieze.util.TransactionedHandleMap<T,StubTransaction> {

    @Override
    public long put(final StubTransaction pTransaction, final T pValue) throws SQLException {
      return put(pValue);
    }

    @Override
    public T get(final StubTransaction pTransaction, final long pHandle) throws SQLException {
      return get(pHandle);
    }

    @Override
    public T get(final StubTransaction pTransaction, final Handle<? extends T> pHandle) throws SQLException {
      return get(pHandle);
    }

    @Override
    public T set(final StubTransaction pTransaction, final long pHandle, final T pValue) throws SQLException {
      return set(pHandle, pValue);
    }

    @Override
    public T set(final StubTransaction pTransaction, final Handle<? extends T> pHandle, final T pValue) throws
            SQLException {
      return set(pHandle, pValue);
    }

    @Override
    public Iterable<T> iterable(final StubTransaction pTransaction) {
      return this;
    }

    @Override
    public boolean contains(final StubTransaction pTransaction, final Object pO) throws SQLException {
      return contains(pO);
    }

    @Override
    public boolean contains(final StubTransaction pTransaction, final Handle<? extends T> pHandle) throws SQLException {
      return contains(pHandle);
    }

    @Override
    public boolean contains(final StubTransaction pTransaction, final long pHandle) throws SQLException {
      return contains(pHandle);
    }

    @Override
    public boolean remove(final StubTransaction pTransaction, final Handle<? extends T> pObject) throws SQLException {
      return remove(pObject);
    }

    @Override
    public boolean remove(final StubTransaction pTransaction, final long pHandle) throws SQLException {
      return remove(pHandle);
    }

    @Override
    public void invalidateCache(final Handle<? extends T> pHandle) { /* No-op */ }

    @Override
    public void clear(final StubTransaction pTransaction) throws SQLException {
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

  private char[] jaxbToCharArray(final Object pObject) {
    char[] receivedChars;
    {
      CharArrayWriter caw = new CharArrayWriter();
      JAXB.marshal(pObject, caw);
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
  public void testExecuteSingleActivity() throws SQLException, IOException, SAXException, ParserConfigurationException {
    XmlProcessModel xmlmodel = JAXB.unmarshal(getXml("testModel1.xml"), XmlProcessModel.class);
    ProcessModelImpl model = new ProcessModelImpl(xmlmodel);
    StubTransaction transaction = mStubTransactionFactory.startTransaction();
    IProcessModelRef modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(1, mStubMessageService.mMessages.size());
    assertEquals(1L, mStubMessageService.mMessageNodes.get(0).getHandle());

    InputStream expected = getXml("testModel1_task1.xml");

    char[] receivedChars = jaxbToCharArray(mStubMessageService.mMessages.get(0));

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(expected), new CharArrayReader(receivedChars));

    ProcessInstance processInstance = mProcessEngine.getProcessInstance(transaction,instanceHandle ,mPrincipal);
    assertEquals(State.STARTED, processInstance.getState());

    assertEquals(1, processInstance.getActive().size());
    assertEquals(1, processInstance.getFinished().size());
    ProcessNodeInstance finished = processInstance.getFinished().iterator().next();
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
  public void testGetDataFromTask() throws SQLException, IOException, SAXException, TransformerException {
    XmlProcessModel xmlmodel = JAXB.unmarshal(getXml("testModel2.xml"), XmlProcessModel.class);
    ProcessModelImpl model = new ProcessModelImpl(xmlmodel);
    StubTransaction transaction = mStubTransactionFactory.startTransaction();
    IProcessModelRef modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal);

    HProcessInstance instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null);

    assertEquals(1, mStubMessageService.mMessages.size());

    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(new InputStreamReader(getXml("testModel2_task1.xml")), new CharArrayReader(jaxbToCharArray(mStubMessageService.mMessages.get(0))));
    ProcessNodeInstance ac1 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.mMessageNodes.get(0), mPrincipal);// This should be 0 as it's the first activity


    mStubMessageService.mMessages.clear(); // (Process the message)
    assertEquals(0, ac1.getResults().size());
    assertEquals(TaskState.Complete,mProcessEngine.finishTask(transaction, ac1, getDocument("testModel2_response1.xml"), mPrincipal));
    assertEquals(2, ac1.getResults().size());
    ProcessData result1 = ac1.getResults().get(0);
    ProcessData result2 = ac1.getResults().get(1);
    assertEquals("name", result1.getName());
    assertEquals("Paul", XmlUtil.toString(result1.getNodeValue()));
    assertEquals("user", result2.getName());
    assertXMLSimilar(XmlUtil.tryParseXml("<user><fullname>Paul</fullname></user>"), toDocument(result2.getNodeValue()));
    assertEquals("<user><fullname>Paul</fullname></user>", XmlUtil.toString(result2.getNodeValue()));

    assertEquals(1, mStubMessageService.mMessages.size());
    assertEquals(1L, mStubMessageService.mMessageNodes.get(0)); //We should have a new message with the new task (with the data)
    ProcessNodeInstance ac2=mProcessEngine.getNodeInstance(transaction, mStubMessageService.mMessageNodes.get(0), mPrincipal);

    assertEquals(1, ac2.getDefines(transaction).size());


    ProcessData define = ac2.getDefines(transaction).get(0);
    assertEquals("mylabel", define.getName());
    assertEquals("Paul", XmlUtil.toString(define.getNodeValue()));

  }

  private static void assertXMLSimilar(final Document pExpected, final Document pActual) {
    Diff diff = XMLUnit.compareXML(pExpected, pActual);
    DetailedDiff detailedDiff = new DetailedDiff(diff);
    if(! detailedDiff.similar()) {
      fail(detailedDiff.toString());
    }
  }

  private static Document toDocument(final Node pNode) throws TransformerException {
    Document result = getDocumentBuilder().newDocument();
    Sources.writeToResult(new DOMSource(pNode), new DOMResult(result));
    return result;
  }


}
