package nl.adaptivity.process.util;

import org.jetbrains.annotations.Nullable;


/**
 * Created by pdvrieze on 01/11/15.
 */
public class Identifier implements Identifiable {

  private String mID;

  public Identifier(final String iD) {
    mID = iD;
  }

  @Override
  public String getId() {
    return mID;
  }

  public void setID(final String iD) {
    mID = iD;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Identifier that = (Identifier) o;

    return mID.equals(that.mID);

  }

  @Override
  public int hashCode() {
    return mID.hashCode();
  }

  @Override
  public String toString() {
    return mID;
  }
}
