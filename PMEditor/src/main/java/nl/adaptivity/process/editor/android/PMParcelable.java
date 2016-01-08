/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.editor.android;

import android.os.Parcel;
import android.os.Parcelable;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class PMParcelable implements Parcelable {

  public static final Parcelable.Creator<PMParcelable> CREATOR = new Creator<PMParcelable>() {

    @Override
    public PMParcelable[] newArray(int size) {
      return new PMParcelable[size];
    }

    @Override
    public PMParcelable createFromParcel(Parcel source) {
      return new PMParcelable(source);
    }
  };
  private ClientProcessModel<?, ?> mProcessModel;

  public PMParcelable(Parcel source) {
    this(PMParser.parseProcessModelFallback(readInputStream(source), PMEditor.NULL_LAYOUT_ALGORITHM, new LayoutAlgorithm<DrawableProcessNode>()));
  }

  public PMParcelable(ClientProcessModel<?, ?> processModel) {
    mProcessModel = processModel;
  }

  private static InputStream readInputStream(Parcel source) {
    int len = source.readInt();
    if (len>0) {
      byte buf[] = new byte[len];
      source.readByteArray(buf);
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
  public void writeToParcel(Parcel dest, int flags) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      if (mProcessModel!=null) {
        PMParser.serializeProcessModel(out, mProcessModel);
      } else {
        dest.writeInt(0);
      }
    } catch (XmlException | XmlPullParserException | IOException e) {
      dest.writeInt(0);
      throw new RuntimeException(e);
    }
    dest.writeInt(out.size());
    if (out.size()>0) {
      dest.writeByteArray(out.toByteArray());
    }
  }

  public ClientProcessModel<?, ?> getProcessModel() {
    return mProcessModel;
  }

}
