package nl.adaptivity.process.processModel;

import net.devrieze.util.security.SecureObject;


/**
 * Created by pdvrieze on 18/08/15.
 */
public interface SecureProcessModel<T extends ProcessNode<? extends T, M>, M extends ProcessModelBase<T, M>> extends ProcessModel<T, M>, SecureObject {

}
