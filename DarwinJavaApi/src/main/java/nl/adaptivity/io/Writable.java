package nl.adaptivity.io;

import java.io.IOException;
import java.io.Writer;


/**
 * A visitor interface for things that can write themselves to a writer.
 * Created by pdvrieze on 19/11/15.
 */
public interface Writable {
  void writeTo(Writer destination) throws IOException;

}
