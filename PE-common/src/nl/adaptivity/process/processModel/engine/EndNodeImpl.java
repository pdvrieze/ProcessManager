package nl.adaptivity.process.processModel.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlExportType;


@XmlRootElement(name = EndNodeImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNodeImpl extends ProcessNodeImpl implements EndNode<ProcessNodeImpl> {

  private List<XmlExportType> aExports;

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
  public List<? extends XmlExportType> getExports() {
    if (aExports == null) {
      aExports = new ArrayList<>();
    }
    return aExports;
  }

  @Override
  public void setExports(Collection<? extends IXmlExportType> pExports) {
    aExports = toExportableExports(pExports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getSuccessors()
   */
  @Override
  public Set<? extends ProcessNodeImpl> getSuccessors() {
    return Collections.emptySet();
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
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
