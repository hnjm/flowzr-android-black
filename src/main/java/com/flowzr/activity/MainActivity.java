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
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
import com.flowzr.utils.*;
import com.flowzr.utils.MyPreferences.StartupScreen;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;

public class MainActivity  extends AbstractActionBarActivity
        implements OnAccountSelectedListener, FragmentAPI {

    static final int CHANGE_PREFERENCES = 600;
	public static final int TAB_BLOTTER = 1;
	public static final String REQUEST_SPLIT_BLOTTER = "REQUEST_SPLIT_BLOTTER";
	public final static String REQUEST_BLOTTER_TOTALS="REQUEST_BLOTTER_TOTALS";

	public final static String REQUEST_MASS_OP = "REQUEST_MASSOP";
	public final static String REQUEST_TEMPLATES="REQUEST_TEMPLATES";
	public final static String REQUEST_EXCHANGE_RATES="REQUEST_EXCHANGE_RATES";
	public final static String REQUEST_BUDGET_BLOTTER="REQUEST_BUDGET_BLOTTER";

	public final static String REQUEST_PLANNER="REQUEST_PLANNER";
	public final static String REQUEST_CATEGORY_SELECTOR="REQUEST_CATEGORY_SELECTOR";
	public final static String REQUEST_SCHEDULED="REQUEST_SCHEDULED";
	public final static String REQUEST_NEW_TRANSACTION_FROM_TEMPLATE="REQUEST_NEW_TRANSACTION_FROM_TEMPLATE";
	public final static String REQUEST_BUDGET_TOTALS="REQUEST_BUDGET_TOTALS";
	public final static String REQUEST_ACCOUNT_TOTALS="REQUEST_ACCOUNT_TOTALS";

    public static Activity activity ;

    protected void initUI() {
		activity=this;
		setContentView(R.layout.main);
		findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);
		initToolbar();
		setupDrawer();

		if (WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)) {
			if (mDrawerLayout!=null) {
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
                    // fragments ...
					case R.id.drawer_item_reports:
                        loadFragment(new ReportsListFragment());
						break;
					case R.id.drawer_item_entities:
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
            viewPager.setCurrentItem(savedInstanceState.getInt(STATE_TABID,startupScreen.ordinal()));
        } else {
            switch (startupScreen.ordinal()) {
                case 3: // reports
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(EntityListActivity.REQUEST_REPORTS, true);
                    Fragment f = new EntityListFragment();
                    f.setArguments(bundle);
                    loadFragment(f);
                    break;
                case 4: // entity list
                    loadFragment(new EntityListFragment());
                    break;
                default:
                    viewPager.setCurrentItem(startupScreen.ordinal());
            }
        }

    }

    @Override
    public void onFragmentMessage(String TAG, Bundle data) {
        Log.e("flowzr","onFramgementMessage (bundle) " + TAG);
        Log.e("flowzr","data: " + data.toString());
        if (TAG.equals(FragmentAPI.REQUEST_REPORTS)){
            if (data.getString(FragmentAPI.EXTRA_REPORT_TYPE)!=null) {
                if (data.getBoolean(FragmentAPI.CONVENTIONAL_REPORTS,true)) {
                    Fragment fragment= new ReportFragment();
                    fragment.setArguments(data);
                    loadFragment(fragment);
                } else {
                    Fragment fragment= new Report2DChartFragment();
                    fragment.setArguments(data);
                    loadFragment(fragment);
                }
            } else {
                Log.e("flowzr","no report type should load list or entities" );
            }

        } else if (TAG.equals(FragmentAPI.REQUEST_BLOTTER)){
            //Do something with 'data' that comes from fragment2
        }
    }

    @Override
    public void onFragmentMessage(int requestCode, int resultCode, Intent data) {
        Log.e("flowzr","onFragmentMessage requestCode, resultCode " + String.valueOf(requestCode) + " " + String.valueOf(resultCode));
        Log.e("flowzr","data: " + data.toString());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		PinProtection.unlock(this);
        if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }
		if (resultCode != MainActivity.RESULT_CANCELED) {
            onFragmentMessage(requestCode,resultCode,data);
			setResult(RESULT_OK);
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

	public void loadFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}

    // out of listener
	public void loadTabFragment( int rId, Bundle bundle, final int tabId) {
		bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, rId);
		Intent data=new Intent(this,BlotterFragment.class);
		data.putExtras(bundle);
		viewPager.setCurrentItem(tabId);
        mAdapter.blotterFragment.onActivityResult(BlotterFragment.FILTER_REQUEST, MainActivity.RESULT_OK, data);
	}

    @Override
    public void onAccountSelected(String title, long id) {
        Bundle bundle=new Bundle();
        bundle.putBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, true);
        Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
                .toBundle(title, bundle);
        bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
        loadTabFragment(R.layout.blotter, bundle, 1);
    }

}


