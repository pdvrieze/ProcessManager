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

package nl.adaptivity.process.util;

import android.os.Parcel;
import android.os.Parcelable;
import net.devrieze.util.StringUtil;
import nl.adaptivity.xml.*;

import javax.xml.namespace.QName;


/**
 * Holder for task variables. Created by pdvrieze on 15/02/16.
 */
public abstract class ModifySequence implements CharSequence, XmlSerializable, Parcelable {

  public static class AttributeSequence extends ModifySequence {

    public static final Creator<AttributeSequence> CREATOR = new Creator<AttributeSequence>() {
      @Override
      public AttributeSequence createFromParcel(final Parcel source) {
        return new AttributeSequence(source);
      }

      @Override
      public AttributeSequence[] newArray(final int size) {
        return new AttributeSequence[size];
      }
    };

    public static final QName ELEMENTNAME = new QName(Constants.MODIFY_NS_STR, "attribute", Constants.MODIFY_NS_PREFIX);
    private final CharSequence mParamName;
    private final CharSequence mDefineName;
    private final CharSequence mXpath;

    // Object Initialization

    public AttributeSequence(final Parcel source) {
      mParamName = readCharSequence(source);
      mDefineName = readCharSequence(source);
      mXpath = readCharSequence(source);
    }

    public AttributeSequence(final CharSequence paramName, final CharSequence defineName, final CharSequence xpath) {
      mParamName = paramName;
      mDefineName = defineName;
      mXpath = xpath;
    }
// Object Initialization end


    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      writeCharSequence(dest, mParamName);
      writeCharSequence(dest, mDefineName);
      writeCharSequence(dest, mXpath);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      final QName elementName = ELEMENTNAME;
      XmlWriterUtil.smartStartTag(out, elementName);
      XmlWriterUtil.writeAttribute(out, "value", mDefineName);
      XmlWriterUtil.writeAttribute(out, "name", mParamName);
      XmlWriterUtil.writeAttribute(out, "xpath", mXpath);
      XmlWriterUtil.endTag(out, elementName);
    }

    @Override
    public int length() {
      if (mDefineName==null) {
        return 4;
      }
      int len = 3 + mDefineName.length();

      if (mXpath!=null && mXpath.length()>1 && (!StringUtil.isEqual(".", mXpath))) {
        len+= 2 + mXpath.length();
      }
      return len;
    }

    @Override
    public char charAt(final int index) {
      if (mDefineName == null) { return "null".charAt(index); }
      if (index<2) {
        return "${".charAt(index);
      }
      int offset = 2;

      if (index-offset<mDefineName.length()) {
        return mDefineName.charAt(index-offset);
      }
      offset += mDefineName.length();

      if (mXpath!=null && mXpath.length()>1 && (!StringUtil.isEqual(".", mXpath))) {
        if (index==offset) { return '['; }
        offset++;
        if (index-offset<mXpath.length()) { return mXpath.charAt(index-offset); }
        offset+=mXpath.length();
        if (index==offset) { return ']'; }
        offset++;
      }
      if (index==offset) { return '}'; }
      throw new IndexOutOfBoundsException();
    }



    // Property accessors start
    public CharSequence getParamName() {
      return mParamName;
    }

