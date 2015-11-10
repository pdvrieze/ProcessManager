package nl.adaptivity.process.util;

import org.jetbrains.annotations.Nullable;


/**
 * Created by pdvrieze on 01/11/15.
 */
public class Identifier implements Identifiable {

  private String aID;

  public Identifier(final String iD) {
    aID = iD;
  }

  @Override
  public String getId() {
    return aID;
  }

  public void setID(final String iD) {
    aID = iD;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Identifier that = (Identifier) o;

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
