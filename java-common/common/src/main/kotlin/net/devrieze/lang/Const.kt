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
package net.devrieze.lang


/**
 * A class for general constants.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
object Const {

    /**
     * Prime used for creating hashes.
     */
    val _HASHPRIME = 31

    /** An ascii/utf8 linefeed.  */
    val _LF: Char = 10.toChar()

    /** An ascii/utf8 carriage return.  */
    val _CR: Char = 13.toChar()
}// Not instanciatable.
