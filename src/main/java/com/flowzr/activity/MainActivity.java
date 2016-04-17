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

import android.app.Activity;
import android.graphics.Typeface;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ListFragment;
import android.support.v4.view.GravityCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import com.flowzr.R;
import com.flowzr.activity.AccountListFragment.OnAccountSelectedListener;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.dialog.WebViewDialog;
import com.flowzr.export.flowzr.FlowzrSyncEngine;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.utils.*;
import com.flowzr.utils.MyPreferences.StartupScreen;
import com.flowzr.view.MyFloatingActionMenu;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;
import static com.flowzr.utils.AndroidUtils.isCompatible;

public class MainActivity  extends AbstractActionBarActivity
        implements OnAccountSelectedListener, MyFragmentAPI {

    static final int CHANGE_PREFERENCES = 600;


    public static Activity activity;


    @Override
    public void onFragmentMessage(String TAG, Bundle data) {
        switch (TAG) {
            case MyFragmentAPI.EDIT_ENTITY_REQUEST:
                editEntityRequest(data);
                break;
            case MyFragmentAPI.REQUEST_BLOTTER:
                requestBlotter(data);
                break;
            case MyFragmentAPI.REQUEST_MYENTITY_FINISH:
                myEntityFinish(data);
                break;
            default:
                Log.e("flowzr", "UNHANDLED TAG :  " + TAG + "data: " + data.toString());
                break;
        }
        mAdapter.notifyDataSetChanged();
    }

    // called for attachpicture, location, filemanager, calc
    public void onFragmentMessage(String TAG, int requestCode,Intent intent, Fragment target) {
        startActivityForResult(intent, requestCode);
    }

    public void startFragmentForResult(Fragment fragment, Fragment target) {
        ensurePaneMode();
        showHideFragment(fragment,target);
    }

    private Fragment getFragmentForClass(String canonicalClassName) {
        Class clazz = null;
        try {
            clazz = Class.forName(canonicalClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            return (Fragment) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }


    private ListFragment getListFragmentForClass(String canonicalClassName) {
        Class clazz = null;
        try {
            clazz = Class.forName(canonicalClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            return (ListFragment) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }


    private void editEntityRequest(Bundle data) {
        if (data.containsKey(ENTITY_CLASS_EXTRA)) {
            Fragment fragment = getFragmentForClass(data.getString(ENTITY_CLASS_EXTRA));
            fragment.setArguments(data);
            loadFragment(fragment);

        } else {
            Log.e("flowzr", "Unhandled message (no ENTITY_CLASS_EXTRA) " + data);
        }
    }

    private void requestBlotter(Bundle data) {
        Fragment fragment;
        if (data.containsKey(MyFragmentAPI.ENTITY_CLASS_EXTRA)) {
            if (data.getString(MyFragmentAPI.ENTITY_CLASS_EXTRA).equals(ReportFragment.class.getCanonicalName())) {
                fragment = getFragmentForClass(BlotterFragment.class.getCanonicalName());
            } else {
                fragment = getFragmentForClass(data.getString(MyFragmentAPI.ENTITY_CLASS_EXTRA));
            }
        }  else {
            fragment = getFragmentForClass(BlotterFragment.class.getCanonicalName());
        }
        if (fragment!=null) {
            fragment.setArguments(data);
            loadFragment(fragment);
        }
    }


    private void myEntityFinish(Bundle data) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment currentFragment = manager.findFragmentById(R.id.fragment_container);
        Fragment target =  currentFragment.getTargetFragment();
        if (target==null) {
            removePaneFragment(currentFragment);
            // from entity list ...
            if (data.getInt(ENTITY_REQUEST_EXTRA)==CategorySelectorFragment.CATEGORY_PICK) {
                requestBlotter(data);
            }
        } else {
            Intent intent = new Intent();
            intent.putExtras(data);
            int resultCode=data.getInt(MyFragmentAPI.RESULT_EXTRA);
            int requestCode=data.getInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA);
           target.onActivityResult(requestCode,resultCode , intent);
            showResultingFragment(target,currentFragment);
        }
    }

    private void loadFragment(Fragment fragment) {
        ensurePaneMode();
        if (fragment.getTargetFragment() == null) {
            replacePaneFragments(fragment);
        } else {
            showHideFragment(fragment,fragment.getTargetFragment());
        }
    }


    private void showResultingFragment(Fragment target, Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!target.isAdded()) {
            transaction.add(R.id.fragment_container, target);
            activePaneFragments.add(target);
        } else {
            transaction.show(target);
        }
        //******
        if (fragment.isAdded()) {
            activePaneFragments.remove(fragment);
            transaction.remove( fragment);
        }
        transaction.addToBackStack(BACKSTACK);
        transaction.commit();
        if (activePaneFragments.size()==0) {
            ensureViewPagerMode();
        }

    }

    private void showHideFragment(Fragment fragment, Fragment target) {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
            activePaneFragments.add(fragment);
        } else {
            transaction.show(fragment);
        }

        if (!target.isAdded()) {
            transaction.add(R.id.fragment_container, target);
        }

        transaction.hide(target);
        transaction.addToBackStack(BACKSTACK);
        transaction.commit();
    }

    private void replacePaneFragments(Fragment fragment) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            activePaneFragments.add(fragment);
            fragmentTransaction.addToBackStack(BACKSTACK);
            fragmentTransaction.commit();
    }

    private void removePaneFragment(Fragment activeFragment) {
        if (activePaneFragments.size() > 0) {
            FragmentTransaction  fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(activeFragment);
            activePaneFragments.remove(activeFragment);
            fragmentTransaction.addToBackStack(BACKSTACK);
            if (activePaneFragments.size() > 0) {
                Fragment f2 = activePaneFragments.get(activePaneFragments.size()-1);
                if (f2.isAdded()) {
                    fragmentTransaction.show(f2);
                } else {
                    fragmentTransaction.add(R.id.fragment_container,f2);
                }
            }
            fragmentTransaction.commit();
        }
        if (activePaneFragments.size() ==0 ) {
            ensureViewPagerMode();
            recreateViewPagerAdapter();
        }
    }

    private void ensurePaneMode() {
        viewPager.setVisibility(View.GONE);
        if ( findViewById(R.id.fragment_container)!=null) {
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        }
        if (isCompatible(14)) {
            MyFloatingActionMenu menu1 = (MyFloatingActionMenu) findViewById(R.id.menu1);
            menu1.hideMenu(true);
        }


        paneMode=true;
    }


    protected void initUI() {
        activity = this;
        setContentView(R.layout.main);
        findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);
        findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);
        initToolbar();
        setupDrawer();

        if (WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)) {
            if (mDrawerLayout != null) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        }
        initialLoad();
        FlowzrSyncEngine.setUpdatable(this);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    // drawer ...
                    case R.id.drawer_item_account:
                        menuItem.setChecked(true);
                        ensureViewPagerMode();
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.drawer_item_blotter:
                        menuItem.setChecked(true);
                        ensureViewPagerMode();
                        viewPager.setCurrentItem(1);
                        break;
                    case R.id.drawer_item_budget:
                        menuItem.setChecked(true);
                        ensureViewPagerMode();
                        viewPager.setCurrentItem(2);
                        break;
                    // fragments ...
                    case R.id.drawer_item_reports:
                        ensurePaneMode();
                        loadFragment(new ReportsListFragment());
                        break;
                    case R.id.drawer_item_entities:
                        ensurePaneMode();
                        loadFragment(new EntityListFragment());
                        break;
                    // activities ...
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();

        recreateViewPagerAdapter();

        StartupScreen startupScreen = MyPreferences.getStartupScreen(this);
        if (savedInstanceState != null) {
            viewPager.setCurrentItem(savedInstanceState.getInt(STATE_TABID, startupScreen.ordinal()));
        } else {
            switch (startupScreen.ordinal()) {
                case 3: // reports
                    ensurePaneMode();
                    Bundle bundle = new Bundle();
                    Fragment f = new ReportsListFragment();
                    f.setArguments(bundle);
                    loadFragment(f);
                    break;
                case 4: // entity list
                    ensurePaneMode();
                    loadFragment(new EntityListFragment());
                    break;
                default:
                    viewPager.setCurrentItem(startupScreen.ordinal());
            }
        }

    }


    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            if (mDrawerLayout != null) {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawers();
                    return;
                }
            }
            if (paneMode) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                switch (activePaneFragments.size()) {
                    case 0:
                        ensureViewPagerMode();
                        break;
                    default:
                        activePaneFragments.remove(f);

                }
                super.onBackPressed();

            } else { // viewpager mode
                if (viewPager.getCurrentItem() > 0 && !paneMode) {
                    ensureViewPagerMode();
                    viewPager.setCurrentItem(viewPager.getCurrentItem()-1);
                    // return; (not needed)
                } else if (viewPager.getCurrentItem() == 0) {
                    // quit app ...
                    Log.e("flowzr", "======= quit app ?");
                    super.onBackPressed();
                }
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PinProtection.unlock(this);
        if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void initialLoad() {
        long t3, t2, t1, t0 = System.currentTimeMillis();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            SQLiteDatabase x = db.db();
            x.beginTransaction();
            t1 = System.currentTimeMillis();
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location));
                x.setTransactionSuccessful();
            } finally {
                x.endTransaction();
            }
            t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
            CurrencyCache.initialize(db.em());
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
            db.close();
        }
        long t4 = System.currentTimeMillis();
        Log.d("Financisto", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms");
    }

    private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
    }

    public void refreshCurrentTab() {
        try {
            Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.pager);
            RefreshSupportedActivity activity = (RefreshSupportedActivity) fragment;
            if (activity != null) {
                viewPager.getAdapter().notifyDataSetChanged();
                viewPager.destroyDrawingCache();
                viewPager.setCurrentItem(viewPager.getCurrentItem());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // out of listener
    public void loadTabFragment(int rId, Bundle bundle, final int tabId) {
        ensureViewPagerMode();
        bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, rId);
        Intent data = new Intent(this, BlotterFragment.class);
        data.putExtras(bundle);
        viewPager.setCurrentItem(tabId);
        try {
            mAdapter.blotterFragment.onActivityResult(BlotterFragment.FILTER_REQUEST, MainActivity.RESULT_OK, data);
        } catch (Exception e) {
            // java.lang.IllegalStateException: Content view not yet created
            e.printStackTrace();
        }

    }

    @Override
    public void onAccountSelected(String title, long id) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, true);
        Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
                .toBundle(title, bundle);
        bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
        loadTabFragment(R.layout.blotter, bundle, 1);
    }
}

