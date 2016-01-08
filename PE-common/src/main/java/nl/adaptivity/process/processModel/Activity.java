package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.List;


public interface Activity<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M> {

  /** The name of the XML element. */
  String ELEMENTLOCALNAME = "activity";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  /**
   * Get the name of the activity.
   *
   * @return The name
   */
  String getName();

  /**
   * Set the name of this activity. Note that for serialization to XML to work
   * this needs to be unique for the process model at time of serialization, and
   * can not be null or an empty string. While in Java mode other nodes are
   * referred to by reference, not name.
   *
   * @param name The name of the activity.
   */
  void setName(String name);

  /**
   * Get the condition of the activity.
   *
   * @return The condition.
   */
  @Nullable
  String getCondition();

  /**
   * Set the condition that needs to be true to start this activity.
   *
   * @param condition The condition.
   */
  void setCondition(String condition);

  /**
   * Get the list of imports. The imports are provided to the message for use as
   * data parameters.
   *
   * @return The list of imports.
   */
  @Override
  List<? extends IXmlResultType> getResults();

  /**
   * Set the import requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param results The imports to set.
   */
  void setResults(Collection<? extends IXmlResultType> results);

  /**
   * Get the list of exports. Exports will allow storing the response of an
   * activity.
   *
   * @return The list of exports.
   */
  @Override
  List<? extends IXmlDefineType> getDefines();

  /**
   * Set the export requirements for this activity. This will create a copy of
   * the parameter for safety.
   *
   * @param defines The exports to set.
   */
  void setDefines(Collection<? extends IXmlDefineType> defines);

  /**
   * Get the predecessor node for this activity.
   *
   * @return the predecessor
   */
  @Nullable
  Identifiable getPredecessor();

  /**
   * Set the predecessor for this activity.
   *
   * @param predecessor The predecessor
   */
  void setPredecessor(Identifiable predecessor);

  /**
   * Get the message of this activity. This provides all the information to be
   * able to actually invoke the service.
   *
   * @return The message.
   */
  IXmlMessage getMessage();

  /**
   * Set the message of this activity. This encodes what actually needs to be
   * done when the activity is activated.
   *
   * @param message The message.
   */
  void setMessage(IXmlMessage message);

}