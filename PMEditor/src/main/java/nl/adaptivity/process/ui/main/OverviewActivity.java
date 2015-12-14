package nl.adaptivity.process.ui.main;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.process.editor.android.ProcessModelListOuterFragment;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.ui.task.TaskListOuterFragment;


public class OverviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private nl.adaptivity.process.ui.main.ActivityOverviewBinding mBinding;
  private CharSequence mTitle;
  private TitleFragment mActiveFragment;
  private ActionBarDrawerToggle mDrawerToggle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTitle = getTitle();
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_overview);
    setSupportActionBar(mBinding.overviewAppBar.toolbar);

    DrawerLayout drawer = mBinding.overviewDrawer;
    mDrawerToggle = new ActionBarDrawerToggle(this, drawer, mBinding.overviewAppBar.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        CharSequence title = getActiveFragment()==null ? getTitle() : getActiveFragment().getTitle(OverviewActivity.this);
        ActionBar ab = getSupportActionBar();
        if(ab!=null) { ab.setTitle(title); }
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

      /** Called when a drawer has settled in a completely open state. */
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        ActionBar ab = getSupportActionBar();
        if(ab!=null) { ab.setTitle(mTitle); }
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

    };
    drawer.setDrawerListener(mDrawerToggle);

    NavigationView navigationView = mBinding.navView;
    navigationView.setNavigationItemSelectedListener(this);
  }

  private TitleFragment getActiveFragment() {
    return mActiveFragment;
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.overview_drawer);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    switch (id) {
      case R.id.nav_home:
        // Handle the camera action
        break;
      case R.id.nav_tasks: {
        mActiveFragment = new TaskListOuterFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.overviewFragment, mActiveFragment).commit();

        break;
      }
      case R.id.nav_models: {
        mActiveFragment = new ProcessModelListOuterFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.overviewFragment, mActiveFragment).commit();
      }

        break;
      case R.id.nav_share:

        break;
      case R.id.nav_settings: {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        break;
      }
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.overview_drawer);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }
}
