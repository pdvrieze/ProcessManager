package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.bind.annotation.XmlValue;


/**
 * Created by pdvrieze on 10/12/15.
 */
public abstract class XmlHandle<T> implements Handle<T>, XmlSerializable, SimpleXmlDeserializable {

  @XmlValue
  private long mHandle;

  public XmlHandle(long handle) {
    mHandle = handle;
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    mHandle = Long.parseLong(elementText.toString());
    return true;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {
    // ignore
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlUtil.writeSimpleElement(out, getElementName(), Long.toString(mHandle));
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  public void setHandle(final long handle) {
    mHandle = handle;
  }
}
