package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import org.jetbrains.annotations.NotNull;


public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel<? extends ProcessNodeImpl>> {

  @NotNull
  @Override
  public XmlProcessModel marshal(@NotNull final ProcessModel<? extends ProcessNodeImpl> processModel) throws Exception {
    return new XmlProcessModel(processModel);
  }

  @NotNull
  @Override
  public ProcessModel<? extends ProcessNodeImpl> unmarshal(@NotNull final XmlProcessModel model) throws Exception {
    return new ProcessModelImpl(model);
  }

}
