package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@XmlDeserializer(StartNodeImpl.Factory.class)
@XmlRootElement(name = StartNodeImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
public class StartNodeImpl extends ProcessNodeImpl implements StartNode<ProcessNodeImpl>, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public StartNodeImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
      return StartNodeImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static StartNodeImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XMLStreamReader in) throws XMLStreamException {
    return XmlUtil.deserializeHelper(new StartNodeImpl(ownerModel), in);
  }

  private static final long serialVersionUID = 7779338146413772452L;

  public static final String ELEMENTLOCALNAME = "start";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private List<XmlResultType> aImports;

  public StartNodeImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel, Collections.<Identifiable>emptyList());
  }

  public StartNodeImpl(final ProcessModelImpl ownerModel, final List<XmlResultType> imports) {
    super(ownerModel, Collections.<Identifiable>emptyList());
    aImports = imports;
  }

  @Override
  public boolean deserializeChild(@NotNull final XMLStreamReader in) throws XMLStreamException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI())) {
      switch (in.getLocalName()) {
        case "import":
          getResults().add(XmlResultType.deserialize(in)); return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final String elementText) {
    return false;
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  protected void serializeChildren(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeChildren(out, aImports);
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.StartNode#getImports()
   */
  @Override
  @XmlElement(name = "import")
  public List<XmlResultType> getResults() {
    if (aImports == null) {
      aImports = new ArrayList<>();
    }
    return this.aImports;
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
