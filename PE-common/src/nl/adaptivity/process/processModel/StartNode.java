package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;


@XmlRootElement(name = StartNode.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNode extends ProcessNode {

  public StartNode() {
    super((ProcessNode) null);
  }

  private static final long serialVersionUID = 7779338146413772452L;

  public static final String ELEMENTNAME = "start";

  private List<XmlImportType> aImports;

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  @XmlElement(name = "import")
  public List<XmlImportType> getImport() {
    if (aImports == null) {
      aImports = new ArrayList<XmlImportType>();
    }
    return this.aImports;
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
