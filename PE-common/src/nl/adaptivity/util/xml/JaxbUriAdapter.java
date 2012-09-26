package nl.adaptivity.util.xml;

import java.net.URI;

import javax.xml.bind.annotation.adapters.XmlAdapter;


public class JaxbUriAdapter extends XmlAdapter<String, URI> {

  @Override
  public URI unmarshal(String pV) throws Exception {
    return URI.create(pV);
  }

  @Override
  public String marshal(URI pV) throws Exception {
    return pV.toString();
  }

}
