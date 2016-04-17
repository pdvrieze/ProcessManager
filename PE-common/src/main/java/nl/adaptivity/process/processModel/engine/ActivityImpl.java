/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.ActivityBase;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;


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
public class ActivityImpl extends ActivityBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory<ActivityImpl> {

    @NotNull
    @Override
    public ActivityImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return ActivityImpl.deserialize(null, in);
    }
  }

  @Nullable protected ConditionImpl mCondition;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param predecessor The process node that starts immediately precedes this
   *          activity.
   */
  public ActivityImpl(final ProcessModelImpl  ownerModel, final ExecutableProcessNode predecessor) {
    super(ownerModel);
    setPredecessor(predecessor);
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public ActivityImpl(final ProcessModelImpl  ownerModel) {super(ownerModel);}

  public ActivityImpl(final Activity<?, ?> orig) {
    super(orig);
  }

  @Override
  protected void serializeCondition(final XmlWriter out) throws XmlException {
    XmlUtil.writeChild(out, mCondition);
  }

  @Override
  protected void deserializeCondition(final XmlReader in) throws XmlException {
    mCondition = ConditionImpl.deserialize(in);
  }

  /* (non-Javadoc)
         * @see nl.adaptivity.process.processModel.IActivity#getCondition()
         */
  @Nullable
  @Override
  @XmlName(Condition.ELEMENTLOCALNAME)
  public String getCondition() {
    return mCondition == null ? null : mCondition.toString();
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IActivity#setCondition(java.lang.String)
       */
  @Override
  public void setCondition(@Nullable final String condition) {
    mCondition = condition == null ? null : new ConditionImpl(condition);
    notifyChange();
  }

  @NotNull
  public static ActivityImpl deserialize(final ProcessModelImpl  ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new ActivityImpl(ownerModel), in);
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
    final T message = messageService.createMessage(getMessage());
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


}
