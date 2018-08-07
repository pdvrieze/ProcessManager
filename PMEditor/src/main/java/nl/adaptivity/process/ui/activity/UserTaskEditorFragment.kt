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

package nl.adaptivity.process.ui.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import net.devrieze.util.CollectionUtil
import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.android.recyclerview.ClickableAdapter
import nl.adaptivity.android.recyclerview.ClickableAdapter.OnItemClickListener
import nl.adaptivity.process.diagram.DrawableActivity
import nl.adaptivity.process.diagram.android.ParcelableActivity
import nl.adaptivity.process.diagram.android.getUserTask
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.editor.android.databinding.FragmentUserTaskEditorBinding
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.tasks.EditableUserTask
import nl.adaptivity.process.tasks.TaskItem
import nl.adaptivity.process.tasks.items.LabelItem
import nl.adaptivity.process.tasks.items.ListItem
import nl.adaptivity.process.tasks.items.PasswordItem
import nl.adaptivity.process.tasks.items.TextItem
import nl.adaptivity.process.ui.UIConstants
import nl.adaptivity.process.ui.activity.UserTaskEditAdapter.ItemViewHolder
import nl.adaptivity.process.util.CharSequenceDecorator
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.ModifySequence
import nl.adaptivity.process.util.VariableReference.ResultReference
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader

import java.util.ArrayList


/**
 * A placeholder fragment containing a simple view.
 */
class UserTaskEditorFragment : Fragment(), OnItemClickListener<ItemViewHolder>, CharSequenceDecorator {
    private lateinit var binding: FragmentUserTaskEditorBinding
    private lateinit var taskEditAdapter: UserTaskEditAdapter
    private lateinit var activity: DrawableActivity.Builder
    /** The list of possible variables to use in here.  */
    private var variables: List<ResultReference>? = null

