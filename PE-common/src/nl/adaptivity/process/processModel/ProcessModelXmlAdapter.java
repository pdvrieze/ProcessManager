package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.adaptivity.process.processModel.engine.ProcessModelImpl;


public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel> {

  @Override
  public XmlProcessModel marshal(final ProcessModel pProcessModel) throws Exception {
    return new XmlProcessModel(pProcessModel);
  }

  @Override
  public ProcessModel unmarshal(final XmlProcessModel pModel) throws Exception {
    return new ProcessModelImpl(pModel);
  }

}
