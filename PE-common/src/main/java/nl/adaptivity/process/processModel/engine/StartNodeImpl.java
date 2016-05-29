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
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.StartNodeBase;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;


@XmlDeserializer(StartNodeImpl.Factory.class)
public class StartNodeImpl extends StartNodeBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class Factory implements XmlDeserializerFactory<StartNodeImpl> {

    @NotNull
    @Override
    public StartNodeImpl deserialize(@NotNull final XmlReader reader) throws XmlException {
      return StartNodeImpl.deserialize(null, reader);
    }
  }

  public StartNodeImpl(final StartNode<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static StartNodeImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.process.processModel.engine.StartNodeImpl>deserializeHelper(new StartNodeImpl(ownerModel), in);
  }

  public StartNodeImpl(final ProcessModelImpl  ownerModel) {
    super(ownerModel);
  }

  public StartNodeImpl(final ProcessModelImpl ownerModel, final List<XmlResultType> imports) {
    super(ownerModel);
    setResults(imports);
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
