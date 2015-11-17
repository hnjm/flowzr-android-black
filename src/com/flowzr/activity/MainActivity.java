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
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.flowzr.R;
import com.flowzr.activity.AccountListFragment.OnAccountSelectedListener;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.dialog.WebViewDialog;
import com.flowzr.export.csv.Csv;
import com.flowzr.export.flowzr.FlowzrSyncEngine;
import com.flowzr.filter.Criteria;
import com.flowzr.service.FinancistoService;
import com.flowzr.utils.*;
import com.flowzr.utils.MyPreferences.StartupScreen;

import java.util.List;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;



public class MainActivity  extends AbstractActionBarActivity implements OnAccountSelectedListener,ViewPager.OnPageChangeListener, ActionBar.TabListener {


    static final int CHANGE_PREFERENCES = 6;
    
	public static final int TAB_BLOTTER = 1;

    public static final String REQUEST_BLOTTER = "REQUEST_BLOTTER";
	protected static final String REQUEST_MASSOP = "REQUEST_MASSOP";   
	protected static final  String REQUEST_SCHEDULE = "REQUEST_SCHEDULE";   
	protected static final  String REQUEST_PLANNER = "REQUEST_PLANNER";   
	protected static final  String REQUEST_TEMPLATE = "REQUEST_TEMPLATE";
	public final static String REQUEST_BUDGET_BLOTTER="REQUEST_BUDGET_BLOTTER";
	
	public static final String REQUEST_SPLIT_BLOTTER = "REQUEST_SPLIT_BLOTTER";   
	
	private Tab mAccountTab;
	private Tab mBlotterTab;
	private Tab mBudgetsTab;

    public static Activity activity ;

    private static BlotterFragment blotterFragment;

