/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;


import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.utils.*;

public class AbstractActionBarActivity  extends AppCompatActivity {

    private static final int CHANGE_PREFERENCES = 600;
    private static final int ACTIVITY_BACKUP = 800;

	public Toolbar actionBar;
	// Within which the entire activity is enclosed
	protected DrawerLayout mDrawerLayout;
	protected static ViewPager viewPager;
    public static MyAdapter mAdapter;

    private ActionBarDrawerToggle mDrawerToggle;
    private boolean isDrawerLocked;


    public static class MyAdapter extends FragmentPagerAdapter {
        Bundle bundle;
        public static BlotterFragment blotterFragment;
        public static AccountListFragment accountListFragment;
        public static BudgetListFragment budgetListFragment;

        public MyAdapter(FragmentManager fm, Intent i) {
            super(fm);
            bundle=i.getExtras();
        }

        public void setFilter(Bundle b) {
            bundle=b;
        }


        @Override
        public int getCount() {
            return 3;
        }

        //@Override
        //public int getItemPosition(Object object){
        //    return POSITION_NONE; // clear cache
        //}

        @Override
        public Fragment getItem(int position) {

            if (position==0) {
                accountListFragment= (AccountListFragment) AccountListFragment.newInstance(bundle);
                return  accountListFragment;
            }
            if (position==1) {
                blotterFragment = (BlotterFragment) BlotterFragment.newInstance(bundle);
                return blotterFragment;
            }
            if (position==2) {
                budgetListFragment= (BudgetListFragment) BudgetListFragment.newInstance(bundle);
                return budgetListFragment;
            }
            return null;
        }
    }

    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this); //Whoo whoo ...
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

	protected void setupDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_V);
        NavigationView view = (NavigationView) findViewById(R.id.navigation_view);

        if (findViewById(R.id.fragment_land_container)!=null) { // in sw600dp-land
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, view);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mDrawerLayout.openDrawer(GravityCompat.START);
            isDrawerLocked = true;
        }

        if (!isDrawerLocked) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.menu, R.string.close) {
            };
            mDrawerToggle.syncState();
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.drawer_item_account:
                        menuItem.setChecked(true);
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.drawer_item_blotter:
                        menuItem.setChecked(true);
                        viewPager.setCurrentItem(1);
                        mAdapter.notifyDataSetChanged();
                        break;
                    case R.id.drawer_item_budget:
                        menuItem.setChecked(true);
                        viewPager.setCurrentItem(2);
                        break;
                    case R.id.drawer_item_reports:
                        Intent intent = new Intent(getApplicationContext(), EntityListActivity.class);
                        intent.putExtra(EntityListActivity.REQUEST_REPORTS, true);
                        startActivity(intent);
                        break;
                    case R.id.drawer_item_entities:
                        intent = new Intent(getApplicationContext(), EntityListActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.drawer_item_preferences:
                        startActivityForResult(new Intent(getApplicationContext(), PreferencesActivity.class), CHANGE_PREFERENCES);
                        break;
                    case R.id.drawer_item_about:
                        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                        break;
                    case R.id.drawer_item_sync:
                        startActivity(new Intent(getApplicationContext(), FlowzrSyncActivity.class));
                        break;
                    case R.id.drawer_item_backup:
                        startActivityForResult(new Intent(getApplicationContext(), BackupListActivity.class), ACTIVITY_BACKUP);
                        break;
                }
                if (!isDrawerLocked) {
                    mDrawerLayout.closeDrawers();
                }
                return false;
            }
        });
	}


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerLayout!=null && !isDrawerLocked) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PinProtection.unlock(this);
        if (mDrawerLayout!=null ) {
            mDrawerToggle.onConfigurationChanged(newConfig);
            if (newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE) {
                mDrawerLayout.findViewById(R.id.drawer_header).setVisibility(View.GONE);
            } else {
                mDrawerLayout.findViewById(R.id.drawer_header).setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    /**
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // check if any view exists on current view
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        } catch (Exception e) {
            // Button was not found
            // It means, your button doesn't exist on the "current" view
            // It was freed from the memory, therefore stop of activity was performed
            // In this case I restart my app
            Intent i = new Intent();
            i.setClass(getApplicationContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            // Show toast to the user
            Toast.makeText(getApplicationContext(), "Data lost due to excess use of other apps", Toast.LENGTH_LONG).show();
            //finish();
        }
    }
    **/

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("flowzr", "on pause A");
        //PinProtection.lock(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                TaskStackBuilder tsb = TaskStackBuilder.create(this);
                final int intentCount = tsb.getIntentCount();
                if (intentCount > 0)
                {
                    Intent upIntent = tsb.getIntents()[intentCount - 1];
                    if (NavUtils.shouldUpRecreateTask(this, upIntent))
                    {
                        // This activity is not part of the application's task, so create a new task with a synthesized back stack.
                        tsb.startActivities();
                        //finish();
                    }
                    else
                    {
                        // This activity is part of the application's task, so simply navigate up to the hierarchical parent activity.
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                }
                else
                {
                    onBackPressed();
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }
}