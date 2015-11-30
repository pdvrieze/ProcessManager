package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.SplitBase;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;


@XmlDeserializer(SplitImpl.Factory.class)
public class SplitImpl extends SplitBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public SplitImpl deserialize(final XmlReader in) throws XmlException {
      return SplitImpl.deserialize(null, in);
    }
  }

  public SplitImpl(final ProcessModelImpl  ownerModel, final ExecutableProcessNode predecessor, final int min, final int max) {
    super(ownerModel, Collections.singleton(predecessor), max, min);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public SplitImpl(final ProcessModelImpl  ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static SplitImpl andSplit(final ProcessModelImpl ownerModel, final ExecutableProcessNode predecessor) {
    return new SplitImpl(ownerModel, predecessor, Integer.MAX_VALUE, Integer.MAX_VALUE);
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
    return true;
  }

}
