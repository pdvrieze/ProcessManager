package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.*;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.*;


@XmlDeserializer(EndNodeImpl.Factory.class)
@XmlRootElement(name = EndNodeImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNodeImpl extends ProcessNodeImpl implements EndNode<ProcessNodeImpl> {

  public class Factory implements XmlDeserializerFactory {

    @Override
    public EndNodeImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return EndNodeImpl.deserialize(in);
    }
  }

  public static EndNodeImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
    assert in.getEventType()== XMLStreamConstants.START_ELEMENT;
    assert ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI()) && ELEMENTNAME.equals(in.getLocalName());

    EndNodeImpl result = new EndNodeImpl();

    for(int i=0; i<in.getAttributeCount(); ++i) {
      switch (in.getAttributeLocalName(i)) {
        default:
          result.deserializeAttr(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
      }
    }

    int t;
    while ((t=in.next())!=XMLStreamConstants.END_ELEMENT) {
      switch (t) {
        case XMLStreamConstants.START_ELEMENT: {
          if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI())) {
            switch (in.getLocalName()) {
              case "export":
                result.getDefines(); result.aExports.add(XmlDefineType.deserialize(in)); break;
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

  private List<XmlDefineType> aExports;

  public EndNodeImpl(final ProcessNodeImpl pPrevious) {
    super(Collections.singletonList(pPrevious));
  }

  public EndNodeImpl() {}

  private static final long serialVersionUID = 220908810658246960L;

  public static final String ELEMENTNAME = "end";

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getPredecessor()
   */
  @Override
  @XmlAttribute(name = "predecessor", required = true)
  @XmlIDREF
  public ProcessNodeImpl getPredecessor() {
    final Collection<? extends ProcessNodeImpl> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#setPredecessor(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public void setPredecessor(final ProcessNodeImpl predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getExports()
   */
  @Override
  @XmlElement(name = "export")
  public List<? extends XmlDefineType> getDefines() {
    if (aExports == null) {
      aExports = new ArrayList<>();
    }
    return aExports;
  }

  @Override
  public void setDefines(Collection<? extends IXmlDefineType> pExports) {
    aExports = toExportableDefines(pExports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getSuccessors()
   */
  @Override
  public Set<? extends ProcessNodeImpl> getSuccessors() {
    return Collections.emptySet();
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
    //    pProcessInstance.finish();
    return true;
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitEndNode(this);
  }

}
