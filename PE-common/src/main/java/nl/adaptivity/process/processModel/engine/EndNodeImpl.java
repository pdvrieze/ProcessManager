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
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.EndNodeBase;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;


@XmlDeserializer(EndNodeImpl.Factory.class)
public class EndNodeImpl extends EndNodeBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory<EndNodeImpl> {

    @NotNull
    @Override
    public EndNodeImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return EndNodeImpl.deserialize(null, in);
    }
  }

  public EndNodeImpl(final EndNode<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static EndNodeImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.process.processModel.engine.EndNodeImpl>deserializeHelper(new EndNodeImpl(ownerModel), in);
  }

  public EndNodeImpl(final ProcessModelImpl ownerModel, final ExecutableProcessNode previous) {
    super(ownerModel);
    setPredecessor(previous);
  }

  public EndNodeImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
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
