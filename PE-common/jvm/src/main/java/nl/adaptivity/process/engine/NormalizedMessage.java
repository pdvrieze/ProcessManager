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

package nl.adaptivity.process.engine;

import org.jetbrains.annotations.NotNull;

import javax.activation.DataHandler;
import javax.xml.transform.Source;

import java.util.Set;


public interface NormalizedMessage {

  @NotNull
  Source getContent();

  void setContent(Source result);

  @NotNull
  DataHandler getAttachment(String key);

  void addAttachment(String string, DataHandler dataHandler);

  void removeAttachment(String key);

  @NotNull
  Set<String> getAttachmentNames();

}
