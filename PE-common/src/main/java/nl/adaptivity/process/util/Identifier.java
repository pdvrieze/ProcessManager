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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A class representing a simple identifier. It just holds a single string.
 */
public final class Identifier implements Identified {
  private static class ChangeableIdentifier implements Identified {

    private final String mIdBase;
    private int mIdNo;

    public ChangeableIdentifier(final String idBase) {
      mIdBase = idBase;
      mIdNo = 1;
    }

    @Override
    public int compareTo(final Identifiable o) {
      return getId().compareTo(o.getId());
    }

    public void next() {
      ++mIdNo;
    }

    @Override
    public String getId() {
      return mIdBase + Integer.toString(mIdNo);
    }

    @Nullable
    @Override
    public Identifier getIdentifier() {
      return Identified.DefaultImpls.getIdentifier(this);
    }
  }

  @NotNull
  private String mID;

  public Identifier(final @NotNull String iD) {
    mID = iD;
  }

  public Identifier(final @NotNull CharSequence iD) {
    mID = iD.toString();
  }

  @Override
  public String getId() {
    return mID;
  }

  public void setId(final @NotNull String iD) {
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
  public int compareTo(final Identifiable o) {
    return mID.compareTo(o.getId());
  }

  @Override
  public int hashCode() {
    return mID.hashCode();
  }

  @Override
  public String toString() {
    return mID;
  }

  @Nullable
  @Override
  public Identifier getIdentifier() {
    return this;
  }

  public static String findIdentifier(String idBase, Iterable<? extends Identifiable> exclusions) {
    ChangeableIdentifier idFactory = new ChangeableIdentifier(idBase);
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
