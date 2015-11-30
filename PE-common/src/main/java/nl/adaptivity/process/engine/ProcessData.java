package nl.adaptivity.process.engine;

import net.devrieze.util.Named;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import java.util.List;


/** Class to represent data attached to process instances. */
@XmlJavaTypeAdapter(XmlSerializable.JAXBAdapter.class)
public class ProcessData implements Named, ExtXmlDeserializable, XmlSerializable {

  public static final String ELEMENTLOCALNAME = "value";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String mName;
  private CompactFragment mValue;

// Object Initialization

  /**
   * @deprecated Initialise with compact fragment instead.
   * @see #ProcessData(String, CompactFragment)
   */
  @Deprecated
  public ProcessData(final String name, final Node value) throws XmlException {
    this(name, toCompactFragment(value));
  }

  public ProcessData(final String name, final CompactFragment value) {
    mName = name;
    mValue = value;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public ProcessData(final String name, @Nullable final NodeList value) throws XmlException {
    this(name, (value==null || value.getLength()<=1)? toNode(value) : XmlUtil.toDocFragment(value));
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public ProcessData(final String name, final List<Node> value) throws XmlException {
    this(name, XmlUtil.toDocFragment(value));
  }

  private ProcessData() {}

  // Object Initialization end
  public static ProcessData deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new ProcessData(), in);
  }

  @Override
  public void deserializeChildren(final XmlReader in) throws XmlException {
    mValue = XmlUtil.siblingsToFragment(in);
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (StringUtil.isEqual("name", attributeLocalName)) {
      mName=attributeValue.toString();
      return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {
    // nothing
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    XmlUtil.writeAttribute(out, "name", mName);
    mValue.serialize(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  public DocumentFragment getContentFragment() throws XmlException {
    return XmlUtil.childrenToDocumentFragment(getContentStream());
  }

  @NotNull
  private static CompactFragment toCompactFragment(final Node value) throws XmlException {
    return XmlUtil.nodeToFragment(value);
  }


  @Nullable
  private static Node toNode(@Nullable final NodeList value) {
    if (value==null|| value.getLength()==0) { return null; }
    assert value.getLength()==1;
    return value.item(0);
  }


  @NotNull
  @Override
  public Named newWithName(final String name) {
    return new ProcessData(name, mValue);
  }

  @Override
  public String getName() {
    return mName;
  }

  public CompactFragment getContent() {
    return mValue;
  }

  @NotNull
  public XmlReader getContentStream() throws XmlException {
    return XMLFragmentStreamReader.from(getContent());
  }

  @SuppressWarnings("Duplicates")
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mName == null) ? 0 : mName.hashCode());
    result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
    return result;
  }

  @SuppressWarnings("Duplicates")
  @Override
  public boolean equals(@Nullable final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ProcessData other = (ProcessData) obj;
    if (mName == null) {
      if (other.mName != null)
        return false;
    } else if (!mName.equals(other.mName))
      return false;
    if (mValue == null) {
      if (other.mValue != null)
        return false;
    } else if (!mValue.equals(other.mValue))
      return false;
    return true;
  }

}