    /**
     * From the fragment, retrieve a parcelable activity.
     *
     * @return The parcelable activity that represents the activity state.
     */
    // TODO use existing prefix instead of hardcoded
    val parcelableResult: ParcelableActivity
        get() {
            val items = taskEditAdapter.content
            val editableUserTask: EditableUserTask
            if (activity.message == null) {
                editableUserTask = EditableUserTask(null, null, null, items)
            } else {
                editableUserTask = activity.getUserTask() ?: EditableUserTask(null, null, null, items)
                editableUserTask.setItems(items)
            }
            for (item in items) {
                if (!(item.isReadOnly || isNullOrEmpty(item.name) ||
                      item.name is ModifySequence)) {
                    val result = getResultFor(Constants.USER_MESSAGE_HANDLER_NS_PREFIX, item.name.toString())
                    if (result == null) {
                        val newResult = XmlResultType(getResultName("r_" + item.name), getResultPath(
                                Constants.USER_MESSAGE_HANDLER_NS_PREFIX, item.name.toString()), null as CharArray?,
                                                      SimpleNamespaceContext(
                                                              Constants.USER_MESSAGE_HANDLER_NS_PREFIX,
                                                              Constants.USER_MESSAGE_HANDLER_NS))
                        activity.results.add(newResult)
                    }
                }
            }
            activity.message = editableUserTask.asMessage()

            return ParcelableActivity(this.activity)
        }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_task_editor, container, false)
        binding.handler = this
        val view = binding.root

        val fab = view.findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { toggleFabMenu() }

        taskEditAdapter = UserTaskEditAdapter(this)
        binding.content.adapter = taskEditAdapter
        taskEditAdapter.onItemClickListener = this

        val ac: ParcelableActivity?
        if (savedInstanceState != null && savedInstanceState.containsKey(UIConstants.KEY_ACTIVITY)) {
            ac = savedInstanceState.getParcelable(UIConstants.KEY_ACTIVITY)
        } else if (arguments?.containsKey(UIConstants.KEY_ACTIVITY)==true) {
            ac = arguments!!.getParcelable(UIConstants.KEY_ACTIVITY)
        } else { ac = null }

        variables = arguments?.getParcelableArrayList(UIConstants.KEY_VARIABLES) ?: emptyList()
        if (ac != null) {
            activity = ac.builder()
            val EditableUserTask = ac.getUserTask()
            if (EditableUserTask != null) {
                taskEditAdapter.setItems(EditableUserTask.items)
            }
        }


        return view
    }

    private fun toggleFabMenu() {
        if (binding.fabMenu.visibility == View.VISIBLE) {
            hideFabMenu()
        } else {
            showFabMenu()
        }
    }

    private fun showFabMenu() {
        binding.fabMenu.visibility = View.VISIBLE
        binding.fabMenu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                                  MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val startHeight = binding.fab.measuredHeight
        val startWidth = binding.fab.measuredWidth

        val targetWidth = binding.fabMenu.measuredWidth
        val targetHeight = binding.fabMenu.measuredHeight
        binding.fabMenu.pivotX = (targetWidth - startWidth / 2).toFloat()
        binding.fabMenu.pivotY = targetHeight.toFloat()

        val menuAnimator = ValueAnimator.ofFloat(0f, 1f)
        menuAnimator.interpolator = AccelerateDecelerateInterpolator()
        menuAnimator.addUpdateListener(object : AnimatorUpdateListener {
            internal var oldImage = true

            override fun onAnimationUpdate(animation: ValueAnimator) {
                val animatedFraction = animation.animatedFraction
                binding.fabMenu.scaleX = (startWidth + (targetWidth - startWidth) * animatedFraction) / targetWidth
                binding.fabMenu.scaleY = (startHeight + (targetHeight - startHeight) * animatedFraction) / targetHeight
                if (oldImage && animatedFraction > 0.5f) {
                    binding.fab.setImageResource(R.drawable.ic_clear_black_24dp)
                    oldImage = false
                }
            }
        })
        menuAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.fabMenu.scaleX = 1f
                binding.fabMenu.scaleY = 1f
            }
        })
        menuAnimator.duration = ANIMATION_DURATION.toLong()
        menuAnimator.start()
    }

    private fun hideFabMenu() {
        // TODO animate this

        binding.fabMenu.visibility = View.VISIBLE
        binding.fabMenu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                                  MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val targetHeight = binding.fab.measuredHeight
        val targetWidth = binding.fab.measuredWidth

        val startWidth = binding.fabMenu.measuredWidth
        val startHeight = binding.fabMenu.measuredHeight
        binding.fabMenu.pivotX = (startWidth - targetWidth / 2).toFloat()
        binding.fabMenu.pivotY = startHeight.toFloat()

        val menuAnimator = ValueAnimator.ofFloat(0f, 1f)
        menuAnimator.interpolator = AccelerateDecelerateInterpolator()
        menuAnimator.addUpdateListener(object : AnimatorUpdateListener {
            internal var oldImage = true

            override fun onAnimationUpdate(animation: ValueAnimator) {
                val animatedFraction = animation.animatedFraction
                binding.fabMenu.scaleX = (startWidth + (targetWidth - startWidth) * animatedFraction) / startWidth
                binding.fabMenu.scaleY = (startHeight + (targetHeight - startHeight) * animatedFraction) / startHeight
                if (oldImage && animatedFraction > 0.5f) {
                    binding.fab.setImageResource(R.drawable.ic_action_new)
                    oldImage = false
                }
            }
        })
        menuAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.fabMenu.scaleX = 1f
                binding.fabMenu.scaleY = 1f
                binding.fabMenu.visibility = View.GONE
            }
        })
        menuAnimator.duration = ANIMATION_DURATION.toLong()
        menuAnimator.start()
    }

    override fun decorate(sequence: CharSequence): CharSequence {
        if (sequence is ModifySequence) {
            val idefine = activity.getDefine(sequence.variableName.toString()) ?: throw IllegalArgumentException(
                    "Invalid state")
            val define = XmlDefineType(idefine)
            try {
                val bodyStreamReader = define.bodyStreamReader
                return toLightSpanned(bodyStreamReader, define)
            } catch (e: XmlException) {
                throw RuntimeException(e)
            }

        } else {
            return sequence
        }
    }

    @Throws(XmlException::class)
    private fun toLightSpanned(bodyStreamReader: XmlReader, define: XmlDefineType): CharSequence {
        return VariableSpan.getSpanned(getActivity(), bodyStreamReader, define, VARSPAN_LIGHT_BORDER_ID)
    }

    fun onFabMenuItemClicked(v: View) {
        hideFabMenu()
        val name: String? = null // TODO use a dialog to ask for a name.
        when (v.id) {
            R.id.fabMenuLabel    -> {
                taskEditAdapter.addItem(LabelItem(name, null))
                ItemEditDialogFragment.newInstance(taskEditAdapter.getItem(taskEditAdapter.itemCount - 1),
                                                   variables,
                                                   activity.defines, taskEditAdapter.itemCount - 1)
                        .show(fragmentManager, "itemdialog")
            }
            R.id.fabMenuList     -> {
                taskEditAdapter.addItem(ListItem(name, "list", null, ArrayList<String>()))
                ItemEditDialogFragment.newInstance(taskEditAdapter.getItem(taskEditAdapter.itemCount - 1),
                                                   variables,
                                                   activity.defines, taskEditAdapter.itemCount - 1)
                        .show(fragmentManager, "itemdialog")
            }
            R.id.fabMenuOther    -> {
            }
            R.id.fabMenuPassword -> {
                taskEditAdapter.addItem(PasswordItem(name, "password", null))
                ItemEditDialogFragment.newInstance(taskEditAdapter.getItem(taskEditAdapter.itemCount - 1),
                                                   variables,
                                                   activity.defines, taskEditAdapter.itemCount - 1)
                        .show(fragmentManager, "itemdialog")
            }
            R.id.fabMenuText     -> {
                taskEditAdapter.addItem(TextItem(name, "text", null, ArrayList<String>()))
                ItemEditDialogFragment.newInstance(taskEditAdapter.getItem(taskEditAdapter.itemCount - 1),
                                                   variables,
                                                   activity.defines, taskEditAdapter.itemCount - 1)
                        .show(fragmentManager, "itemdialog")
            }
        }
    }

    override fun onClickItem(adapter: ClickableAdapter<out ItemViewHolder>,
                             viewHolder: ItemViewHolder): Boolean {
        ItemEditDialogFragment.newInstance(taskEditAdapter.getItem(viewHolder.adapterPosition), variables,
                                           activity.defines, viewHolder.adapterPosition)
                .show(fragmentManager, "itemdialog")
        return true
    }

    fun updateItem(itemNo: Int, newItem: TaskItem) {
        taskEditAdapter.setItem(itemNo, newItem)
    }

    fun updateDefine(define: XmlDefineType) {
        activity.defines.replaceBy(define)
    }

    /**
     * Get a result that is a simple output result for the task value.
     *
     * @param name The name of the value
     * @return The first matching result, or null, if none found.
     */
    private fun getResultFor(prefix: String, name: String): XmlResultType? {
        val xpath = getResultPath(prefix, name)
        return activity.results.firstOrNull() { it.content?.let { content -> content.isNotEmpty() && xpath == it.getPath() } == true }
            ?.let { XmlResultType(it) }
    }

    private fun getResultPath(prefix: String, valueName: String): String {
        return "/" + prefix +
               "result/value[@name='" +
               valueName + "']/text()"
    }

    private fun getResultName(candidate: String): String {
        val activity = activity
        if (activity.getResult(candidate) == null) {
            return candidate
        }
        return (2..Int.MAX_VALUE).asSequence()
            .map { "$candidate$it" }
            .first { activity.getResult(it)==null }
    }

    companion object {

        const val ANIMATION_DURATION = 200
        private const val VARSPAN_LIGHT_BORDER_ID = R.drawable.varspan_border_light

        fun newInstance(activity: ParcelableActivity,
                        variables: Collection<ResultReference>): UserTaskEditorFragment {
            val args = Bundle(2)
            args.putParcelable(UIConstants.KEY_ACTIVITY, activity)
            args.putParcelableArrayList(UIConstants.KEY_VARIABLES, CollectionUtil.toArrayList(variables))
            val fragment = UserTaskEditorFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
