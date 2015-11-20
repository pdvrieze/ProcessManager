package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.StringUtil;
import net.devrieze.util.Transaction;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of
 *
 * @author Paul de Vrieze
 */
@XmlDeserializer(ActivityImpl.Factory.class)
@XmlRootElement(name = ActivityImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ActivityImpl.ELEMENTLOCALNAME + "Type", propOrder = { "defines", "results", "condition", XmlMessage.ELEMENTLOCALNAME})
public class ActivityImpl extends ProcessNodeImpl implements Activity<ProcessNodeImpl>, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public ActivityImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return ActivityImpl.deserialize(null, in);
    }
  }

  private static final long serialVersionUID = 282944120294737322L;

  /** The name of the XML element. */
  public static final String ELEMENTLOCALNAME = "activity";

  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  @Nullable private String mName;

  @Nullable private ConditionImpl mCondition;

  @NotNull private List<XmlResultType> mResults = new ArrayList<>();

  @NotNull private List<XmlDefineType> mDefines = new ArrayList<>();

  private XmlMessage mMessage;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param predecessor The process node that starts immediately precedes this
   *          activity.
   */
  public ActivityImpl(final ProcessModelImpl ownerModel, final ProcessNodeImpl predecessor) {
    super(ownerModel, Collections.singletonList(predecessor));
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public ActivityImpl(final ProcessModelImpl ownerModel) {super(ownerModel);}

  @NotNull
  public static ActivityImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new ActivityImpl(ownerModel), in);
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case ATTR_PREDECESSOR: setPredecessor(new Identifier(attributeValue.toString())); return true;
      case "name": setName(StringUtil.toString(attributeValue)); return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case XmlDefineType.ELEMENTLOCALNAME:
          mDefines.add(XmlDefineType.deserialize(in));return true;
        case XmlResultType.ELEMENTLOCALNAME:
          mResults.add(XmlResultType.deserialize(in));return true;
        case ConditionImpl.ELEMENTLOCALNAME:
          mCondition = ConditionImpl.deserialize(in); return true;
        case XmlMessage.ELEMENTLOCALNAME:
          mMessage = XmlMessage.deserialize(in);return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.endTag(null, null, null);
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    out.attribute(null, ATTR_PREDECESSOR, null, getPredecessor().getId());
    XmlUtil.writeAttribute(out, "name", getName());
  }

  protected void serializeChildren(final XmlWriter out) throws XmlException {
    super.serializeChildren(out);
    XmlUtil.writeChildren(out, getDefines());
    XmlUtil.writeChildren(out, getResults());
    XmlUtil.writeChild(out, mCondition);
    if (mCondition !=null) { mCondition.serialize(out); }

    {
      final XmlMessage m = getMessage();
      if (m!=null) { m.serialize(out); }
    }
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IActivity#getName()
     */
  @Override
  @XmlAttribute
  public String getName() {
    return mName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setName(java.lang.String)
   */
  @Override
  public void setName(final String name) {
    mName = name;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getCondition()
   */
  @Nullable
  @Override
  @XmlElement(name = ConditionImpl.ELEMENTLOCALNAME)
  public String getCondition() {
    return mCondition ==null ? null : mCondition.toString();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setCondition(java.lang.String)
   */
  @Override
  public void setCondition(@Nullable final String condition) {
    mCondition = condition==null ? null : new ConditionImpl(condition);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getImports()
   */
  @NotNull
  @Override
  @XmlElement(name = XmlResultType.ELEMENTLOCALNAME)
  public List<? extends XmlResultType> getResults() {
    if (mResults ==null) {
      mResults = new ArrayList<>();
    }
    return mResults;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setImports(java.util.Collection)
   */
  @Override
  public void setResults(@Nullable final Collection<? extends IXmlResultType> imports) {
    mResults = imports==null ? new ArrayList<XmlResultType>(0) : toExportableResults(imports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getExports()
   */
  @NotNull
  @Override
  @XmlElement(name = XmlDefineType.ELEMENTLOCALNAME)
  public List<? extends XmlDefineType> getDefines() {
    if (mDefines ==null) {
      mDefines = new ArrayList<>();
    }
    return mDefines;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setExports(java.util.Collection)
   */
  @Override
  public void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    mDefines = exports==null ? new ArrayList<XmlDefineType>(0) : toExportableDefines(exports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getPredecessor()
   */
  @Nullable
  @Override
  @XmlAttribute(name = ATTR_PREDECESSOR, required = true)
  @XmlIDREF
  public Identifiable getPredecessor() {
    final Collection<? extends Identifiable> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setPredecessor(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public void setPredecessor(final Identifiable predecessor) {
    setPredecessors(Collections.singleton(predecessor));
  }



  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#getMessage()
   */
  @Override
  @XmlElement(name = XmlMessage.ELEMENTLOCALNAME, required = true)
  public XmlMessage getMessage() {
    return mMessage;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IActivity#setMessage(nl.adaptivity.process.processModel.XmlMessage)
   */
  @Override
  public void setMessage(final IXmlMessage message) {
    mMessage = XmlMessage.get(message);
  }

  public void setMessage(final XmlMessage message) {
    mMessage = XmlMessage.get(message);
  }

  /**
   * Determine whether the process can start.
   */
  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    if (mCondition == null) {
      return true;
    }
    return mCondition.eval(transaction, instance);
  }

  /**
   * This will actually take the message element, and send it through the
   * message service.
   *
   * @param messageService The message service to use to send the message.
   * @param instance The processInstance that represents the actual activity
   *          instance that the message responds to.
   * @throws SQLException
   * @todo handle imports.
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final Transaction transaction, @NotNull final IMessageService<T, U> messageService, @NotNull final U instance) throws SQLException {
    // TODO handle imports
    final T message = messageService.createMessage(mMessage);
    try {
      if (!messageService.sendMessage(transaction, message, instance)) {
        instance.failTaskCreation(transaction, new MessagingException("Failure to send message"));
      }
    } catch (@NotNull final RuntimeException e) {
      instance.failTaskCreation(transaction, e);
      throw e;
    }

    return false;
  }

  /**
   * Take the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically taken.
   *
   * @return <code>false</code>
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> messageService, final U instance) {
    return false;
  }

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.
   *
   * @return <code>false</code>
   */
  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> messageService, final U instance) {
    return false;
  }

  @Override
  public <R> R visit(@NotNull final ProcessNode.Visitor<R> visitor) {
    return visitor.visitActivity(this);
  }


}
