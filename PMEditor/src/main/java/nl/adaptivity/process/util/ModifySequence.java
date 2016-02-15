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

import net.devrieze.lang.Const;
import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;


/**
 * Holder for task variables. Created by pdvrieze on 15/02/16.
 */
public abstract class ModifySequence implements CharSequence, XmlSerializable {

  public static class AttributeSequence extends ModifySequence {

    public static final QName ELEMENTNAME = new QName(Constants.MODIFY_NS_STR, "attribute", Constants.MODIFY_NS_PREFIX);
    private final CharSequence mParamName;
    private final CharSequence mDefineName;
    private final CharSequence mXpath;

    // Object Initialization
    public AttributeSequence(final CharSequence paramName, final CharSequence defineName, final CharSequence xpath) {
      mParamName = paramName;
      mDefineName = defineName;
      mXpath = xpath;
    }
// Object Initialization end

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      final QName elementName = ELEMENTNAME;
      XmlUtil.writeStartElement(out, elementName);
      XmlUtil.writeAttribute(out, "value", mDefineName);
      XmlUtil.writeAttribute(out, "name", mParamName);
      XmlUtil.writeAttribute(out, "xpath", mXpath);
      XmlUtil.writeEndElement(out, elementName);
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

    public CharSequence getDefineName() {
      return mDefineName;
    }
    public CharSequence getXpath() {
      return mXpath;
    }
    // Property acccessors end

  }

  public static class FragmentSequence extends ModifySequence {

    private final String mElementName;
    private final CharSequence mDefineName;
    private final CharSequence mXpath;

    // Object Initialization
    public FragmentSequence(final String elementName, final CharSequence defineName, final CharSequence xpath) {
      mElementName = elementName;
      mDefineName = defineName;
      mXpath = xpath;
    }
    // Object Initialization end

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      final QName elementName = new QName(Constants.MODIFY_NS_STR, mElementName, Constants.MODIFY_NS_PREFIX);
      XmlUtil.writeStartElement(out, elementName);
      XmlUtil.writeAttribute(out, "value", mDefineName);
      XmlUtil.writeAttribute(out, "xpath", mXpath);
      XmlUtil.writeEndElement(out, elementName);
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
    @Override
    public CharSequence getDefineName() {
      return mDefineName;
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

  // Property accessors start
  public abstract CharSequence getDefineName();

  public abstract CharSequence getXpath();
  // Property accesors end

  public static FragmentSequence newFragmentSequence(final String elementName, final CharSequence defineName, final CharSequence xpath) {
    return new FragmentSequence(elementName, defineName, xpath);
  }

  public static AttributeSequence newAttributeSequence(final CharSequence paramName, final CharSequence defineName, final CharSequence xpath) {
    return new AttributeSequence(paramName, defineName, xpath);
  }
}
