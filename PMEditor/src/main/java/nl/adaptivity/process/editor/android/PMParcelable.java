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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.editor.android;

import android.os.Parcel;
import android.os.Parcelable;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class PMParcelable implements Parcelable {

  public static final Parcelable.Creator<PMParcelable> CREATOR = new Creator<PMParcelable>() {

    @Override
    public PMParcelable[] newArray(final int size) {
      return new PMParcelable[size];
    }

    @Override
    public PMParcelable createFromParcel(final Parcel source) {
      return new PMParcelable(source);
    }
  };
  private final RootDrawableProcessModel mProcessModel;

  public PMParcelable(final Parcel source) {
    this(PMParser.parseProcessModel(readInputStream(source), PMEditor.NULL_LAYOUT_ALGORITHM, new LayoutAlgorithm()));
  }

  public PMParcelable(final RootDrawableProcessModel processModel) {
    mProcessModel = processModel;
  }

  public PMParcelable(final RootDrawableProcessModel.Builder processModel) {
    this(processModel.build());
  }

  private static InputStream readInputStream(final Parcel source) {
    final int len = source.readInt();
    if (len>0) {
      final byte[] buf = new byte[len];
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
  public void writeToParcel(final Parcel dest, final int flags) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      if (mProcessModel!=null) {
        PMParser.serializeProcessModel(out, mProcessModel);
      } else {
        dest.writeInt(0);
      }
    } catch (XmlPullParserException | IOException e) {
      dest.writeInt(0);
      throw new RuntimeException(e);
    }
    dest.writeInt(out.size());
    if (out.size()>0) {
      dest.writeByteArray(out.toByteArray());
    }
  }

  public RootDrawableProcessModel getProcessModel() {
    return mProcessModel;
  }

}
