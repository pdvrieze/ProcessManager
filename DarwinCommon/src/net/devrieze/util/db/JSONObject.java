package net.devrieze.util.db;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static net.devrieze.util.Annotations.*;

import net.devrieze.annotations.NotNull;


public abstract class JSONObject {


  public static class JSONString extends JSONObject {

    @NotNull
    private final CharSequence aString;

    public JSONString(@NotNull final CharSequence pString) {
      aString = pString;
    }

    @Override
    @NotNull
    public CharSequence getValue() {
      return aString;
    }

    @Override
    @NotNull
    public StringBuilder appendTo(@NotNull final StringBuilder pStringBuilder) {
      final int len = aString.length();
      pStringBuilder.append('"');
      for (int i = 0; i < len; ++i) {
        final char c = aString.charAt(i);
        switch (c) {
          case '"':
            pStringBuilder.append("\\\"");
            break;
          case '\\':
            pStringBuilder.append("\\\\");
            break;
          case '\b':
            pStringBuilder.append("\\b");
            break;
          case '\f':
            pStringBuilder.append("\\f");
            break;
          case '\n':
            pStringBuilder.append("\\n");
            break;
          case '\r':
            pStringBuilder.append("\\r");
            break;
          case '\t':
            pStringBuilder.append("\\t");
            break;
          default:
            pStringBuilder.append(c);
        }
      }
      pStringBuilder.append('"');
      return pStringBuilder;
    }

  }

  public static class JSONArray extends JSONObject {

    @NotNull
    private final List<JSONObject> aItems;

    private JSONArray(@NotNull final List<JSONObject> pItems) {
      aItems = pItems;
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
    return jsonArray(notNull(Arrays.asList(items)));
  }

  public static JSONString jsonString(@NotNull final CharSequence pString) {
    return new JSONString(pString);
  }

  public abstract Object getValue();

  @NotNull
  public abstract StringBuilder appendTo(@NotNull StringBuilder stringBuilder);

  @SuppressWarnings("null")
  @Override
  @NotNull
  public String toString() {
    return appendTo(new StringBuilder()).toString();
  }

}
