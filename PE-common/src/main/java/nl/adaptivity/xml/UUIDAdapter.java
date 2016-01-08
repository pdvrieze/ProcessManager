package nl.adaptivity.xml;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Simple JAXB adapter that maps UUID's to strings and reverse
 * @author Paul de Vrieze
 *
 */
public class UUIDAdapter extends XmlAdapter<String, UUID> {

  @Override
  public UUID unmarshal(final String v) throws Exception {
    return UUID.fromString(v);
  }

  @Override
  public String marshal(@NotNull final UUID v) throws Exception {
    return v.toString();
  }

}
