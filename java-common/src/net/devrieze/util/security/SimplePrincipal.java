package net.devrieze.util.security;

import java.security.Principal;


public class SimplePrincipal implements Principal {

  private final String mName;

  public SimplePrincipal(final String pName) {
    mName = pName;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((mName == null) ? 0 : mName.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SimplePrincipal other = (SimplePrincipal) obj;
    if (mName == null) {
      if (other.mName != null) {
        return false;
      }
    } else if (!mName.equals(other.mName)) {
      return false;
    }
    return true;
  }

}
