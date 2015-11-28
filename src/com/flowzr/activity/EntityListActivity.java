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

import com.flowzr.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;

import javax.persistence.Entity;


public class EntityListActivity extends AppCompatActivity {

	public final static String REQUEST_BLOTTER_TOTALS="REQUEST_BLOTTER_TOTALS";
	public final static String REQUEST_BLOTTER="REQUEST_BLOTTER";
	public final static String REQUEST_MASS_OP="REQUEST_MASSOP";
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
		getSupportActionBar().setTitle(s);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		fragment.onActivityResult(requestCode, resultCode, data);
	}


	public void loadFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		//transaction.addToBackStack(null);
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
	    Intent intent=getIntent();
	    if (intent.hasExtra(ReportsListFragment.EXTRA_REPORT_TYPE)
	    		|| intent.hasExtra(REQUEST_REPORTS)
	    		|| intent.hasExtra(REQUEST_PLANNER)) {
	    	setContentView(R.layout.main_reports);  	    	
	    } else {
	    	setContentView(R.layout.main_entities);  
	    }
		initToolbar();


		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (intent.hasExtra(ReportsListFragment.EXTRA_REPORT_TYPE)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.report);
				Fragment f= new ReportFragment();
				f.setArguments(intent.getExtras());
				transaction.replace(R.id.fragment_container,f);			
		} else if (intent.hasExtra(MainActivity.REQUEST_SPLIT_BLOTTER)) {
			Fragment f= new BudgetBlotterFragment();
			f.setArguments(intent.getExtras());
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
			transaction.replace(R.id.fragment_container,f);          	
        } else 	if (intent.hasExtra(REQUEST_BUDGET_BLOTTER)) {
			Fragment f= new BudgetBlotterFragment();
			f.setArguments(intent.getExtras());
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
			transaction.replace(R.id.fragment_container,f);
		} else 	if (intent.hasExtra(REQUEST_BLOTTER)) {
			Fragment f= new BlotterFragment();
			f.setArguments(intent.getExtras());
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter);
			transaction.replace(R.id.fragment_container,f);
		} else if (intent.hasExtra(REQUEST_REPORTS)) {
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.reports_list);
			transaction.replace(R.id.fragment_container,new ReportsListFragment());						
		} else if (intent.hasExtra(REQUEST_EXCHANGE_RATES)) {
			intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.reports_list);
			transaction.replace(R.id.fragment_container,new ExchangeRatesListFragment());			
		} else if (intent.hasExtra(REQUEST_CATEGORY_SELECTOR)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.category_selector);
				transaction.replace(R.id.fragment_container,new CategorySelectorFragment());
			} else if (intent.hasExtra(REQUEST_SCHEDULED)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.scheduled_transactions);
				transaction.replace(R.id.fragment_container, new ScheduledListActivity());
			} else if (intent.hasExtra(REQUEST_PLANNER)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.planner);
				if (findViewById(R.id.fragment_land_container)!=null) {
					transaction.replace(R.id.fragment_land_container,new PlannerActivity());			
				} else {
					transaction.replace(R.id.fragment_container,new PlannerActivity());		
				}
			} else if (intent.hasExtra(REQUEST_MASS_OP)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.blotter_mass_op);
				transaction.replace(R.id.fragment_container,new MassOpActivity());			
			}	else if (intent.hasExtra(REQUEST_TEMPLATES)) {	
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.templates);
				transaction.replace(R.id.fragment_container,new TemplatesListFragment());			
			}	else if (intent.hasExtra(REQUEST_NEW_TRANSACTION_FROM_TEMPLATE)) {
				intent.putExtra(AbstractTotalListFragment.EXTRA_LAYOUT, R.layout.templates);
				transaction.replace(R.id.fragment_container,new SelectTemplateActivity());			
			}	 else if (intent.hasExtra(REQUEST_BLOTTER_TOTALS)) {
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
			transaction.replace(R.id.fragment_container,new EntityListFragment());
		}
		transaction.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case android.R.id.home: {
				super.onBackPressed();
			}
		}
		return super.onOptionsItemSelected(item);
	}

}
