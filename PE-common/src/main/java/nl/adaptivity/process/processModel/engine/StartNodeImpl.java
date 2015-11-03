package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@XmlDeserializer(StartNodeImpl.Factory.class)
@XmlRootElement(name = StartNodeImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNodeImpl extends ProcessNodeImpl implements StartNode<ProcessNodeImpl> {

  public static class Factory implements XmlDeserializerFactory {

    @Override
    public StartNodeImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return StartNodeImpl.deserialize(null, in);
    }
  }

  public static StartNodeImpl deserialize(final ProcessModelImpl pOwnerModel, final XMLStreamReader in) throws XMLStreamException {
    assert in.getEventType()== XMLStreamConstants.START_ELEMENT;
    assert ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI()) && ELEMENTLOCALNAME.equals(in.getLocalName());

    StartNodeImpl result = new StartNodeImpl(pOwnerModel);

    for(int i=0; i<in.getAttributeCount(); ++i) {
      switch (in.getAttributeLocalName(i)) {
        default:
          result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
      }
    }

    int t;
    while ((t=in.next())!=XMLStreamConstants.END_ELEMENT) {
      switch (t) {
        case XMLStreamConstants.START_ELEMENT: {
          if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI())) {
            switch (in.getLocalName()) {
              case "import":
                result.getResults().add(XmlResultType.deserialize(in)); break;
            }
          }
        }

        default:
          XmlUtil.unhandledEvent(in);
          break;
      }
    }
    return result;
  }

  private static final long serialVersionUID = 7779338146413772452L;

  public static final String ELEMENTLOCALNAME = "start";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private List<XmlResultType> aImports;

  public StartNodeImpl(final ProcessModelImpl pOwnerModel) {
    super(pOwnerModel, Collections.<Identifiable>emptyList());
  }

  public StartNodeImpl(final ProcessModelImpl pOwnerModel, final List<XmlResultType> pImports) {
    super(pOwnerModel, Collections.<Identifiable>emptyList());
    aImports = pImports;
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeAttributes(pOut);
  }

  protected void serializeChildren(final XMLStreamWriter pOut) throws XMLStreamException {
    for(XmlResultType imp: aImports) {
      imp.serialize(pOut);
    }
  }

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.StartNode#getImports()
   */
  @Override
  @XmlElement(name = "import")
  public List<XmlResultType> getResults() {
    if (aImports == null) {
      aImports = new ArrayList<>();
    }
    return this.aImports;
  }

  @Override
  public int getMaxPredecessorCount() {
    return 0;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction pTransaction, final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitStartNode(this);
  }

}
