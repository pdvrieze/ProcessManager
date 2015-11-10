package nl.adaptivity.process.processModel.engine;

import java.util.UUID;

import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import org.jetbrains.annotations.Nullable;


public interface IProcessModelRef<T extends ProcessNode<T>>  extends Handle<ProcessModel<T>>{

  String getName();

  @Nullable
  UUID getUuid();

}