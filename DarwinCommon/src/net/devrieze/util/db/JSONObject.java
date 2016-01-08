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

package net.devrieze.util.db;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public abstract class JSONObject {


  public static class JSONString extends JSONObject {

    @NotNull
    private final CharSequence mValue;

    public JSONString(@NotNull final CharSequence value) {
      mValue = value;
    }

    @Override
    @NotNull
    public CharSequence getValue() {
      return mValue;
    }

    @Override
    @NotNull
    public StringBuilder appendTo(@NotNull final StringBuilder stringBuilder) {
      final int len = mValue.length();
      stringBuilder.append('"');
      for (int i = 0; i < len; ++i) {
        final char c = mValue.charAt(i);
        switch (c) {
          case '"':
            stringBuilder.append("\\\"");
            break;
          case '\\':
            stringBuilder.append("\\\\");
            break;
          case '\b':
            stringBuilder.append("\\b");
            break;
          case '\f':
            stringBuilder.append("\\f");
            break;
          case '\n':
            stringBuilder.append("\\n");
            break;
          case '\r':
            stringBuilder.append("\\r");
            break;
          case '\t':
            stringBuilder.append("\\t");
            break;
          default:
            stringBuilder.append(c);
        }
      }
      stringBuilder.append('"');
      return stringBuilder;
    }

  }

  public static class JSONArray extends JSONObject {

    @NotNull
    private final List<JSONObject> mItems;

    private JSONArray(@NotNull final List<JSONObject> items) {
      mItems = items;
    }

    @Override
    @NotNull
    public List<JSONObject> getValue() {
      return mItems;
    }

    @Override
    @NotNull
    public StringBuilder appendTo(@NotNull final StringBuilder stringBuilder) {
      stringBuilder.append('[');
      for (final Iterator<JSONObject> it = mItems.iterator(); it.hasNext();) {
        it.next().appendTo(stringBuilder);
        if (it.hasNext()) {
          stringBuilder.append(',');
        }
      }
      stringBuilder.append(']');
      return stringBuilder;
    }

  }

  public static JSONArray jsonArray(@NotNull final List<JSONObject> items) {
    return new JSONArray(items);
  }

  public static JSONArray jsonArray(@NotNull final JSONObject... items) {
    return jsonArray(Arrays.asList(items));
  }

  public static JSONString jsonString(@NotNull final CharSequence value) {
    return new JSONString(value);
  }

  public abstract Object getValue();

  @NotNull
  public abstract StringBuilder appendTo(@NotNull StringBuilder stringBuilder);

  @Override
  @NotNull
  public String toString() {
    return appendTo(new StringBuilder()).toString();
  }

}
