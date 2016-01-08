package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public interface IProcessModelRef<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends Handle<ProcessModel<T, M>>{

  String getName();

  @Nullable
  UUID getUuid();

}