    public CharSequence getVariableName() {
      return mDefineName;
    }
    public CharSequence getXpath() {
      return mXpath;
    }
    // Property acccessors end

  }

  public static class FragmentSequence extends ModifySequence {

    public static final Creator<FragmentSequence> CREATOR = new Creator<FragmentSequence>() {
      @Override
      public FragmentSequence createFromParcel(final Parcel source) {
        return new FragmentSequence(source);
      }

      @Override
      public FragmentSequence[] newArray(final int size) {
        return new FragmentSequence[size];
      }
    };

    private final String mElementName;
    private final CharSequence mVariableName;
    private final CharSequence mXpath;
    private final String mRefNodeId;

    // Object Initialization

    public FragmentSequence(final Parcel source) {
      mRefNodeId = source.readString();
      mElementName = source.readString();
      mVariableName = readCharSequence(source);
      mXpath = readCharSequence(source);

    }

    public FragmentSequence(final String elementName, final CharSequence variableName, final CharSequence xpath) {
      this(null, elementName, variableName, xpath);
    }

    public FragmentSequence(final String refNodeId, final String elementName, final CharSequence variableName, final CharSequence xpath) {
      mRefNodeId = refNodeId;
      mElementName = elementName;
      mVariableName = variableName;
      mXpath = xpath;
    }
    // Object Initialization end

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      final QName elementName = new QName(Constants.MODIFY_NS_STR, mElementName, Constants.MODIFY_NS_PREFIX);
      XmlWriterUtil.smartStartTag(out, elementName);
      XmlWriterUtil.writeAttribute(out, "refNode", mRefNodeId);
      XmlWriterUtil.writeAttribute(out, "value", mVariableName);
      XmlWriterUtil.writeAttribute(out, "xpath", mXpath);
      XmlWriterUtil.endTag(out, elementName);
    }

    @Override
    public int length() {
      if (mVariableName == null) {
        return 4;
      }
      int len = 3 + mVariableName.length();

      if (mXpath!=null && mXpath.length()>1 && (!StringUtil.isEqual(".", mXpath))) {
        len+= 2 + mXpath.length();
      }
      return len;
    }

    @Override
    public char charAt(final int index) {
      if (mVariableName == null) { return "null".charAt(index); }
      if (index<2) {
        return "${".charAt(index);
      }
      int offset = 2;

      if (index-offset < mVariableName.length()) {
        return mVariableName.charAt(index - offset);
      }
      offset += mVariableName.length();

      if (mXpath!=null && mXpath.length()>1 && (!StringUtil.isEqual(".", mXpath))) {
        if (index==offset) { return '['; }
        offset++;
        if (index-offset<mXpath.length()) { return mXpath.charAt(index-offset); }
        offset+=mXpath.length();
        if (index==offset) { return ']'; }
        offset++;
      }
      if (index==offset) { return '}'; }
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeString(mRefNodeId);
      dest.writeString(mElementName);
      writeCharSequence(dest, mVariableName);
      writeCharSequence(dest, mXpath);
    }

    // Property accessors start
    public CharSequence getVariableName() {
      return mVariableName;
    }

    @Override
    public CharSequence getXpath() {
      return mXpath;
    }
    // Property acccessors end

  }

  @Override
  public int length() {
    return 0;
  }

  @Override
  public char charAt(final int index) {
    return 0;
  }

  @Override
  public CharSequence subSequence(final int start, final int end) {
    return toString().subSequence(start, end);
  }

  @Override
  public String toString() {
    final int len = length();
    final StringBuilder stringBuilder = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      stringBuilder.append(charAt(i));
    }
    return stringBuilder.toString();
  }

  private static void writeCharSequence(final Parcel dest, final CharSequence sequence) {
    if (sequence==null) {
      dest.writeByte((byte) 2);
    } else if (sequence instanceof Parcelable) {
      dest.writeByte((byte) 1);
      dest.writeParcelable((Parcelable) sequence, 0);
    } else {
      dest.writeByte((byte) 0);
      dest.writeString(sequence.toString());
    }
  }

  private static CharSequence readCharSequence(final Parcel source) {
    final byte b = source.readByte();
    if (b==2) {
      return null;
    } else if (b==0) {
      return source.readString();
    } else {
      return source.readParcelable(ModifySequence.class.getClassLoader());
    }
  }

  // Property accessors start
  public abstract CharSequence getVariableName();

  public abstract CharSequence getXpath();
  // Property accesors end

  public static FragmentSequence newFragmentSequence(final String elementName, final CharSequence defineName, final CharSequence xpath) {
    return new FragmentSequence(elementName, defineName, xpath);
  }

  public static AttributeSequence newAttributeSequence(final CharSequence paramName, final CharSequence defineName, final CharSequence xpath) {
    return new AttributeSequence(paramName, defineName, xpath);
  }
}
