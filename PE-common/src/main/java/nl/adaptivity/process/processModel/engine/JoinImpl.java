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
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.JoinBase;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(JoinImpl.Factory.class)
@XmlRootElement(name = Join.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends JoinBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public JoinImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return JoinImpl.deserialize(null, in);
    }
  }

  public JoinImpl(final Join<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static JoinImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new JoinImpl(ownerModel), in);
  }

  public JoinImpl(final ProcessModelImpl  ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors, max, min);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public JoinImpl(final ProcessModelImpl  ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static JoinImpl andJoin(final ProcessModelImpl ownerModel, final ExecutableProcessNode... predecessors) {
    return new JoinImpl(ownerModel, Arrays.asList(predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Deprecated
  @Nullable
  Set<? extends Identifiable> getXmlPrececessors() {
    if (getPredecessors()==null) { return null; }
    return getPredecessors();
  }

  @Deprecated
  void setXmlPrececessors(final List<? extends ExecutableProcessNode> pred) {
    swapPredecessors(pred);
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
