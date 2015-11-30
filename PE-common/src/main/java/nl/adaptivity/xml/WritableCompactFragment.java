package nl.adaptivity.xml;

import nl.adaptivity.io.Writable;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;


/**
 * Created by pdvrieze on 27/11/15.
 */
public class WritableCompactFragment extends CompactFragment implements Writable {

  public WritableCompactFragment(final Iterable<Namespace> namespaces, final char[] content) {
    super(namespaces, content);
  }

  public WritableCompactFragment(@NotNull final String string) {
    super(string);
  }

  public WritableCompactFragment(@NotNull final CompactFragment orig) {
    super(orig);
  }

  @Override
  public void writeTo(final Writer destination) throws IOException {
    destination.write(getContent());
  }
}
