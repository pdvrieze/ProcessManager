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

package nl.adaptivity.xml;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import java.util.UUID;

/**
 * Simple JAXB adapter that maps UUID's to strings and reverse
 * @author Paul de Vrieze
 *
 */
public class UUIDAdapter extends XmlAdapter<String, UUID> {

  @Override
  public UUID unmarshal(final String v) throws Exception {
    return UUID.fromString(v);
  }

  @Override
  public String marshal(@NotNull final UUID v) throws Exception {
    return v.toString();
  }

}
