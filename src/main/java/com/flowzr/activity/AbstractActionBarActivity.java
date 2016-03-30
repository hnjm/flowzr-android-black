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
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.flowzr.R;
import com.flowzr.utils.PinProtection;


public class AbstractActionBarActivity  extends AppCompatActivity {
    //private Toolbar actionBar;
    protected static final int CHANGE_PREFERENCES = 600;
    protected static final int ACTIVITY_BACKUP = 800;
	protected DrawerLayout mDrawerLayout;
	protected static ViewPager viewPager;
    protected static MyAdapter mAdapter;
    protected NavigationView navigationView;
    protected boolean isDrawerLocked;
    private ActionBarDrawerToggle mDrawerToggle;


    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this); //Whoo whoo ...
    }

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
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        if (findViewById(R.id.fragment_land_container)!=null) { // in sw600dp-land
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, navigationView);
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


    @Override
    protected void onPause() {
        super.onPause();

        //PinProtection.lock(this);
    }


}