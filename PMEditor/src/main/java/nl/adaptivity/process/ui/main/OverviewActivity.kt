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

package nl.adaptivity.process.ui.main

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.AsyncTask
import android.os.Bundle
import android.os.RemoteException
import android.support.annotation.IdRes
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.FragmentManager.OnBackStackChangedListener
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import nl.adaptivity.android.compat.TitleFragment
import nl.adaptivity.android.util.GetNameDialogFragment
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks
import nl.adaptivity.process.data.ProviderHelper
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.editor.android.databinding.ActivityOverviewBinding
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.tasks.data.TaskProvider
import nl.adaptivity.process.ui.main.OverviewFragment.OverviewCallbacks
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks
import nl.adaptivity.process.ui.model.ProcessModelListOuterFragment
import nl.adaptivity.process.ui.task.TaskDetailFragment.TaskDetailCallbacks
import nl.adaptivity.process.ui.task.TaskListOuterFragment


open class OverviewActivity : ProcessBaseActivity(), OnNavigationItemSelectedListener, OverviewCallbacks, GetNameDialogFragmentCallbacks, ProcessModelDetailFragmentCallbacks, TaskDetailCallbacks, OnBackStackChangedListener {

    private lateinit var mBinding: ActivityOverviewBinding
    private var mTitle: CharSequence? = null
    private var activeFragment: TitleFragment? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mModelIdToInstantiate = -1L


    protected open fun bindLayout(): ActivityOverviewBinding {
        return DataBindingUtil.setContentView(this, R.layout.activity_overview)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener(this)
        mTitle = title
        mBinding = bindLayout()
        setSupportActionBar(mBinding.overviewAppBar!!.toolbar)

        val drawer = mBinding.overviewDrawer
        mDrawerToggle = object : ActionBarDrawerToggle(this, drawer, mBinding.overviewAppBar!!.toolbar,
                                                       R.string.navigation_drawer_open,
                                                       R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                finishSettingFragment()
            }

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val ab = supportActionBar
                ab?.setTitle(mTitle)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

        }
        drawer.setDrawerListener(mDrawerToggle)

        val navigationView = mBinding.navView
        navigationView.setNavigationItemSelectedListener(this)

        requestAccount(ProviderHelper.getAuthBase(this))

