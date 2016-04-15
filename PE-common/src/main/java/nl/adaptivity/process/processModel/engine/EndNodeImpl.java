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
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlDeserializer(EndNodeImpl.Factory.class)
@XmlRootElement(name = EndNode.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
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
    return XmlUtil.deserializeHelper(new EndNodeImpl(ownerModel), in);
  }

  public EndNodeImpl(final ProcessModelImpl ownerModel, final ExecutableProcessNode previous) {
    super(ownerModel);
    setPredecessor(previous);
  }

  public EndNodeImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
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
    //    pProcessInstance.finish();
    return true;
  }

}
