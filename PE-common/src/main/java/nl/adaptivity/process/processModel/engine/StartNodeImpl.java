package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import java.util.Collections;
import java.util.List;


@XmlDeserializer(StartNodeImpl.Factory.class)
@XmlRootElement(name = StartNode.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNodeImpl extends ProcessNodeImpl implements StartNode<ExecutableProcessNode>, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public StartNodeImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return StartNodeImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static StartNodeImpl deserialize(final ProcessModelBase<ExecutableProcessNode> ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new StartNodeImpl(ownerModel), in);
  }

  public StartNodeImpl(final ProcessModelBase<ExecutableProcessNode>  ownerModel) {
    super(ownerModel, Collections.<Identifiable>emptyList());
  }

  public StartNodeImpl(final ProcessModelBase<ExecutableProcessNode>  ownerModel, final List<XmlResultType> imports) {
    super(ownerModel, Collections.<Identifiable>emptyList());
    setResults(imports);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case "import":
          getResults().add(XmlResultType.deserialize(in)); return true;
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
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  @Override
  public int getMaxPredecessorCount() {
    return 0;
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

  @Override
  public <R> R visit(@NotNull final ProcessNode.Visitor<R> visitor) {
    return visitor.visitStartNode(this);
  }

}
