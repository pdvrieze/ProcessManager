package nl.adaptivity.xml;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Simple JAXB adapter that maps UUID's to strings and reverse
 * @author Paul de Vrieze
 *
 */
public class UUIDAdapter extends XmlAdapter<String, UUID> {

  @Override
  public UUID unmarshal(String pV) throws Exception {
    return UUID.fromString(pV);
  }

  @Override
  public String marshal(UUID pV) throws Exception {
    return pV.toString();
  }

}
