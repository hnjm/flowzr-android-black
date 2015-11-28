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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
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

import java.util.List;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;



public class MainActivity  extends AbstractActionBarActivity implements OnAccountSelectedListener {


    static final int CHANGE_PREFERENCES = 6;
    
	public static final int TAB_BLOTTER = 1;

    public static final String REQUEST_BLOTTER = "REQUEST_BLOTTER";
	public final static String REQUEST_BUDGET_BLOTTER="REQUEST_BUDGET_BLOTTER";
	public static final String REQUEST_SPLIT_BLOTTER = "REQUEST_SPLIT_BLOTTER";
    public static Activity activity ;



	protected void initUI() {
		activity=this;
		setContentView(R.layout.main);
		findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);

		Intent intent= getIntent();

		mAdapter = new MyAdapter(getSupportFragmentManager(),intent);
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setId(1);
		viewPager.setAdapter(mAdapter);
		viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) {}
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

			public void onPageSelected(int position) {
				switch (position) {
					case 0:
						setTitle(R.string.accounts);
						break;
					case 2:
						setTitle(R.string.budgets);
						break;
				}
			}
		});


		setupDrawer();
		initToolbar();

		if (intent.hasExtra(REQUEST_BLOTTER) ) {
				loadTabFragment(R.layout.blotter, intent.getExtras(), TAB_BLOTTER);
		} else if (intent.hasExtra(REQUEST_SPLIT_BLOTTER)) {
			//loadTabFragment(new SplitsBlotterActivity(),R.layout.blotter, intent.getExtras(), TAB_BLOTTER);
		} else 	if (intent.hasExtra(REQUEST_BUDGET_BLOTTER)) {
			//loadTabFragment(new BudgetBlotterFragment(),R.layout.blotter, intent.getExtras(), TAB_BLOTTER);
		} else {
			StartupScreen startupScreen=MyPreferences.getStartupScreen(this);
			viewPager.setCurrentItem(startupScreen.ordinal());
		}

		if (WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)) {
			if (mDrawerLayout!=null) {
				mDrawerLayout.openDrawer(Gravity.LEFT);
			}
		}
		initialLoad();
		FlowzrSyncEngine.setUpdatable(this);
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        initUI();
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
    protected void onResume()
    {
		PinProtection.unlock(this);
		super.onResume();
    }


           
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		PinProtection.unlock(this);
    	if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }

        if (resultCode == MainActivity.RESULT_OK && requestCode == BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
			viewPager.setCurrentItem(TAB_BLOTTER);
			mAdapter.blotterFragment.onActivityResult(requestCode, resultCode, data);
        }

		if (resultCode == MainActivity.RESULT_OK && requestCode == BlotterFragment. FILTER_REQUEST) {
			mAdapter.blotterFragment.onActivityResult(requestCode, resultCode, data);
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
				activity.recreateCursor();
				activity.integrityCheck();
				viewPager.getAdapter().notifyDataSetChanged();
				viewPager.destroyDrawingCache();
				viewPager.setCurrentItem(viewPager.getCurrentItem());
			}
		} catch (Exception e) {
			//pass
		}
    }
    
    private void setMyTabText(Tab tab, String text) {
    	SpannableString s = new SpannableString(text);
    	s.setSpan(new TypefaceSpan("sans-serif"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	tab.setText(s);
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



}


