package nl.adaptivity.process.engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.process.processModel.ProcessModel;


@XmlRootElement(name=HProcessModel.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class HProcessModel implements Handle<ProcessModel> {
  
  public static final String ELEMENTNAME = "processModelHandle";

  private long aHandle;
  
  protected HProcessModel() {
    aHandle = -1l;
  }

  public HProcessModel(ProcessModel pProcessModel) {
    aHandle = pProcessModel.getHandle();
  }
  
  public HProcessModel(long pHandle) {
    aHandle = pHandle;
  }

  @XmlValue
  @Override
  public long getHandle() {
    return aHandle;
  }
  
  public void setHandle(long pHandle) {
    aHandle = pHandle;
  }

}
