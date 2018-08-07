/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.editor.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.*;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.diagram.android.DiagramView.DiagramDrawable;
import nl.adaptivity.diagram.android.DiagramView.OnNodeClickListener;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Builder;
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider;
import nl.adaptivity.process.processModel.ProcessNode.IBuilder;
import nl.adaptivity.process.ui.main.ProcessBaseActivity;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xmlutil.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static nl.adaptivity.diagram.Drawable.*;


public class PMEditor extends ProcessBaseActivity implements OnNodeClickListener, NodeEditListener, PMProvider {


    private static class MoveDrawable extends DiagramDrawable {

        private final List<float[]> mArrows;
        private final Drawable      mMinMaxOverlay;
        private       int           mAlpha = 255;
        private       Paint         mPaint;

        public MoveDrawable(@Nullable final Drawable minMaxOverlay, @NonNull final List<float[]> arrows) {
            mMinMaxOverlay = minMaxOverlay;
            mArrows = arrows;
        }

        @Override
        public void draw(final Canvas canvas, final double scale) {
            if (mMinMaxOverlay != null) {
                if (mMinMaxOverlay instanceof DiagramDrawable) {
                    ((DiagramDrawable) mMinMaxOverlay).draw(canvas, scale);
                } else {
                    mMinMaxOverlay.draw(canvas);
                }
            }
            if (mPaint == null) {
                mPaint = new Paint();
                mPaint.setAntiAlias(true);
                mPaint.setAlpha(mAlpha);
                mPaint.setStyle(Style.STROKE);
                mPaint.setStrokeWidth(3);
                mPaint.setARGB(255, 0, 255, 0);
            }
            for (final float[] arrow : mArrows) {
                canvas.drawLine(arrow[0], arrow[1], arrow[2], arrow[3], mPaint);
            }
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }

        @Override
        public void setAlpha(final int alpha) {
            mAlpha = alpha;
            if (mPaint != null) {
                mPaint.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(final ColorFilter cf) {
            // ignore
        }
    }

    private static class WaitTask extends FutureTask<Object> {

        private static final Callable<Object> NULLCALLABLE = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                throw new UnsupportedOperationException("Should not be set");
            }
        };

        private final boolean mImmediate;

        private final Drawable mOverlay;

        public WaitTask(final boolean immediate, final Drawable overlay) {
            super(NULLCALLABLE);
            mImmediate = immediate;
            mOverlay = overlay;
        }

        public void trigger() {
            set(Boolean.TRUE);
        }

        public boolean isImmediate() {
            return mImmediate;
        }
    }
    public static final  String EXTRA_PROCESS_MODEL = "nl.adaptivity.process.model";
    final static LayoutAlgorithm NULL_LAYOUT_ALGORITHM = new LayoutAlgorithm() {
        @Override
        public <T extends Positioned> boolean layout(@NonNull final List<? extends DiagramNode<? extends T>> nodes,
                                                     @NonNull final LayoutStepper<? super T> layoutStepper) {
            return false;
        }
    };
    private static final String KEY_PROCESSMODEL     = "processmodel";
    private static final String KEY_PROCESSMODEL_URI = "processmodeluri";
    private static final int STATE_ACTIVE = STATE_CUSTOM1;
    private static final int STATE_GROUP  = STATE_CUSTOM2;
    private static final int STATE_XMOST  = STATE_CUSTOM3;
    private static final int STATE_MOVED  = STATE_CUSTOM4;
    private static final String TAG                 = PMEditor.class.getName();
    private final ItemDragListener mItemDragListener = new ItemDragListener();
    boolean mStep = true;
    private int mBaseDiagramPaddingLeft;
    private int mBaseDiagramPaddingBottom;
    private DiagramView diagramView1;
    private DrawableProcessModel.Builder mPm;
    private MyDiagramAdapter mAdapter;
    private LayoutTask   mLayoutTask;
    private LinearLayout elementsView;
    private Uri          mPmUri;
    private boolean      mCancelled = false;

