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


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.flowzr.R;
import com.flowzr.utils.PinProtection;
import com.flowzr.view.MyFloatingActionMenu;

import java.util.ArrayList;
import java.util.List;

import static com.flowzr.utils.AndroidUtils.isCompatible;


public class AbstractActionBarActivity  extends AppCompatActivity {

    protected final String STATE_TABID="tabId";
    protected static final int CHANGE_PREFERENCES = 600;
    protected static final int ACTIVITY_BACKUP = 800;
	protected DrawerLayout mDrawerLayout;
	protected static ViewPager viewPager;
    protected static MyAdapter mAdapter;
    protected NavigationView navigationView;
    protected boolean isDrawerLocked;
    private ActionBarDrawerToggle mDrawerToggle;
    protected boolean paneMode=false;
    protected static final String BACKSTACK = "BACKSTACK";
    private String PANE_MODE="PANE_MODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this); //Whoo whoo ...
    }

    List<Fragment> activePaneFragments = new ArrayList<Fragment>();

    public void ensureViewPagerMode() {
        removePaneFragments();
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        for(int i = 0; i < count; ++i) {
            try {
                fm.popBackStackImmediate(BACKSTACK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (findViewById(R.id.fragment_container)!=null) {
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
        }
        viewPager.setVisibility(View.VISIBLE);

        fm.executePendingTransactions();
        if (mAdapter !=null && mAdapter.blotterFragment!=null) {
            if (!mAdapter.blotterFragment.isAdded()) {
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.add(mAdapter.blotterFragment,mAdapter.blotterFragment.getTag());
                transaction.commit();
                viewPager.setCurrentItem(1);
            } else if (!mAdapter.blotterFragment.isVisible()) {
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.show(mAdapter.blotterFragment);
                transaction.commit();
            }
        }
        try {
            recreateViewPagerAdapter();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mAdapter.blotterFragment!=null && mAdapter.blotterFragment.isAdded()) {
            mAdapter.blotterFragment.setUpFab();
        }

        paneMode=false;
    }

    private void removePaneFragments() {
        try {
            if (activePaneFragments.size() > 0) {
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                for (Fragment activeFragment : activePaneFragments) {
                    fragmentTransaction.remove(activeFragment);
                    activePaneFragments.remove(activeFragment);
                }
                activePaneFragments.clear();
                fragmentTransaction.addToBackStack(MainActivity.BACKSTACK);
                fragmentTransaction.commit();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        //Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        //Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        //Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        //Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        //Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PinProtection.unlock(this);
        if (mDrawerLayout!=null ) {
            mDrawerToggle.onConfigurationChanged(newConfig);
            if (mDrawerLayout.findViewById(R.id.drawer_header)!=null) {
                if (newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE) {
                    mDrawerLayout.findViewById(R.id.drawer_header).setVisibility(View.GONE);
                } else {
                    mDrawerLayout.findViewById(R.id.drawer_header).setVisibility(View.VISIBLE);
                }
            }
        }
        int j=viewPager.getCurrentItem();
        recreateViewPagerAdapter();
        viewPager.setCurrentItem(j);
    }

    protected void recreateViewPagerAdapter() {
        Intent intent= getIntent();
        int pos=0;
        if (viewPager!=null) {
             pos=viewPager.getCurrentItem();
        }

        mAdapter = new MyAdapter(getSupportFragmentManager(),intent);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(mAdapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        setTitle(R.string.accounts);
                        hideFab();
                        break;
                    case 1:
                        setTitle(R.string.blotter);
                        showFab();
                        break;
                    case 2:
                        setTitle(R.string.budgets);
                        hideFab();
                        break;
                }

            }
        });
        viewPager.setCurrentItem(pos);
        mAdapter.notifyDataSetChanged();
    }

    private void hideFab() {
        if (isCompatible(14)) {
            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) findViewById(R.id.menu1);
            if (menu1 != null) {
                menu1.setVisibility(View.GONE);
            }
        }
    }

    private void showFab() {
        if (isCompatible(14)) {
            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) findViewById(R.id.menu1);
            if (menu1 != null) {
                menu1.setVisibility(View.VISIBLE);
            }
        }
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
                blotterFragment = new BlotterFragment();
                return blotterFragment;
            }
            if (position==2) {
                budgetListFragment= (BudgetListFragment) BudgetListFragment.newInstance(bundle);
                return budgetListFragment;
            }
            return null;
        }
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_TABID,viewPager.getCurrentItem());
        savedInstanceState.putBoolean(PANE_MODE,paneMode);
        super.onSaveInstanceState(savedInstanceState);
    }



    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        PinProtection.unlock(this);
        //ensureViewPagerMode();
        //handle setting title after viewpager generate title at loading
//        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                switch (viewPager.getCurrentItem()) {
//                    case 0:
//                        setTitle(R.string.accounts);
//                        break;
//                    case 2:
//                        setTitle(R.string.budgets);
//                        break;
//                }
//            }
//        }, 800);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                if (mDrawerLayout!=null) {
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawers();
                    } else {
                        mDrawerLayout.openDrawer(GravityCompat.START);
                    }
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }
/**
    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            if (mDrawerLayout!=null) {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawers();
                    return;
                }
            }
            if (viewPager.getCurrentItem()!=0) {
                viewPager.setCurrentItem(0);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }
*/

}