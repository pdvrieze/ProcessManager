package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


@XmlRootElement(name = EndNode.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNode extends ProcessNode {

  private List<XmlExportType> aExports;

  public EndNode(final ProcessNode pPrevious) {
    super(pPrevious);
  }

  public EndNode() {}

  private static final long serialVersionUID = 220908810658246960L;

  public static final String ELEMENTNAME = "end";

  @Override
  public boolean condition(final Task<?> pInstance) {
    return true;
  }

  @XmlAttribute(name = "predecessor", required = true)
  @XmlIDREF
  public ProcessNode getPredecessor() {
    final Collection<ProcessNode> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  public void setPredecessor(final ProcessNode predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  @XmlElement(name = "export")
  public List<XmlExportType> getExports() {
    if (aExports == null) {
      aExports = new ArrayList<XmlExportType>();
    }
    return aExports;
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    return new ArrayList<ProcessNode>(0);
  }

  @Override
  public <T, U extends Task<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends Task<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends Task<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    //    pProcessInstance.finish();
    return true;
  }

}
