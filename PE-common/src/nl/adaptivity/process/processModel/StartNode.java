package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


@XmlRootElement(name="start")
@XmlAccessorType(XmlAccessType.NONE)
public class StartNode extends ProcessNode {

  public StartNode() {
    super((ProcessNode) null);
  }

  private static final long serialVersionUID = 7779338146413772452L;

  private List<XmlImportType> aImports;

  @Override
  public boolean condition() {
    return true;
  }

  @XmlElement(name="import")
  public List<XmlImportType> getImport() {
    if (aImports == null) {
      aImports = new ArrayList<XmlImportType>();
    }
    return this.aImports;
  }

  @Override
  public <T> boolean provideTask(IMessageService<T> pMessageService, Task pInstance) {
    return true;
  }

  @Override
  public <T> boolean takeTask(IMessageService<T> pMessageService, Task pInstance) {
    return true;
  }

  @Override
  public <T> boolean startTask(IMessageService<T> pMessageService, Task pInstance) {
    return true;
  }
}
