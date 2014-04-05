package nl.adaptivity.process.editor.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

import nl.adaptivity.process.diagram.DrawableProcessModel;
import android.os.Parcel;
import android.os.Parcelable;


public class PMParcelable implements Parcelable {
  
  public static final Parcelable.Creator<PMParcelable> CREATOR = new Creator<PMParcelable>() { 
    
    @Override
    public PMParcelable[] newArray(int pSize) {
      return new PMParcelable[pSize];
    }
    
    @Override
    public PMParcelable createFromParcel(Parcel pSource) {
      return new PMParcelable(pSource);
    }
  };
  private DrawableProcessModel mProcessModel;

  public PMParcelable(Parcel pSource) {
    this(PMParser.parseProcessModel(readInputStream(pSource), null));
  }

  public PMParcelable(DrawableProcessModel pProcessModel) {
    mProcessModel = pProcessModel;
  }

  private static InputStream readInputStream(Parcel pSource) {
    int len = pSource.readInt();
    if (len>0) {
      byte buf[] = new byte[len];
      pSource.readByteArray(buf);
      return new ByteArrayInputStream(buf);
    } else {
      return null;
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel pDest, int pFlags) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PMParser.serializeProcessModel(out, mProcessModel);
    } catch (XmlPullParserException | IOException e) {
      pDest.writeInt(0);
      throw new RuntimeException(e);
    }
    pDest.writeInt(out.size());
    if (out.size()>0) {
      pDest.writeByteArray(out.toByteArray());
    }
  }

  public DrawableProcessModel getProcessModel() {
    return mProcessModel;
  }
  
}
