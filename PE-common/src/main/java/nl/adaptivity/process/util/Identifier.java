package nl.adaptivity.process.util;

import org.jetbrains.annotations.Nullable;


/**
 * A class representing a simple identifier. It just holds a single string.
 */
public final class Identifier implements Identifiable {
  private static class ChangableIdentifier implements Identifiable {

    private final String mIdBase;
    private int mIdNo;

    public ChangableIdentifier(final String idBase) {
      mIdBase = idBase;
      mIdNo = 1;
    }

    public void next() {
      ++mIdNo;
    }

    @Override
    public String getId() {
      return mIdBase + Integer.toString(mIdNo);
    }
  }


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

  public static String findIdentifier(String idBase, Iterable<? extends Identifiable> exclusions) {
    ChangableIdentifier idFactory = new ChangableIdentifier(idBase);
    outer: for(String candidate=idFactory.getId(); true; idFactory.next(), candidate = idFactory.getId()) {
      for(Identifiable exclusion: exclusions) {
        if (candidate.equals(exclusion.getId())) {
          continue outer;
        }
      }
      return candidate;
    }
  }
}