    /**
     * Called in response to a drag-drop of items.
     *
     * @param event The corresponding event.
     * @return <code>true</code> if handled succesfully.
     */
    private boolean onItemDropped(final DragEvent event) {
        final DrawableProcessNode.Builder<?> nodeType = (DrawableProcessNode.Builder<?>) event.getLocalState();
        final Constructor<?>      constructor;
        try {
            // TODO replace with factory
            final DrawableProcessNode.Builder<?> builder  = nodeType.copy();
            float                             diagramX = diagramView1.toDiagramX(event.getX());
            float                             diagramY = diagramView1.toDiagramY(event.getY());
            final int                         gridSize = diagramView1.getGridSize();
            if (gridSize > 0) {
                diagramX = Math.round(diagramX / gridSize) * gridSize;
                diagramY = Math.round(diagramY / gridSize) * gridSize;
            }
            builder.setX(diagramX);
            builder.setY(diagramY);
            // Reset id and label
            builder.setId(null);
            builder.setLabel(null);
            mPm.getNodes().add(builder);

            // Get an id from the process model builder. That will ensure uniqueness.
            mPm.ensureId(builder);

            diagramView1.invalidate();
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(PMEditor.class.getSimpleName(), "Failure to instantiate new node", e);
        }
        return false;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processmodel_editor);

        diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
        diagramView1.setOffsetX(0d);
        diagramView1.setOffsetY(0d);
        diagramView1.setOnNodeClickListener(this);
        diagramView1.setOnDragListener(mItemDragListener);

        mBaseDiagramPaddingLeft = diagramView1.getPaddingLeft();
        mBaseDiagramPaddingBottom = diagramView1.getPaddingBottom();

        CardView dragSelector = (CardView) findViewById(R.id.dragSelector);

