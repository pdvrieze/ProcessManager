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

package nl.adaptivity.process.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.util.ModifySequence.FragmentSequence;
import org.jetbrains.annotations.Contract;


/**
 * Created by pdvrieze on 16/02/16.
 */
public abstract class VariableReference implements Parcelable, Comparable<VariableReference> {

  public static class ResultReference extends VariableReference {

    public static final Creator<ResultReference> CREATOR = new Creator<ResultReference>() {
      @Override
      public ResultReference createFromParcel(final Parcel source) {
        return new ResultReference(source);
      }

      @Override
      public ResultReference[] newArray(final int size) {
        return new ResultReference[size];
      }
    };

    private final String mTagName;

    public ResultReference(Identifiable node, IXmlResultType result) {
      this("value", node.getId(), result.getName(), null);
    }

    public ResultReference(final String tagName, final String nodeId, final String variableName, final String xpath) {
      super(nodeId, variableName, xpath);
      mTagName = tagName;
    }

    public ResultReference(Parcel source) {
      this(source.readString(), source.readString(), source.readString(), source.readString());
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeString(mTagName);
      dest.writeString(mNodeId);
      dest.writeString(mVariableName);
      dest.writeString(mXPath);
    }

    @Override
    public FragmentSequence toModifySequence() {
      return ModifySequence.newFragmentSequence("value", getVariableName(), getXPath());
    }

    @NonNull
    @Override
    @Contract(pure=true)
    public String toString() {
      final StringBuilder builder = new StringBuilder(4+mNodeId.length()+mVariableName.length());
      builder.append("#{").append(mNodeId).append('.').append(mVariableName).append('}');
      return builder.toString();
    }

    @Override
    public CharSequence getLabel() {
      final StringBuilder builder = new StringBuilder(4+mNodeId.length()+mVariableName.length());
      builder.append('#').append(mNodeId).append('.').append(mVariableName);
      return builder.toString();
    }
  }

  public static class DefineReference extends VariableReference {

    public static final Creator<DefineReference> CREATOR = new Creator<DefineReference>() {
      @Override
      public DefineReference createFromParcel(final Parcel source) {
        return new DefineReference(source);
      }

      @Override
      public DefineReference[] newArray(final int size) {
        return new DefineReference[size];
      }
    };

    public DefineReference(IXmlDefineType define) {
      this(define.getName(), define.getPath());
    }

    public DefineReference(final String defineName, final String xpath) {
      super(null, defineName, xpath);
    }

    public DefineReference(Parcel source) {
      this(source.readString(), source.readString());
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeString(mVariableName);
    }

    @Override
    public FragmentSequence toModifySequence() {
      return ModifySequence.newFragmentSequence("value", getVariableName(), getXPath());
    }

    @NonNull
    @Override
    @Contract(pure=true)
    public String toString() {
      final StringBuilder builder = new StringBuilder(8+mVariableName.length());
      builder.append("#{this.").append(mVariableName).append('}');
      return builder.toString();
    }

    @NonNull
    @Override
    @Contract(pure=true)
    public CharSequence getLabel() {
      return mVariableName != null ? mVariableName : "$self";
    }

  }

  protected final String mNodeId;
  protected final String mVariableName;
  protected final String mXPath;

  public VariableReference(final String nodeId, final String variableName, final String xpath) {
    mNodeId = nodeId;
    mVariableName = variableName;
    mXPath = xpath;
  }

  public abstract ModifySequence toModifySequence();

  /**
   * Get a label representation for this variable. This is for use when the use as variable is clear (no extra braces etc.
   * will be given)
   * @return The label.
   */
  public abstract CharSequence getLabel();

  public String getNodeId() {
    return mNodeId;
  }

  public String getVariableName() {
    return mVariableName;
  }

  public String getXPath() {
    return mXPath;
  }

  @Override
  public int compareTo(final VariableReference another) {
    String nodeId = mNodeId==null ? "this" : mNodeId;
    String otherNodeId = another.mNodeId==null ? "this" : another.mNodeId;
    int cmp = nodeId.compareTo(otherNodeId);
    if (cmp!=0) { return cmp; }
    return mVariableName.compareTo(another.mVariableName);
  }

  public static ResultReference newResultReference(final Identifiable node, final IXmlResultType result) {
    return new ResultReference(node, result);
  }

  public static ResultReference newResultReference(final String node, final String resultName, final String xpath) {return newResultReference("value", node, resultName, xpath);}

  public static ResultReference newResultReference(final String tagName, final String nodeId, final String variableName, final String xpath) {
    return new ResultReference(tagName, nodeId, variableName, xpath);
  }

  public static DefineReference newDefineReference(final IXmlDefineType define) {
    return new DefineReference(define);
  }

  public static DefineReference newDefineReference(final String node, final String xpath) {
    return new DefineReference(node, xpath);
  }
}
