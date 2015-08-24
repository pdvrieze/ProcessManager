package nl.adaptivity.util.xml;

import java.net.URI;

import javax.xml.bind.annotation.adapters.XmlAdapter;


public class JaxbUriAdapter extends XmlAdapter<String, URI> {

  @Override
  public URI unmarshal(final String pV) throws Exception {
    return URI.create(pV);
  }

  @Override
  public String marshal(final URI pV) throws Exception {
    return pV.toString();
  }

}