        if (savedInstanceState == null) {
            val intent = intent
            val uri = intent.data
            var handle: Long = 0
            @IdRes var navTarget = R.id.nav_home
            if (uri != null) {
                val path = uri.path
                if (path!!.startsWith(SERVERPATH_MODELS)) {
                    navTarget = R.id.nav_models
                    if (path.length > SERVERPATH_MODELS.length && path[SERVERPATH_MODELS.length] == '/') {
                        handle = java.lang.Long.parseLong(path.substring(SERVERPATH_MODELS.length + 1))
                    }
                } else if (path.startsWith(SERVERPATH_TASKS)) {
                    navTarget = R.id.nav_tasks
                    if (path.length > SERVERPATH_TASKS.length && path[SERVERPATH_TASKS.length] == '/') {
                        handle = java.lang.Long.parseLong(path.substring(SERVERPATH_TASKS.length + 1))
                    }
                }
            }
            if (handle != 0L) {
                val bgNavigation = object : AsyncTask<Long, Void, Long>() {
                    var mNavTarget: Int = 0

                    protected override fun doInBackground(vararg params: Long?): Long {
                        mNavTarget = params[0]!!.toInt()
                        val handle = params[1]!!
                        val id: Long
                        when (mNavTarget) {
                            R.id.nav_models -> id = ProcessModelProvider.getIdForHandle(this@OverviewActivity, handle)
                            R.id.nav_tasks  -> id = TaskProvider.getIdForHandle(this@OverviewActivity, handle)
                            else            -> return java.lang.Long.valueOf(0)
                        }

                        return java.lang.Long.valueOf(id)
                    }

                    override fun onPostExecute(itemId: Long) {
                        onNavigationItemSelected(mNavTarget, true, itemId)
                        mBinding.navView.setCheckedItem(mNavTarget)
                    }
                }
                bgNavigation.execute(java.lang.Long.valueOf(navTarget.toLong()), java.lang.Long.valueOf(handle))
            } else {
                // Go by default to the home fragment. Don't add it to the back stack.
                onNavigationItemSelected(navTarget, false)
                mBinding.navView.setCheckedItem(navTarget)
            }
        }

    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()

    }

    private fun finishSettingFragment() {
        val title = if (activeFragment == null) title else activeFragment!!.getTitle(this@OverviewActivity)
        val ab = supportActionBar
        if (ab != null) {
            ab.title = title
        }
        invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
    }

    override fun onBackStackChanged() {
        val fm = supportFragmentManager
        val currentFragment = fm.findFragmentById(mBinding.overviewAppBar!!.overviewContainer.getId())
        var navId = -1
        if (currentFragment is OverviewFragment) {
            navId = R.id.nav_home
        } else if (currentFragment is ProcessModelListOuterFragment) {
            navId = R.id.nav_models
        } else if (currentFragment is TaskListOuterFragment) {
            navId = R.id.nav_tasks
        }
        if (currentFragment is TitleFragment) {
            activeFragment = currentFragment
        }
        if (navId >= 0) {
            mBinding.navView.setCheckedItem(navId)
        }
        finishSettingFragment()

    }

    override fun doAccountDetermined(account: Account?) {
        if (account != null) {
            ProviderHelper.requestSync(this, ProcessModelProvider.AUTHORITY, true)
            ProviderHelper.requestSync(this, TaskProvider.AUTHORITY, true)
        }
    }

    override fun onBackPressed() {
        if (mBinding.overviewDrawer.isDrawerOpen(GravityCompat.START)) {
            mBinding.overviewDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle!!.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        return onNavigationItemSelected(id, true)
    }

    override fun showTasksFragment() {
        mBinding.navView.setCheckedItem(R.id.nav_tasks)
        onNavigationItemSelected(R.id.nav_tasks, true)
        finishSettingFragment()
    }

    override fun showModelsFragment() {
        mBinding.navView.setCheckedItem(R.id.nav_models)
        onNavigationItemSelected(R.id.nav_models, true)
        finishSettingFragment()
    }

    override fun onShowTask(taskId: Long) {
        if (activeFragment is TaskListOuterFragment) {
            (activeFragment as TaskListOuterFragment).showTask(taskId)
        } else {
            activeFragment = TaskListOuterFragment.newInstance(taskId)
            supportFragmentManager.beginTransaction().replace(mBinding.overviewAppBar!!.overviewContainer.getId(),
                                                              activeFragment).addToBackStack("task").commit()
        }
    }

    private fun onNavigationItemSelected(@IdRes id: Int, addToBackstack: Boolean, itemId: Long = 0): Boolean {
        when (id) {
            R.id.nav_home     -> if (activeFragment !is OverviewFragment) {

                activeFragment = OverviewFragment.newInstance()
                val fragmentManager = supportFragmentManager
                @SuppressLint("CommitTransaction")
                val transaction = fragmentManager.beginTransaction()
                    .replace(R.id.overview_container, activeFragment, "home")
                // don't add it to the backstack if there is no child visible yet.
                if (addToBackstack) {
                    transaction.addToBackStack("home")
                }
                transaction.commit()
            }
            R.id.nav_tasks    -> {
                if (activeFragment !is TaskListOuterFragment) {
                    activeFragment = TaskListOuterFragment.newInstance(itemId)
                    @SuppressLint("CommitTransaction")
                    val transaction = supportFragmentManager.beginTransaction()
                        .replace(mBinding.overviewAppBar!!.overviewContainer.getId(), activeFragment, "tasks")
                    if (addToBackstack) {
                        transaction.addToBackStack("tasks")
                    }
                    transaction.commit()
                }
            }
            R.id.nav_models   -> {
                if (activeFragment !is ProcessModelListOuterFragment) {
                    activeFragment = ProcessModelListOuterFragment.newInstance(itemId)
                    @SuppressLint("CommitTransaction")
                    val transaction = supportFragmentManager.beginTransaction()
                        .replace(mBinding.overviewAppBar!!.overviewContainer.getId(), activeFragment, "models")
                    if (addToBackstack) {
                        transaction.addToBackStack("models")
                    }
                    transaction.commit()
                }
            }
            R.id.nav_share    -> {
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        val drawer = mBinding.overviewDrawer
        drawer.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onNameDialogCompletePositive(dialog: GetNameDialogFragment, id: Int, name: String) {
        try {
            ProcessModelProvider.instantiate(this, mModelIdToInstantiate, name)
        } catch (e: RemoteException) {
            Toast.makeText(this, "Unfortunately the process could not be instantiated: " + e.message,
                           Toast.LENGTH_SHORT).show()
        }

    }

    override fun onNameDialogCompleteNegative(dialog: GetNameDialogFragment, id: Int) {
        mModelIdToInstantiate = -1L
    }

    override fun requestSyncProcessModelList(immediate: Boolean, minAge: Long) {
        if (account != null) {
            syncManager!!.requestSyncProcessModelList(immediate, minAge)
        }
    }

    override fun requestSyncTaskList(immediate: Boolean, minAge: Long) {
        if (account != null) {
            syncManager!!.requestSyncTaskList(immediate, minAge)
        }
    }

    override fun onInstantiateModel(modelId: Long, suggestedName: String) {
        mModelIdToInstantiate = modelId
        GetNameDialogFragment.show(supportFragmentManager, DLG_MODEL_INSTANCE_NAME, "Instance name",
                                   "Provide a name for the process instance", this, suggestedName)

    }

    override fun onProcessModelSelected(processModelId: Long) {
        mBinding.navView.setCheckedItem(R.id.nav_models)
        onNavigationItemSelected(R.id.nav_models, true)
        if (activeFragment is ProcessModelListOuterFragment && activeFragment!!.activity != null) {
            val fragment = activeFragment as ProcessModelListOuterFragment?
            fragment!!.onProcessModelSelected(processModelId)
        }

    }

    override fun dismissTaskDetails() {
        val af = activeFragment
        if (af is TaskListOuterFragment) {
            af.onItemSelected(-1, -1)
        }
    }

    companion object {

        private val TAG = "OverviewActivity"
        private val DLG_MODEL_INSTANCE_NAME = 1
        val SERVERPATH_MODELS = "/ProcessEngine/processModels"
        val SERVERPATH_TASKS = "/PEUserMessageHandler/UserMessageService/pendingTasks"
    }
}