        dragSelector.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(final View v,
                                       final int left, final int top, final int right, final int bottom,
                                       final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    diagramView1.setPadding(right + mBaseDiagramPaddingLeft, diagramView1.getPaddingTop(),
                                            diagramView1.getPaddingRight(), diagramView1.getPaddingBottom());
                } else {
                    diagramView1.setPadding(diagramView1.getPaddingLeft(), diagramView1.getPaddingTop(),
                                            diagramView1.getPaddingRight(), bottom - top + mBaseDiagramPaddingBottom);
                }
            }
        });


        elementsView = (LinearLayout) findViewById(R.id.diagramElementsGroup);
        if (elementsView != null) {
            elementsView.removeAllViews();

            final AndroidTheme theme = new AndroidTheme(AndroidStrategy.INSTANCE);

            final DrawableProcessModel viewOwner;
            {
                final Builder builder = new Builder();
                builder.getNodes().add(builder.startNodeBuilder());
                builder.getNodes().add(builder.activityBuilder());
                builder.getNodes().add(builder.splitBuilder());
                builder.getNodes().add(builder.joinBuilder());
                builder.getNodes().add(builder.endNodeBuilder());
                viewOwner = builder.build(false);

                for (IBuilder<DrawableProcessNode, DrawableProcessModel> node : builder.getNodes()) {
                    addNodeView(theme, (DrawableProcessNode.Builder<?>) node);
                }
            }

            elementsView.requestLayout();
        }

        if (savedInstanceState != null) {
            final PMParcelable pmparcelable = savedInstanceState.getParcelable(KEY_PROCESSMODEL);
            if (pmparcelable != null) {
                setPm(pmparcelable.getProcessModel().builder());
            }
            mPmUri = savedInstanceState.getParcelable(KEY_PROCESSMODEL_URI);
        } else {
            mPmUri = getIntent().getData();
            if (mPmUri != null) {
                final LayoutAlgorithm layoutAlgorithm = new LayoutAlgorithm();
                setPm(loadProcessModel(mPmUri, layoutAlgorithm, layoutAlgorithm));
            }
        }
        if (getPm() == null) {
            setPm(loadInitialProcessModel());
        }
        mAdapter = new MyDiagramAdapter(this, getPm());
        diagramView1.setAdapter(mAdapter);

        getSupportFragmentManager().beginTransaction().add(new PMProcessesFragment(), "processModelHelper").commit();
    }

    /**
     * Add a view to the elementsView that shows a single process node.
     *
     * @param theme
     * @param node
     */
    private void addNodeView(final AndroidTheme theme, final DrawableProcessNode.Builder node) {
        final LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.weight = 1f;
        if (elementsView.getOrientation() == LinearLayout.HORIZONTAL) {
            lp.leftMargin = getResources().getDimensionPixelSize(R.dimen.dragnodemargin);
            lp.rightMargin = lp.leftMargin;
        } else {
            lp.topMargin = getResources().getDimensionPixelSize(R.dimen.dragnodemargin);
            lp.bottomMargin = lp.topMargin;
        }

        final ImageView v = new ImageView(this);
        v.setLayoutParams(lp);
        v.setImageDrawable(new DrawableDrawable(positionNode(node), theme, true));
        v.setOnTouchListener(mItemDragListener);
        v.setOnDragListener(mItemDragListener);
        v.setTag(node);
        elementsView.addView(v);
    }

    private static <T extends DrawableProcessNode> DrawableProcessNode.Builder<T> positionNode(final DrawableProcessNode.Builder<T> node) {
        node.setX(0);
        node.setY(0);
        final Rectangle bounds = node.getBounds();
        node.setX(-bounds.left);
        node.setY(-bounds.top);
        return node;
    }

    private DrawableProcessModel.Builder getPm() {
        return mPm;
    }

    private void setPm(final DrawableProcessModel.Builder pm) {
        mPm = pm;
    }

    private DrawableProcessModel.Builder loadInitialProcessModel() {
        final LayoutAlgorithm layout = new LayoutAlgorithm();
        return loadInitialProcessModel(layout, layout);
    }

    private DrawableProcessModel.Builder loadInitialProcessModel(final LayoutAlgorithm simpleLayoutAlgorithm,
                                                                 final LayoutAlgorithm advancedAlgorithm) {
        return loadProcessModel(getResources().openRawResource(R.raw.processmodel), simpleLayoutAlgorithm,
                                advancedAlgorithm);
    }

    private DrawableProcessModel.Builder loadProcessModel(final Uri uri,
                                                          final LayoutAlgorithm simpleLayoutAlgorithm,
                                                          final LayoutAlgorithm advancedAlgorithm) {
        try {
            return loadProcessModel(getContentResolver().openInputStream(uri), simpleLayoutAlgorithm,
                                    advancedAlgorithm);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }
    }

    public DrawableProcessModel.Builder loadProcessModel(final InputStream in,
                                                         final LayoutAlgorithm simpleLayoutAlgorithm,
                                                         final LayoutAlgorithm advancedAlgorithm) {
        try {
            return PMParser.parseProcessModel(in, simpleLayoutAlgorithm, advancedAlgorithm);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(PMEditor.class.getName(), e.getMessage(), e);
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            // no results yet
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(getPm().build().getRootModel()));
        if (mPmUri != null) {
            outState.putParcelable(KEY_PROCESSMODEL_URI, mPmUri);
        }
    }

    private void setPm(final DrawableProcessModel pm) {
        mPm = pm.builder();
    }

    public void setLabel(final String string) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                getSupportActionBar().setTitle("PMEditor - " + string);
            }
        });
    }

    public void updateDiagramBounds() {
        double                             minX = Double.POSITIVE_INFINITY;
        double                             minY = Double.POSITIVE_INFINITY;
        double                             maxY = Double.NEGATIVE_INFINITY;
        final DrawableProcessModel.Builder pm   = getPm();
        if (pm != null) {
            for (final DrawableProcessNode.Builder node : pm.getChildElements()) {
                if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
                    final Rectangle bounds = node.getBounds();
                    if (bounds.left < minX) { minX = bounds.left; }
                    if (bounds.top < minY) { minY = bounds.top; }
                    if (bounds.bottom() > maxY) { maxY = bounds.bottom(); }
                }
            }
            final double offsetX = Double.isInfinite(minX) ? 0 : minX - pm.getLeftPadding();
            final double offsetY = Double.isInfinite(minY) ? 0 : minY - pm.getTopPadding();
            diagramView1.setOffsetX(offsetX/*/diagramView1.getScale()*/);
            diagramView1.setOffsetY(
                offsetY - (((diagramView1.getHeight() / diagramView1.getScale()) - (maxY - minY)) / 2));
        }
    }

    @Override
    protected void onDestroy() {
        if (mLayoutTask != null) {
            mLayoutTask.playAll();
            mLayoutTask.cancel(false);
        }
        super.onDestroy();
    }

    private void updateActivityResult() {
        if (mCancelled) {
            setResult(RESULT_CANCELED);
        } else {
            final Intent               data                 = new Intent();
            final DrawableProcessModel drawableProcessModel = getPm().build();
            if (mPmUri != null) {
                data.setData(mPmUri);
                if ("content".equals(mPmUri.getScheme()) || "file".equals(mPmUri.getScheme())) {
                    final OutputStream out;
                    try {
                        out = getContentResolver().openOutputStream(mPmUri);
                        try {
                            PMParser.serializeProcessModel(out, drawableProcessModel.getRootModel());
                        } finally {
                            out.close();
                        }
                    } catch (XmlPullParserException | IOException e) {
                        Log.e(TAG, "Failure to save process model on closure", e);
                    }
                }
            }
            if (getPm() != null) {
                final PMParcelable parcelable = new PMParcelable(drawableProcessModel.getRootModel());
                data.putExtra(EXTRA_PROCESS_MODEL, parcelable);
            }
            setResult(RESULT_OK, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.diagram_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ac_cancel:
                mCancelled = true;
                finish();
                break;
            case R.id.ac_next:
                if (mLayoutTask != null) {
                    mLayoutTask.next();
                } else {
                    mLayoutTask = new LayoutTask(getPm(), false);
                    mLayoutTask.execute();
                }
                break;
            case R.id.ac_reset:
                if (mLayoutTask != null) {
                    mLayoutTask.cancel(false);
                    mLayoutTask.playAll();
                }
                mPm = new RootDrawableProcessModel.Builder(); // unset the process model
                mLayoutTask = new LayoutTask(getPm(), true);
                mLayoutTask.execute();
                break;
            case R.id.ac_play:
                if (mLayoutTask != null) {
                    mLayoutTask.playAll();
                }
                break;
            case android.R.id.home:
                updateActivityResult();
                //$FALL-THROUGH$ we just add behaviour but then go to the parent.
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void finish() {
        updateActivityResult();
        super.finish();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        final Intent intent = super.getParentActivityIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public boolean onNodeClicked(final DiagramView view, final int position, final MotionEvent event) {
        final int oldSelection = view.getSelection();
        if (oldSelection == position) {
            view.setSelection(-1);
        } else {
            view.setSelection(position);
        }
        return true;
    }

    @Override
    public DrawableProcessNode.Builder getNode(final int pos) {
        return mAdapter.getItem(pos);
    }

    @Override
    public void onNodeEdit(final int pos) {
        diagramView1.invalidate();
    }

    @Override
    public void updateNode(final int pos, final DrawableProcessNode.Builder newValue) {
        mAdapter.updateItem(pos, newValue);
    }

    @Override
    public DrawableProcessNode.Builder getNode(final Identifiable id) {
        return mAdapter.getDiagram().getNode(id.getId());
    }

    @Override
    public DrawableProcessModel.Builder getProcessModel() {
        return getPm();
    }

    private final class LineDrawable extends DiagramDrawable {

        private final Paint  mPaint;
        private       double mX1;
        private       double mY1;
        private       double mX2;
        private       double mY2;

        public LineDrawable(final double x1, final double y1, final double x2, final double y2, final Paint paint) {
            mX1 = x1;
            mX2 = x2;
            mY1 = y1;
            mY2 = y2;
            mPaint = paint;
        }

        @Override
        public void draw(final Canvas canvas, final double scale) {
            final int height = (int) Math.round((canvas.getHeight() / scale));
            final int width  = (int) Math.round(canvas.getWidth() / scale);
            if (!Double.isNaN(mX1)) {
                final float scaledX = (float) (mX1 - diagramView1.getOffsetX());
                canvas.drawLine(scaledX, 0, scaledX, height, mPaint);
            }
            if (!Double.isNaN(mY1)) {
                final float scaledY = (float) (mY1 - diagramView1.getOffsetY());
                canvas.drawLine(0, scaledY, width, scaledY, mPaint);
            }
            if (!Double.isNaN(mX2)) {
                final float scaledX = (float) (mX2 - diagramView1.getOffsetX());
                canvas.drawLine(scaledX, 0, scaledX, height, mPaint);
            }
            if (!Double.isNaN(mY2)) {
                final float scaledY = (float) (mY2 - diagramView1.getOffsetY());
                canvas.drawLine(0, scaledY, width, scaledY, mPaint);
            }
        }

        @Override
        public void setAlpha(final int alpha) {
            //
        }        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }



        @Override
        public void setColorFilter(final ColorFilter cf) {
            //
        }
    }

    private class MyStepper extends AbstractLayoutStepper<DrawableProcessNode.Builder> {

        private boolean                                            mImmediate     = false;
        private AndroidPen                                         mXMostPen;
        private DiagramNode<? extends DrawableProcessNode.Builder> mLayoutNode;
        private double                                             mMinX          = Double.NaN;
        private double                                             mMinY          = Double.NaN;
        private double                                             mMaxX          = Double.NaN;
        private double                                             mMaxY          = Double.NaN;
        private LineDrawable                                       mMinMaxOverlay = null;

        @Override
        public void reportMove(final DiagramNode<? extends DrawableProcessNode.Builder> node,
                               final double newX,
                               final double newY) {
            setLabel("move");
            final DrawableProcessNode.Builder target = node.getTarget();
            target.setState(target.getState() | STATE_MOVED);
            target.setX(newX);
            target.setY(newY);
            updateDiagramBounds();

            waitForNextClicked(moveDrawable(mMinMaxOverlay, Arrays.asList(node)));

            target.setState(target.getState() & ~STATE_MOVED);
            mMinX = Double.NaN;
            mMinY = Double.NaN;
            mMaxX = Double.NaN;
            mMaxY = Double.NaN;
            mMinMaxOverlay = null;
        }

        @Override
        public void reportLowest(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                 final DiagramNode<? extends DrawableProcessNode.Builder> node) {
            setLabel("lowest");
            reportXMost(nodes, node);
        }

        @Override
        public void reportHighest(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                  final DiagramNode<? extends DrawableProcessNode.Builder> node) {
            setLabel("highest");
            reportXMost(nodes, node);
        }

        @Override
        public void reportRightmost(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                    final DiagramNode<? extends DrawableProcessNode.Builder> node) {
            setLabel("rightmost");
            reportXMost(nodes, node);
        }

        @Override
        public void reportLeftmost(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                   final DiagramNode<? extends DrawableProcessNode.Builder> node) {
            setLabel("leftmost");
            reportXMost(nodes, node);
        }

        @Override
        public void reportLayoutNode(final DiagramNode<? extends DrawableProcessNode.Builder> node) {
            mMinX = Double.NaN;
            mMinY = Double.NaN;
            mMaxX = Double.NaN;
            mMaxY = Double.NaN;
            mMinMaxOverlay = null;
            setLabel("node under consideration");
            if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
                if (mLayoutNode != null) {
                    final DrawableProcessNode.Builder target = mLayoutNode.getTarget();
                    target.setState(target.getState() & ~STATE_ACTIVE);
                }
                mLayoutNode = node;
                {
                    final DrawableProcessNode.Builder target = mLayoutNode.getTarget();
                    target.setState(target.getState() | STATE_ACTIVE);
                }
                waitForNextClicked(null);
            }
        }

        @Override
        public void reportMoveX(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                final double offset) {
            setLabel("moveX");
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setX(node.getX() + offset);
                target.setY(node.getY());
                target.setState(target.getState() | STATE_MOVED);
            }
            updateDiagramBounds();
            waitForNextClicked(moveDrawable(mMinMaxOverlay, nodes));
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() & ~STATE_MOVED);
            }
        }

        @Override
        public void reportMoveY(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                final double offset) {
            setLabel("moveY");
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setX(node.getX());
                target.setY(node.getY() + offset);
                target.setState(target.getState() | STATE_MOVED);
            }
            updateDiagramBounds();
            waitForNextClicked(moveDrawable(mMinMaxOverlay, nodes));
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() & ~STATE_MOVED);
            }
        }

        @Override
        public void reportMinX(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                               final double x) {
            setLabel("minX");
            mMinX = x;
            reportMinMax(nodes);
        }

        @Override
        public void reportMinY(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                               final double y) {
            setLabel("minY");
            mMinY = y;
            reportMinMax(nodes);
        }

        @Override
        public void reportMaxX(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                               final double x) {
            setLabel("maxX");
            mMaxX = x;
            reportMinMax(nodes);
        }

        @Override
        public void reportMaxY(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                               final double y) {
            setLabel("maxY");
            mMaxY = y;
            reportMinMax(nodes);
        }

        @Override
        public void reportSiblings(final DiagramNode<? extends DrawableProcessNode.Builder> currentNode,
                                   final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                   final boolean above) {
            setLabel(above ? "Siblings above" : "Siblings below");
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setX(node.getX());
                target.setY(node.getY());

                if (node != currentNode) {
                    target.setState(target.getState() | STATE_GROUP);
                }
            }
            if (currentNode != null) {
                final DrawableProcessNode.Builder target = currentNode.getTarget();
                target.setState(target.getState() | STATE_ACTIVE);
            }
            updateDiagramBounds();
            waitForNextClicked(mMinMaxOverlay);
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() & ~(STATE_ACTIVE | STATE_GROUP));
            }
        }

        private void reportMinMax(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes) {
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() | STATE_GROUP);
            }
            if (mMinMaxOverlay == null) {
                mMinMaxOverlay = new LineDrawable(mMinX, mMinY, mMaxX, mMaxY, getLinePen().getPaint());
            } else {
                mMinMaxOverlay.mX1 = mMinX;
                mMinMaxOverlay.mY1 = mMinY;
                mMinMaxOverlay.mX2 = mMaxX;
                mMinMaxOverlay.mY2 = mMaxY;
            }
            waitForNextClicked(mMinMaxOverlay);
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() & ~STATE_GROUP);
            }
        }

        private AndroidPen getLinePen() {
            if (mXMostPen == null) {
                final Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setARGB(255, 255, 0, 0);
                mXMostPen = new AndroidPen(paint);
            }
            return mXMostPen;
        }

        private void waitForNextClicked(final Drawable overlay) {
            if (mLayoutTask != null) {
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    if (BuildConfig.DEBUG) {
                        throw new IllegalStateException("Performing layout on UI thread");
                    }
                } else {
                    final WaitTask task = new WaitTask(mImmediate, overlay);
                    mLayoutTask.postProgress(task);
                    try {
                        task.get();
                    } catch (ExecutionException | InterruptedException e) { // ignore
                    }
                }
            }
        }

        private void reportXMost(final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes,
                                 final DiagramNode<? extends DrawableProcessNode.Builder> activeNode) {
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setX(node.getX());
                target.setY(node.getY());

                if (node != activeNode) {
                    target.setState(target.getState() | STATE_GROUP);
                }
            }
            if (activeNode != null) {
                final DrawableProcessNode.Builder target = activeNode.getTarget();
                target.setState(target.getState() | STATE_XMOST);
            }
            updateDiagramBounds();
            waitForNextClicked(mMinMaxOverlay);
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                final DrawableProcessNode.Builder target = node.getTarget();
                target.setState(target.getState() & ~(STATE_XMOST | STATE_GROUP));
            }
        }

        @NonNull
        private MoveDrawable moveDrawable(@Nullable final Drawable minMaxOverlay,
                                          @NonNull
                                          final List<? extends DiagramNode<? extends DrawableProcessNode.Builder>> nodes) {
            final List<float[]> arrows = new ArrayList<>(nodes.size());
            for (final DiagramNode<? extends DrawableProcessNode.Builder> node : nodes) {
                if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
                    final double scale = diagramView1.getScale();
                    arrows.add(new float[]{(float) ((node.getX() + (diagramView1.getOffsetX() * scale)) * scale),
                                           (float) ((node.getY() + (diagramView1.getOffsetY() * scale)) * scale),
                                           (float) ((node.getTarget().getX() + (diagramView1.getOffsetX() * scale)) *
                                                    scale),
                                           (float) ((node.getTarget().getY() + (diagramView1.getOffsetY() * scale)) *
                                                    scale)});
                }
            }
            return new MoveDrawable(minMaxOverlay, arrows);
        }
    }

    private class LayoutTask extends AsyncTask<DrawableProcessModel.Builder, WaitTask, DrawableProcessModel.Builder> {

        private final MyStepper                    mStepper = new MyStepper();
        private       WaitTask                     mTask;
        private       DrawableProcessModel.Builder mPm;

        LayoutTask(final DrawableProcessModel.Builder pm, boolean fullReset) {
            if (pm != null) {
                mPm = pm;
            } else if (mPm == null || mPm.getNodes().isEmpty()) {
                mPm = loadInitialProcessModel(NULL_LAYOUT_ALGORITHM, new LayoutAlgorithm());
            }
            if (fullReset) {
                for (DrawableProcessNode.Builder node : mPm.getChildElements()) {
                    node.setX(Double.NaN);
                    node.setY(Double.NaN);
                }
            }
        }

        @Override
        protected DrawableProcessModel.Builder doInBackground(final DrawableProcessModel.Builder... params) {
            // Start with null layout algorithm, to prevent dual layout.
            if (mPm != null) {
                mPm.layout(mStepper);
            }
            return mPm;
        }

        @Override
        protected void onPreExecute() {
            if (mPm != null) {
                final LayoutAlgorithm alg = new LayoutAlgorithm();
                alg.setGridSize(diagramView1.getGridSize());
                alg.setTighten(true);
                mPm.setLayoutAlgorithm(alg);
                mAdapter = new MyDiagramAdapter(PMEditor.this, mPm);
                diagramView1.setAdapter(mAdapter);
//        mPm.layout();
            }
        }

        @Override
        protected void onPostExecute(final DrawableProcessModel.Builder result) {
            mLayoutTask = null;
            updateDiagramBounds();
            if (mStepper.mLayoutNode != null) {
                final DrawableProcessNode.Builder target = mStepper.mLayoutNode.getTarget();
                target.setState(target.getState() & (~(STATE_CUSTOM1 | STATE_CUSTOM2 | STATE_CUSTOM3 | STATE_CUSTOM4)));
                mStepper.mLayoutNode = null;
            }
            diagramView1.setOverlay(null);
            diagramView1.invalidate();
        }

        @Override
        protected void onProgressUpdate(final WaitTask... values) {
            final WaitTask task = values[0];
            if (!isCancelled()) {
                //      findViewById(R.id.ac_next).setEnabled(true);
                diagramView1.setOverlay(task.mOverlay);
                diagramView1.invalidate();
            }
            if (task.isImmediate()) {
                task.trigger();
            }
            mTask = task;
        }

        public void postProgress(final WaitTask waitTask) {
            this.publishProgress(waitTask);
        }

        public void playAll() {
            mStepper.mImmediate = true;
            if (mTask != null) {
                mTask.trigger();
                mTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
            }
        }

        public void next() {
            if (mTask != null) {
                mTask.trigger();
                mTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
            }
        }
    }

    private class ItemDragListener implements OnDragListener, OnTouchListener {

        @Override
        public boolean onDrag(final View v, final DragEvent event) {
            final int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    return (v == diagramView1);
                case DragEvent.ACTION_DROP: {
                    if (v == diagramView1) {
                        if (onItemDropped(event)) { return true; }
                    }
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            // Don't handle multitouch
            if (event.getPointerCount() > 1) {
                return false;
            }

            final ImageView        v      = (ImageView) view;
            final DrawableDrawable d      = (DrawableDrawable) v.getDrawable();
            final int              action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    // Get the bounds of the drawable
                    final RectF boundsF = new RectF(d.getBounds());
                    // Convert the bounds into imageview relative coordinates
                    v.getImageMatrix().mapRect(boundsF);
                    // Only work when the image is touched
                    if (boundsF.contains(event.getX(), event.getY())) {
                        final ClipData data = ClipData.newPlainText("label", "text");
                        v.startDrag(data, new ItemShadowBuilder(d), v.getTag(), 0);
                    }
                    break;
                }
            }

            return false;
        }
    }

    private class ItemShadowBuilder extends DragShadowBuilder {

        private final DrawableDrawable mDrawable;
        private       double           mScale;

        public ItemShadowBuilder(final DrawableDrawable d) {
            super(elementsView);
            mDrawable = d.clone();
            mDrawable.setState(new int[]{android.R.attr.state_pressed});
        }

        @Override
        public void onProvideShadowMetrics(final Point shadowSize, final Point shadowTouchPoint) {
            mScale = diagramView1.getScale();
            mDrawable.setScale(mScale);
            shadowSize.x = (int) Math.ceil(mDrawable.getIntrinsicWidth() + 2 * mScale * AndroidTheme.SHADER_RADIUS);
            shadowSize.y = (int) Math.ceil(mDrawable.getIntrinsicHeight() + 2 * mScale * AndroidTheme.SHADER_RADIUS);
            shadowTouchPoint.x = shadowSize.x / 2;
            shadowTouchPoint.y = shadowSize.y / 2;
            final int padding = (int) Math.round(mScale * AndroidTheme.SHADER_RADIUS);
            mDrawable.setBounds(padding, padding, shadowSize.x - padding, shadowSize.y - padding);
        }

        @Override
        public void onDrawShadow(final Canvas canvas) {
            final float padding = (float) (mScale * AndroidTheme.SHADER_RADIUS);
            final int   restore = canvas.save();
            canvas.translate(padding, padding);
            mDrawable.draw(canvas);
            canvas.restoreToCount(restore);
        }
    }
}
