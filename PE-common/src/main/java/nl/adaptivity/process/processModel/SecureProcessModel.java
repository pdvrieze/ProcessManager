package nl.adaptivity.process.processModel;

import net.devrieze.util.security.SecureObject;


/**
 * Created by pdvrieze on 18/08/15.
 */
public interface SecureProcessModel<T extends ProcessNode<? extends T>> extends ProcessModel<T>, SecureObject {

}
