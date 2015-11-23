package nl.adaptivity.process.processModel;

import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.adapters.XmlAdapter;


public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel<? extends ExecutableProcessNode>> {

  @NotNull
  @Override
  public XmlProcessModel marshal(@NotNull final ProcessModel<? extends ExecutableProcessNode> processModel) throws Exception {
    return new XmlProcessModel(processModel);
  }

  @NotNull
  @Override
  public ProcessModel<? extends ExecutableProcessNode> unmarshal(@NotNull final XmlProcessModel model) throws Exception {
    return new ProcessModelImpl(model);
  }

}
