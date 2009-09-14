package nl.adaptivity.jbi.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;


public class UrlDataSource implements DataSource {
  
  private static MimetypesFileTypeMap _mimeMap;
  
  private final String aContentType;
  private final URL aURL;

  private final InputStream aInputStream;

  private final Map<String, List<String>> aHeaders;

  public UrlDataSource(URL pFinalUrl) throws IOException {
    URLConnection connection;
    connection = pFinalUrl.openConnection();
    aContentType = connection.getContentType();
    
    aInputStream = connection.getInputStream();

    aHeaders= connection.getHeaderFields();
    
    aURL = pFinalUrl;
  }

  @Override
  public String getContentType() {
    return aContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return aInputStream;
  }

  @Override
  public String getName() {
    return aURL.getPath();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not allowed");
    
  }

  private static String getMimeTypeForFileName(String pFileName) {
    if (_mimeMap==null) {
      _mimeMap = new MimetypesFileTypeMap();
    }
    return _mimeMap.getContentType(pFileName);
  }

  private static String fileName(String pPath) {
    int i = pPath.lastIndexOf('/');
    if (i<0) {
      return pPath;
    }
    return pPath.substring(i+1);
  }

  public Map<String, List<String>> getHeaders() {
    return aHeaders;
  }
  
}
