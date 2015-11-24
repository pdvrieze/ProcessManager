package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.EndNodeBase;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlDeserializer(EndNodeImpl.Factory.class)
@XmlRootElement(name = EndNode.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNodeImpl extends EndNodeBase<ExecutableProcessNode> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public EndNodeImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return EndNodeImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static EndNodeImpl deserialize(final ProcessModelBase<ExecutableProcessNode> ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new EndNodeImpl(ownerModel), in);
  }

  public EndNodeImpl(final ProcessModelBase<ExecutableProcessNode> ownerModel, final ExecutableProcessNode previous) {
    super(ownerModel);
    setPredecessor(previous);
  }

  public EndNodeImpl(final ProcessModelBase<ExecutableProcessNode> ownerModel) {
    super(ownerModel);
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final Transaction transaction, final IMessageService<T, U> messageService, final U instance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> messageService, final U instance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> messageService, final U instance) {
    //    pProcessInstance.finish();
    return true;
  }

}
