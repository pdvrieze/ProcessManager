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

package nl.adaptivity.process.models;

import nl.adaptivity.process.diagram.RootDrawableProcessModel;


/**
 * Created by pdvrieze on 15/11/15.
 */
public class ProcessModelHolder {

  public final RootDrawableProcessModel.Builder model;
  public final Long handle;
  private final boolean mLoading;
  private final boolean mPublicationPending;
  private final long id;

  public ProcessModelHolder() {
    mLoading = true;
    this.model = null;
    this.handle = null;
    this.mPublicationPending = false;
    this.id= -1L;
  }

  public ProcessModelHolder(final RootDrawableProcessModel.Builder model, final long id, final Long handle, boolean publicationPending) {
    mLoading = false;
    this.model = model;
    this.handle = handle;
    this.mPublicationPending = publicationPending;
    this.id = id;
  }

  public String getName() {
    return model==null ? null : model.getName();
  }

  public boolean isLoading() {
    return mLoading;
  }

  public boolean isFavourite() {
    return model==null ? false : model.isFavourite();
  }

  public long getId() {
    return id;
  }

  public void setFavourite(final boolean favourite) {
    model.setFavourite(favourite);
  }

  public boolean isPublicationPending() {
    return mPublicationPending;
  }
}
