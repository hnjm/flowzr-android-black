/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.flowzr.R;

public class EntityListActivity extends AbstractActionBarActivity {

	public final static String REQUEST_BLOTTER_TOTALS="REQUEST_BLOTTER_TOTALS";
    public final static String REQUEST_BLOTTER = "REQUEST_BLOTTER"; // from reports
    public final static String REQUEST_MASS_OP = "REQUEST_MASSOP";
    public final static String REQUEST_TEMPLATES="REQUEST_TEMPLATES";
	public final static String REQUEST_EXCHANGE_RATES="REQUEST_EXCHANGE_RATES";
	public final static String REQUEST_BUDGET_BLOTTER="REQUEST_BUDGET_BLOTTER";
	public final static String REQUEST_REPORTS="REQUEST_REPORTS";
	public final static String REQUEST_PLANNER="REQUEST_PLANNER";
	public final static String REQUEST_CATEGORY_SELECTOR="REQUEST_CATEGORY_SELECTOR";
	public final static String REQUEST_SCHEDULED="REQUEST_SCHEDULED";
	public final static String REQUEST_NEW_TRANSACTION_FROM_TEMPLATE="REQUEST_NEW_TRANSACTION_FROM_TEMPLATE";
	public final static String REQUEST_BUDGET_TOTALS="REQUEST_BUDGET_TOTALS";
	public final static String REQUEST_ACCOUNT_TOTALS="REQUEST_ACCOUNT_TOTALS";


	public void setMyTitle(String t) {
		SpannableString s = new SpannableString(t);
		s.setSpan(new TypefaceSpan("sans-serif"), 0, s.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		if (getSupportActionBar()!=null) {
			getSupportActionBar().setTitle(s);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		fragment.onActivityResult(requestCode, resultCode, data);
		setResult(RESULT_OK);
	}


	public void loadFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.addToBackStack(null);
		transaction.commitAllowingStateLoss();
	}

	protected void initToolbar() {
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Toast.makeText(this, R.string.integrity_fix_in_progress, Toast.LENGTH_SHORT).show();
		setContentView(R.layout.main_entities);


/**
 * 		Intent intent=getIntent();
 if (intent.hasExtra(FragmentAPI.EXTRA_REPORT_TYPE)
 || intent.hasExtra(REQUEST_REPORTS)
 || intent.hasExtra(REQUEST_PLANNER)) {
 setContentView(R.layout.main_reports);
 } else {

 }
		initToolbar();
		setupDrawer();

		// if (navigationView != null) {
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {

				switch (menuItem.getItemId()) {
					case R.id.drawer_item_account:
						menuItem.setChecked(true);
						viewPager.setCurrentItem(0);
                        finish();
						break;
					case R.id.drawer_item_blotter:
						menuItem.setChecked(true);
						viewPager.setCurrentItem(1);
						mAdapter.notifyDataSetChanged();
						finish();
                        break;
					case R.id.drawer_item_budget:
						menuItem.setChecked(true);
						viewPager.setCurrentItem(2);
						finish();
                        break;
					case R.id.drawer_item_reports:
						Intent intent = new Intent(getApplicationContext(), EntityListActivity.class);
						intent.putExtra(EntityListActivity.REQUEST_REPORTS, true);
						startActivity(intent);
						break; // call finish();
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

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (intent.hasExtra(FragmentAPI.EXTRA_REPORT_TYPE)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.report);
				Fragment f= new ReportFragment();
				f.setArguments(intent.getExtras());
				transaction.replace(R.id.fragment_container, f);
            transaction.addToBackStack(null);
        }

		if (intent.hasExtra(MainActivity.REQUEST_BLOTTER)) { // from reports
            Fragment f = new BlotterFragment();
            f.setArguments(intent.getExtras());
            intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
            transaction.replace(R.id.fragment_container, f);
            // that time add to backstack because
            // another fragment follows
            // // not that time : transaction.addToBackStack(null);
        } else if (intent.hasExtra(MainActivity.REQUEST_SPLIT_BLOTTER)) {
            // @TODO split blotter fragment
            Fragment f = new BudgetBlotterFragment();
            f.setArguments(intent.getExtras());
            intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
			transaction.replace(R.id.fragment_container, f);
			transaction.addToBackStack(null);
        } else 	if (intent.hasExtra(REQUEST_BUDGET_BLOTTER)) {
			Fragment f= new BudgetBlotterFragment();
			f.setArguments(intent.getExtras());
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
			transaction.replace(R.id.fragment_container, f);
			//transaction.addToBackStack(null);
		} else if (intent.hasExtra(REQUEST_REPORTS)) {
			Log.e("flowzr","calling report list fragment");
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.reports_list);
			transaction.replace(R.id.fragment_container, new ReportsListFragment());
			//transaction.addToBackStack(null);
		} else if (intent.hasExtra(REQUEST_EXCHANGE_RATES)) {
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.reports_list);
			transaction.replace(R.id.fragment_container,new ExchangeRatesListFragment());
			transaction.addToBackStack(null);
		} else if (intent.hasExtra(REQUEST_CATEGORY_SELECTOR)) {
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.category_selector);
			transaction.replace(R.id.fragment_container,new CategorySelectorFragment());
			//transaction.addToBackStack(null);
			} else if (intent.hasExtra(REQUEST_SCHEDULED)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.scheduled_transactions);
			transaction.replace(R.id.fragment_container, new ScheduledListFragment());
			//transaction.addToBackStack(null);
			} else if (intent.hasExtra(REQUEST_PLANNER)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.planner);
				if (findViewById(R.id.fragment_land_container)!=null) {
					transaction.replace(R.id.fragment_land_container,new PlannerFragment());
				} else {
					transaction.replace(R.id.fragment_container,new PlannerFragment());
				}
			} else if (intent.hasExtra(REQUEST_MASS_OP)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter_mass_op);
			    transaction.replace(R.id.fragment_container,new MassOpFragment());
			}	else if (intent.hasExtra(REQUEST_TEMPLATES)) {	
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.templates);
			    transaction.replace(R.id.fragment_container,new TemplatesListFragment());
			    transaction.addToBackStack(null);
			}	else if (intent.hasExtra(REQUEST_NEW_TRANSACTION_FROM_TEMPLATE)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.templates);
			    transaction.replace(R.id.fragment_container,new SelectTemplateFragment());
			    //transaction.addToBackStack(null);
		    }	 else if (intent.hasExtra(REQUEST_BLOTTER_TOTALS)) { //Total used for
				Fragment f= new BlotterTotalsDetailsFragment();
				f.setArguments(intent.getExtras());
				transaction.replace(R.id.fragment_container,f);									
			} else if (intent.hasExtra(REQUEST_ACCOUNT_TOTALS)) {
				Fragment f= new AccountListTotalsDetailsActivity();
				f.setArguments(intent.getExtras());
				transaction.replace(R.id.fragment_container,f);									
			} else if (intent.hasExtra(REQUEST_BUDGET_TOTALS)) {
				Fragment f= new BudgetListTotalsDetailsActivity();
				f.setArguments(intent.getExtras());
				transaction.replace(R.id.fragment_container,f);									
			}
		else {
            Log.d("flowzr","Loading default entity list ?");
			transaction.replace(R.id.fragment_container,new EntityListFragment());
		}
		transaction.commit();

		**/
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        if (mDrawerLayout!=null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers();
                return;
            }
        }
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof ReportFragment) {
            if (((ReportFragment) f).viewingPieChart) {
                ((ReportFragment) f).selectReport();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

}
