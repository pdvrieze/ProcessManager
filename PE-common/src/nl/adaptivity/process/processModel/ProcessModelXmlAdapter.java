package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;


public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel<? extends ProcessNodeImpl>> {

  @Override
  public XmlProcessModel marshal(final ProcessModel<? extends ProcessNodeImpl> pProcessModel) throws Exception {
    return new XmlProcessModel(pProcessModel);
  }

  @Override
  public ProcessModel<? extends ProcessNodeImpl> unmarshal(final XmlProcessModel pModel) throws Exception {
    return new ProcessModelImpl(pModel);
  }

}
