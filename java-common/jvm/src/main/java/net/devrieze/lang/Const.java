/*
 * Copyright (c) 2018.
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

/*
 * Created on Nov 24, 2005
 *
 */
package net.devrieze.lang;


/**
 * A class for general constants.
 * 
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public final class Const {

  /**
   * Prime used for creating hashes.
   */
  public static final int _HASHPRIME = 31;

  /** An ascii/utf8 linefeed. */
  public static final char _LF = 10;

  /** An ascii/utf8 carriage return. */
  public static final char _CR = 13;

  private Const() {
    // Not instanciatable.
  }
}
