package nl.adaptivity.xml;

/**
 * Created by pdvrieze on 15/11/15.
 */
public interface XmlReadingConstants {

  int START_DOCUMENT = 0;
  int START_TAG = 1;
  int END_TAG = 2;
  int COMMENT = 3;
  int TEXT = 4;
  int CDSECT = 5;
  int DOCDECL = 6;
  int END_DOCUMENT = 7;
  int ENTITY_REF = 8;
  int IGNORABLE_WHITESPACE = 9;
  int PROCESSING_INSTRUCTION = 10;
}
