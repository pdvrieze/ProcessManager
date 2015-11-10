package net.devrieze.util.db;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;


public abstract class JSONObject {


  public static class JSONString extends JSONObject {

    @NotNull
    private final CharSequence aString;

    public JSONString(@NotNull final CharSequence string) {
      aString = string;
    }

    @Override
    @NotNull
    public CharSequence getValue() {
      return aString;
    }

    @Override
    @NotNull
    public StringBuilder appendTo(@NotNull final StringBuilder stringBuilder) {
      final int len = aString.length();
      stringBuilder.append('"');
      for (int i = 0; i < len; ++i) {
        final char c = aString.charAt(i);
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
    private final List<JSONObject> aItems;

    private JSONArray(@NotNull final List<JSONObject> items) {
      aItems = items;
    }

    @Override
    @NotNull
    public List<JSONObject> getValue() {
      return aItems;
    }

    @Override
    @NotNull
    public StringBuilder appendTo(@NotNull final StringBuilder stringBuilder) {
      stringBuilder.append('[');
      for (final Iterator<JSONObject> it = aItems.iterator(); it.hasNext();) {
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

  public static JSONArray jsonArray(final JSONObject... items) {
    return jsonArray(Arrays.asList(items));
  }

  public static JSONString jsonString(@NotNull final CharSequence string) {
    return new JSONString(string);
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
