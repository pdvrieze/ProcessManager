package nl.adaptivity.process.engine;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.namespace.QName;

@XmlDeserializer(HProcessInstance.Factory.class)
public final class HProcessInstance extends XmlHandle {

  public static class Factory implements XmlDeserializerFactory<HProcessInstance> {

    @Override
    public HProcessInstance deserialize(final XmlReader in) throws XmlException {
      return HProcessInstance.deserialize(in);
    }
  }

  public static final java.lang.String ELEMENTLOCALNAME = "instanceHandle";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public HProcessInstance() {
    super(-1);
  }

  public HProcessInstance(final long handle) {
    super(handle);
  }

  private static HProcessInstance deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new HProcessInstance(), in);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj == this) || ((obj instanceof HProcessInstance) && (getHandle() == ((HProcessInstance) obj).getHandle()));
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

}
