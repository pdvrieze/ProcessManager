package net.devrieze.util.security;

import net.devrieze.util.security.SecurityProvider.Permission;


public interface SecureObject {

  public enum Permissions implements Permission {
    READ,
    RENAME,
    UPDATE,
    DELETE, ;

  }

}
