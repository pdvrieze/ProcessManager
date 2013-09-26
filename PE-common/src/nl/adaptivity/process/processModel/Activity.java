package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.List;


public interface Activity<T extends ProcessNode<T>> extends ProcessNode<T> {

  /**
   * Get the name of the activity.
   *
   * @return The name
   */
  public abstract String getName();

  /**
   * Set the name of this activity. Note that for serialization to XML to work
   * this needs to be unique for the process model at time of serialization, and
   * can not be null or an empty string. While in Java mode other nodes are
   * referred to by reference, not name.
   *
   * @param pName The name of the activity.
   */
  public abstract void setName(String pName);

  /**
   * Get the condition of the activity.
   *
   * @return The condition.
   */
  public abstract String getCondition();

  /**
   * Set the condition that needs to be true to start this activity.
   *
   * @param pCondition The condition.
   */
  public abstract void setCondition(String pCondition);

  /**
   * Get the list of imports. The imports are provided to the message for use as
   * data parameters.
   *
   * @return The list of imports.
   */
  public abstract List<XmlImportType> getImports();

  /**
   * Set the import requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param pImports The imports to set.
   */
  public abstract void setImports(Collection<? extends XmlImportType> pImports);

  /**
   * Get the list of exports. Exports will allow storing the response of an
   * activity.
   *
   * @return The list of exports.
   */
  public abstract List<XmlExportType> getExports();

  /**
   * Set the export requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param pExports The exports to set.
   */
  public abstract void setExports(Collection<? extends XmlExportType> pExports);

  /**
   * Get the predecessor node for this activity.
   *
   * @return the predecessor
   */
  public abstract T getPredecessor();

  /**
   * Set the predecessor for this activity.
   *
   * @param predecessor The predecessor
   */
  public abstract void setPredecessor(T predecessor);

  /**
   * Get the message of this activity. This provides all the information to be
   * able to actually invoke the service.
   *
   * @return The message.
   */
  public abstract XmlMessage getMessage();

  /**
   * Set the message of this activity. This encodes what actually needs to be
   * done when the activity is activated.
   *
   * @param message The message.
   */
  public abstract void setMessage(XmlMessage message);

}