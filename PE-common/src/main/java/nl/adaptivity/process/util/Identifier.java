package nl.adaptivity.process.util;

/**
 * Created by pdvrieze on 01/11/15.
 */
public class Identifier implements Identifiable {

  private String aID;

  public Identifier(final String pID) {
    aID = pID;
  }

  @Override
  public String getId() {
    return aID;
  }

  public void setID(final String pID) {
    aID = pID;
  }

  @Override
  public boolean equals(final Object pO) {
    if (this == pO) return true;
    if (pO == null || getClass() != pO.getClass()) return false;

    Identifier that = (Identifier) pO;

    return aID.equals(that.aID);

  }

  @Override
  public int hashCode() {
    return aID.hashCode();
  }

  @Override
  public String toString() {
    return aID;
  }
}
