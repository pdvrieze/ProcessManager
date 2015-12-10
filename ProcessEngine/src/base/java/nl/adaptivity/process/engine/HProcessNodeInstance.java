package nl.adaptivity.process.engine;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

@XmlDeserializer(HProcessNodeInstance.Factory.class)
public final class HProcessNodeInstance extends XmlHandle<ProcessNodeInstance> {

  public static class Factory implements XmlDeserializerFactory<HProcessNodeInstance> {

    @Override
    public HProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
      return HProcessNodeInstance.deserialize(in);
    }
  }

  public static final java.lang.String ELEMENTLOCALNAME = "nodeInstanceHandle";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  @XmlValue
  private long mHandle;

  public HProcessNodeInstance() {
    super(-1);
  }

  public HProcessNodeInstance(final long handle) {
    super(handle);
  }

  private static HProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new HProcessNodeInstance(), in);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj == this) || ((obj instanceof HProcessNodeInstance) && (getHandle() == ((HProcessNodeInstance) obj).getHandle()));
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  public void setHandle(final long handle) {
    mHandle = handle;
  }

}
