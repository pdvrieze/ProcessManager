package nl.adaptivity.process.models;

import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.processModel.ProcessModel;

/**
 * Created by pdvrieze on 15/11/15.
 */
public class ProcessModelHolder {

  public final DrawableProcessModel model;
  public final Long handle;
  private final boolean mLoading;

  public ProcessModelHolder() {
    mLoading = true;
    this.model = null;
    this.handle = null;
  }

  public ProcessModelHolder(DrawableProcessModel model, Long handle) {
    mLoading = false;
    this.model = model;
    this.handle = handle;
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

  public void setFavourite(final boolean favourite) {
    model.setFavourite(favourite);
  }
}
