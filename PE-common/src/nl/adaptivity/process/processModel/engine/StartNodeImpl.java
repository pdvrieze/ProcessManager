package nl.adaptivity.process.processModel.engine;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.IXmlImportType;


@XmlRootElement(name = StartNodeImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNodeImpl extends ProcessNodeImpl implements StartNode<ProcessNodeImpl> {

  public StartNodeImpl() {
  }

  private static final long serialVersionUID = 7779338146413772452L;

  public static final String ELEMENTNAME = "start";

  private List<IXmlImportType> aImports;

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.StartNode#getImports()
   */
  @Override
  @XmlElement(name = "import")
  public List<IXmlImportType> getImports() {
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
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
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
}
