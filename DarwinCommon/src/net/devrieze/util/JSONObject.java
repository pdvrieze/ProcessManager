package net.devrieze.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public abstract class JSONObject {


  public static class JSONString extends JSONObject {

    private final CharSequence aString;

    public JSONString(final CharSequence pString) {
      aString = pString;
    }

    @Override
    public CharSequence getValue() {
      return aString;
    }

    @Override
    public StringBuilder appendTo(final StringBuilder pStringBuilder) {
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

    private final List<JSONObject> aItems;

    private JSONArray(final List<JSONObject> pItems) {
      aItems = pItems;
    }

    @Override
    public List<JSONObject> getValue() {
      return aItems;
    }

    @Override
    public StringBuilder appendTo(final StringBuilder stringBuilder) {
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

  public static JSONArray jsonArray(final List<JSONObject> items) {
    return new JSONArray(items);
  }

  public static JSONArray jsonArray(final JSONObject... items) {
    return jsonArray(Arrays.asList(items));
  }

  public static JSONString jsonString(final CharSequence pString) {
    return new JSONString(pString);
  }

  public abstract Object getValue();

  public abstract StringBuilder appendTo(StringBuilder stringBuilder);

  @Override
  public String toString() {
    return appendTo(new StringBuilder()).toString();
  }

}
