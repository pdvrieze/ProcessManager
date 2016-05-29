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
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(JoinImpl.Factory.class)
public class JoinImpl extends JoinBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory<JoinImpl> {

    @NotNull
    @Override
    public JoinImpl deserialize(@NotNull final XmlReader reader) throws XmlException {
      return JoinImpl.deserialize(null, reader);
    }
  }

  public JoinImpl(final Join<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static JoinImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.process.processModel.engine.JoinImpl>deserializeHelper(new JoinImpl(ownerModel), in);
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
  public <T extends Transaction> boolean condition(final T transaction, final IProcessNodeInstance<T, ?> instance) {
    return true;
  }

  @Override
  public <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean provideTask(final T transaction, final IMessageService<V, T, U> messageService, final U instance) throws SQLException {
    return true;
  }

  @Override
  public <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean takeTask(final IMessageService<V, T, U> messageService, final U instance) {
    return true;
  }

  @Override
  public <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean startTask(final IMessageService<V, T, U> messageService, final U instance) {
    return true;
  }

}
