package nl.adaptivity.android.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.ByteArrayBuffer;

import android.util.Pair;


public class MultipartEntity extends AbstractHttpEntity {

  private static final String MIMETYPE_BASE = "multipart/formdata";
  private static final String CRLF="\r\n";
  private static final String CONTENT_DISPOSITION = "Content-Disposition: form-data; name=";

  private List<Pair<String, HttpEntity>> aContent = new ArrayList<>();

  private String mBoundary;

  public MultipartEntity() {
    mBoundary = generateBoundary();
  }

  private static String generateBoundary() {
    Random rnd = new Random();
    return "--"+MultipartEntity.class.getSimpleName()+'-'+Long.toHexString(rnd.nextLong());
  }

  @Override
  public InputStream getContent() throws IOException, IllegalStateException {
    // TODO do this in a less memory intensive way
    long cl = getContentLength();
    ByteArrayOutputStream bos = cl<0 ? new ByteArrayOutputStream() :  new ByteArrayOutputStream((int) cl);
    writeTo(bos);
    return new ByteArrayInputStream(bos.toByteArray());
  }

  private void ensureBoundary() {
    // TODO do nothing for now. Should verify the viability of the boundary
  }

  @Override
  public Header getContentType() {
    return new BasicHeader("Content-Type", MIMETYPE_BASE+"; boundary="+mBoundary.substring(2));
  }

  @Override
  public void setContentType(Header contentType) {
    setContentType(contentType.getValue());
  }

  @Override
  public void setContentType(String contentType) {
    if (contentType.startsWith(MIMETYPE_BASE)) {
      int i = contentType.indexOf(';', MIMETYPE_BASE.length())+1;
      while (i>=0) {
        int j = contentType.indexOf(';', i);
        String param;
        if (j>=0) {
          param = contentType.substring(i).trim();
        } else {
          param = contentType.substring(i, j).trim();
        }
        int e = param.indexOf('=');
        if (e>=0) {
          String parName = param.substring(0, e).trim();
          String parValue = param.substring(e+1).trim();
          if ("boundary".equals(parName)) {
            if (parValue.startsWith("--")) {
              mBoundary = parValue;
            } else {
              mBoundary = "--"+parValue;
            }
          }
        }
        i=j+1;
      }
    } else {
      throw new UnsupportedOperationException("The type must be multipart/formdata");
    }
  }

  @Override
  public long getContentLength() {
    if (aContent.isEmpty()) { return 0; }
    long result = -2; // First boundary does not include a CRLF
    for (Pair<String, HttpEntity> content: aContent) {
      final long cl = getContentLength(content.first, content.second);
      if (cl<0) { return cl; } // we don't know if a child doesn't know
      result+=cl;
    }
    result+=6+mBoundary.length();
    return result;
  }

  private long getContentLength(String name, HttpEntity entity) {
    final long el = entity.getContentLength();
    if (el>=0) {
      return getBoundary(name, entity).length()+el;
    } else {
      return el;
    }
  }

  private String getBoundary(String name, HttpEntity entity) {
    StringBuilder result = new StringBuilder();
    result.append(CRLF).append(mBoundary).append(CRLF);
    result.append(CONTENT_DISPOSITION).append(name).append(CRLF);
    {
      Header ct = entity.getContentType();
      if (ct!=null) {
        result.append(ct.getName()).append(": ").append(ct.getValue()).append(CRLF);
      }
    }
    {
      Header ce = entity.getContentEncoding();
      if (ce!=null) {
        result.append(ce.getName()).append(": ").append(ce.getValue()).append(CRLF);
      }
    }
    long cl = entity.getContentLength();
    if (cl>=0) {
      result.append("Content-Length: ").append(cl).append(CRLF);
    }
    result.append(CRLF);
    return result.toString();
  }

  @Override
  public boolean isRepeatable() {
    for(Pair<String, HttpEntity> elem:aContent) {
      if (!elem.second.isRepeatable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isStreaming() {
    for(Pair<String, HttpEntity> elem:aContent) {
      if (elem.second.isStreaming()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void writeTo(OutputStream outstream) throws IOException {
    ensureBoundary();
    Charset cs = Charset.forName("UTF8");
    int skip = 2;
    for(Pair<String, HttpEntity> elem:aContent) {
      final ByteBuffer boundary = cs.encode(getBoundary(elem.first, elem.second));
      int pos = boundary.position();
      if (pos>0) {
        boundary.flip();
      }
      outstream.write(boundary.array(), boundary.arrayOffset()+skip, boundary.limit()-skip);
      elem.second.writeTo(outstream);
      skip = 0;
    }
    final ByteBuffer boundary = cs.encode(CRLF+mBoundary+"--"+CRLF);
    outstream.write(boundary.array(), boundary.arrayOffset(), boundary.limit());
    outstream.flush();
  }

  public void add(String name, HttpEntity childEntity) {
    aContent.add(new Pair<>(name, childEntity));
  }

}
