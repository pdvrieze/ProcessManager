package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.ArrayList;
import java.util.List;


@XmlDeserializer(StartNodeImpl.Factory.class)
@XmlRootElement(name = StartNodeImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNodeImpl extends ProcessNodeImpl implements StartNode<ProcessNodeImpl> {

  public class Factory implements XmlDeserializerFactory {

    @Override
    public StartNodeImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return StartNodeImpl.deserialize(in);
    }
  }

  public static StartNodeImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
    assert in.getEventType()== XMLStreamConstants.START_ELEMENT;
    assert ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI()) && ELEMENTNAME.equals(in.getLocalName());

    StartNodeImpl result = new StartNodeImpl();

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

  public StartNodeImpl() {
  }

  private static final long serialVersionUID = 7779338146413772452L;

  public static final String ELEMENTNAME = "start";

  private List<XmlResultType> aImports;

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
