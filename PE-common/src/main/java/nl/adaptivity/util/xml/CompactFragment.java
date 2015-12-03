package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
public class CompactFragment implements XmlSerializable {

  private final SimpleNamespaceContext namespaces;
  private final char[] content;

  public CompactFragment(final Iterable<Namespace> namespaces, final char[] content) {
    this.namespaces = SimpleNamespaceContext.from(namespaces);
    this.content = content;
  }

  /** Convenience constructor for content without namespaces. */
  public CompactFragment(@NotNull final String string) {
    this(Collections.<Namespace>emptyList(), string.toCharArray());
  }

  public CompactFragment(final CompactFragment orig) {
    namespaces = SimpleNamespaceContext.from(orig.namespaces);
    content = orig.content;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlReader in = XmlUtil.filterSubstream(XMLFragmentStreamReader.from(this));
    XmlUtil.serialize(in, out);
    in.close();
  }

  public SimpleNamespaceContext getNamespaces() {
    return namespaces;
  }

  public char[] getContent() {
    return content;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final CompactFragment that = (CompactFragment) o;

    if (!namespaces.equals(that.namespaces)) return false;
    return Arrays.equals(content, that.content);

  }

  @Override
  public int hashCode() {
    int result = namespaces.hashCode();
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @NotNull
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CompactFragment{");
    sb.append("namespaces=[");
    {
      final int nsCount = namespaces.size();
      for(int i = 0; i< nsCount; ++i) {
        if (i>0) { sb.append(", "); }
        sb.append(namespaces.getPrefix(i)).append(" -> ").append(namespaces.getNamespaceURI(i));
      }
    }
    sb.append(']');
    if (content!=null) {
      sb.append(", content=").append(new String(content));
    }
    sb.append('}');
    return sb.toString();
  }

  @NotNull
  public String getContentString() {
    return new String(getContent());
  }
}
