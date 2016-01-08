package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

import javax.xml.bind.annotation.adapters.XmlAdapter;


public class JaxbUriAdapter extends XmlAdapter<String, URI> {

  @Override
  public URI unmarshal(@NotNull final String v) throws Exception {
    return URI.create(v);
  }

  @Override
  public String marshal(@NotNull final URI v) throws Exception {
    return v.toString();
  }

}
