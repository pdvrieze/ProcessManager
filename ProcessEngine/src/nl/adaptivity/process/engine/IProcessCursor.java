package nl.adaptivity.process.engine;

import java.util.Collection;


public interface IProcessCursor {

  Collection<ITransition> completeActivity(IMessage pMessage);

}