		/**
	  public final void updateActivityFromBgThread() {
		    runOnUiThread(new Runnable() {
		      @Override
		      public void run() {
		        updateActivity();
		      }});
		  }	
	  **/
		/**
	  public void updateActivity() {
		 
		  
	  }
		**/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //initUI();
        PinProtection.unlock(this);
    }



	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        initUI();
	}

    protected void initUI() {
        activity=this;
        setContentView(R.layout.main);
        actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);


        //@see: http://stackoverflow.com/questions/16539251/get-rid-of-blue-line,
        //only way found to remove on various devices 2.3x, 3.0, ...
		//actionBar.setBackgroundDrawable(new ColorDrawable(R.color.f_dark));


        setupDrawer();


        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // create new tabs and and set up the titles of the tabs
        mAccountTab = setupAccountsTab(actionBar);
        mBlotterTab = setupBlotterTab(actionBar);
        mBudgetsTab = setupBudgetsTab(actionBar);

        Intent intent= getIntent();

        mAdapter = new MyAdapter(getSupportFragmentManager(),intent);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(mAdapter);
        viewPager.setOnPageChangeListener(this);

        mAccountTab.setTabListener(this);
        mBlotterTab.setTabListener(this);
        mBudgetsTab.setTabListener(this);

        addMyTabs();

        if (intent.hasExtra(REQUEST_BLOTTER)) {
            blotterFragment=new BlotterFragment();
            if (intent.hasExtra(EntityListActivity.REQUEST_NEW_TRANSACTION_FROM_TEMPLATE)) {
                Bundle bundle = new Bundle();
                bundle.putInt(BlotterFragment.EXTRA_REQUEST_TYPE, BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
                blotterFragment.setArguments(bundle);
            }
            loadTabFragment(blotterFragment,R.layout.blotter, intent.getExtras(), TAB_BLOTTER);

        } else if (intent.hasExtra(REQUEST_SPLIT_BLOTTER)) {
            loadTabFragment(new SplitsBlotterActivity(),R.layout.blotter, intent.getExtras(), TAB_BLOTTER);
        } else 	if (intent.hasExtra(REQUEST_BUDGET_BLOTTER)) {
            loadTabFragment(new BudgetBlotterFragment(),R.layout.blotter, intent.getExtras(), TAB_BLOTTER);
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
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);    
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);    
	}
	
	
    @Override
    protected void onResume()
    {
		PinProtection.unlock(this);
		super.onResume();
    }
	
	public void addMyTabs() {
		actionBar.addTab(mAccountTab);
		actionBar.addTab(mBlotterTab);
		actionBar.addTab(mBudgetsTab);
	}
           
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		PinProtection.unlock(this);
		super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }
        if (resultCode == MainActivity.RESULT_OK && requestCode == BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            blotterFragment.createTransactionFromTemplate(data);
        }
        refreshCurrentTab();
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
    
    private Tab setupAccountsTab(ActionBar actionBar) {
		Tab aTab = actionBar.newTab(); 
		setMyTabText(aTab,getString(R.string.accounts));
		aTab.setIcon(R.drawable.ic_action_collection);
		return aTab;
    	
    }

    private Tab setupBlotterTab(ActionBar actionBar) {
		Tab aTab = actionBar.newTab();
		setMyTabText(aTab,getString(R.string.blotter));
		aTab.setIcon(R.drawable.ic_action_view_as_list);
		return aTab;    
    }

    private Tab setupBudgetsTab(ActionBar actionBar) {
		Tab aTab = actionBar.newTab();
		setMyTabText(aTab,getString(R.string.budgets));
		aTab.setIcon(R.drawable.ic_action_mail);
		return aTab;
    }
	/**
    private void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            //eventually market is not available
            Toast.makeText(this, R.string.donate_error, Toast.LENGTH_LONG).show();
        }
    }
	*/
   
  	@Override
	public void onAccountSelected(String title, long id) {
		Bundle bundle = new Bundle();
        Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
        	.toBundle(title, bundle);
	    bundle.putBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, true);           
    	ListFragment fragment=new BlotterFragment();
	    bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
	    fragment.setArguments(bundle);
	    loadTabFragment(fragment, R.layout.blotter, bundle, 1);
	}
  	
  	public void setMyTitle(String t) {
  	  SpannableString s = new SpannableString(t);
  	  s.setSpan(new TypefaceSpan("sans-serif"), 0, s.length(),
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      actionBar.setTitle(s);
  	}
	
  	public static class MyAdapter extends FragmentStatePagerAdapter {
  		Bundle bundle;
  	    Fragment fragment=null;

		public MyAdapter(FragmentManager fm, Intent i) {
  	        super(fm);
  	        bundle=i.getExtras();
  	        bundle=new Bundle();  	       
  	    }

  	    public void setFilter(Bundle b) {
  	    	bundle=b;
  	    }
  	    
  	    public void setMyArguments(Fragment f, Bundle b) {
  	    	fragment=f;
			bundle=b;	
		}

		@Override
  	    public int getCount() {
  	        return 3;
  	    }
  	    
  	   @Override
  	    public int getItemPosition(Object object){
		   if (object.getClass().getSimpleName().equals(AccountListFragment.class.getCanonicalName())) {
			   return 0;
		   }
		   if (object.getClass().getSimpleName().equals(BlotterFragment.class.getCanonicalName())) {
			   return 1;
		   }
		   if (object.getClass().getSimpleName().equals(BudgetListFragment.class.getCanonicalName())) {
			   return 2;
		   }
		   return POSITION_NONE;

  	    }
  	    
  	   @Override
  	    public Fragment getItem(int position) {
		   	
  	    	if (position==0) {
  	    		return AccountListFragment.newInstance(bundle);
  	    	}
  	    	if (position==1) {  	    
  	    		Fragment f= BlotterFragment.newInstance(bundle);
  	    		return f;
  	    	}
  	    	if (position==2) {
  	    		return BudgetListFragment.newInstance(bundle);
  	    	}
  	    	return null;
  	    }
  	}

	@Override
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int pos) {
	    switch (pos) {
		    case 0:
		    	setMyTitle(getString(R.string.accounts));
		    	break;
		    case 1:
		    	setMyTitle(getString(R.string.blotter));
		    	break;    		
		    case 2:
		    	setMyTitle(getString(R.string.budgets));
		    	break;	    	
	    }
		PinProtection.unlock(this); //reset time		 
	}


	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		viewPager.setCurrentItem(arg0.getPosition());
	}

	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
        viewPager.setCurrentItem(arg0.getPosition());	
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// 		
	}
	
  	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
				Log.e("flowzr","home");
            	if (mDrawerLayout!=null) {
					Log.e("flowzr","home2");
	                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
	                    mDrawerLayout.closeDrawer(mDrawerList);
	                } else {
						Log.e("flowzr","home3");
	                    mDrawerLayout.openDrawer(mDrawerList);
	                }
            	}
                return true;
            }

        }
		Log.e("flowzr","go to super");
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onBackPressed() {
		if (isTaskRoot()) {

			if (mDrawerLayout!=null) {
				if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
					mDrawerLayout.closeDrawer(mDrawerList);
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


