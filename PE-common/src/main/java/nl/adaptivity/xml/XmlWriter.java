package nl.adaptivity.xml;

/**
 * Created by pdvrieze on 15/11/15.
 */
public interface XmlWriter {

  /**
   * Flush all state to the underlying buffer
   */
  void flush() throws XmlException;

  /**
   * Write a start tag.
   * @param namespace The namespace to use for the tag.
   * @param localName The local name for the tag.
   * @param prefix The prefix to use, or <code>null</code> for the namespace to be assigned automatically
   */
  void startTag(CharSequence namespace, CharSequence localName, CharSequence prefix) throws XmlException;

  /**
   * Write a comment.
   * @param text The comment text
   */
  void comment(CharSequence text) throws XmlException;

  /**
   * Write text.
   * @param text The text content.
   */
  void text(CharSequence text) throws XmlException;

  /**
   * Write a CDATA section
   * @param text The text of the section.
   */
  void cdsect(CharSequence text) throws XmlException;
}
