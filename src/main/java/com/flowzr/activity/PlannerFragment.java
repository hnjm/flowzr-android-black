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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.ScheduledListAdapter;
import com.flowzr.datetime.Period;
import com.flowzr.datetime.PeriodType;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Total;
import com.flowzr.utils.FuturePlanner;
import com.flowzr.utils.TransactionList;
import com.flowzr.utils.Utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/20/12 2:22 PM
 */
public class PlannerFragment extends BlotterFragment {

    protected TextView totalText;
    private TextView filterText;

    private WhereFilter filter = WhereFilter.empty();
	private Context thiscontext;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	setHasOptionsMenu(true);
    	thiscontext = container.getContext();
    	return inflater.inflate(R.layout.planner, container, false);
	}

    @Override
    public void onResume() {
        super.onResume();
        loadFilter();
        setupFilter();
        totalText = (TextView)getView().findViewById(R.id.total);
        filterText = (TextView)getView().findViewById(R.id.period);
        recreateAdapter();
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        loadFilter();
        setupFilter();
        totalText = (TextView)getView().findViewById(R.id.total);
        filterText = (TextView)getView().findViewById(R.id.period);
        recreateAdapter();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.blotter_actions, menu);
        menu.findItem(R.id.bAdd).setVisible(false);
        menu.findItem(R.id.bTransfer).setVisible(false);
        menu.findItem(R.id.action_mass_op).setVisible(false);
        menu.findItem(R.id.action_list_template).setVisible(false);
        menu.findItem(R.id.opt_menu_bill).setVisible(false);
        menu.findItem(R.id.opt_menu_month).setVisible(false);

    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_filter: 
	        	showFilter();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    private void loadFilter() {
        SharedPreferences preferences = getActivity().getPreferences(PreferenceActivity.MODE_PRIVATE);
        filter = WhereFilter.fromSharedPreferences(preferences);
        applyDateTimeCriteria(filter.getDateTime());
    }

    private void setupFilter() {
        if (filter.isEmpty()) {
            applyDateTimeCriteria(null);
        }
    }

    private void applyDateTimeCriteria(DateTimeCriteria criteria) {
        if (criteria == null) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            criteria = new DateTimeCriteria(PeriodType.THIS_MONTH);
        }
        long now = System.currentTimeMillis();
        if (now > criteria.getLongValue1()) {
            Period period = criteria.getPeriod();
            period.start = now;
            criteria = new DateTimeCriteria(period);
        }
        filter.put(criteria);
    }

    private void showFilter() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(DateFilterFragment.EXTRA_FILTER_DONT_SHOW_NO_FILTER, true);
        bundle.putBoolean(DateFilterFragment.EXTRA_FILTER_SHOW_PLANNER, true);
        filter.toBundle(bundle);
        bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,FILTER_REQUEST);
        Fragment fragment = new DateFilterFragment();
        fragment.setArguments(bundle);
        fragment.setTargetFragment(this,FILTER_REQUEST);
        activity.startFragmentForResult(fragment,this);
    }

    private void saveFilter() {
        SharedPreferences preferences = getActivity().getPreferences(PreferenceActivity.MODE_PRIVATE);
        filter.toSharedPreferences(preferences);
        SharedPreferences.Editor editor = preferences.edit();
        editor.apply();
    }

    @Override
    protected Cursor createCursor() {
        retrieveData();
        return null;
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK && data!=null) {
            if (data.getStringExtra(DateFilterFragment.EXTRA_FILTER_PERIOD_TYPE)!=null) {
                DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
                applyDateTimeCriteria(c);
                saveFilter();
                retrieveData();
            }
            recreateCursor();
            recreateAdapter();
            retrieveData();
        }
    }

    private PlannerTask task;

    private void retrieveData() {
        if (filter!=null) {
            if (task != null) {
                task.cancel(true);
            }
            if (filter.isEmpty()) {
                filter=WhereFilter.empty();
                applyDateTimeCriteria(null);
            }

            task = new PlannerTask(filter);
            task.execute();
        }
    }

    private class PlannerTask extends AsyncTask<Void, Void, TransactionList> {

        private final WhereFilter filter;

        private PlannerTask(WhereFilter filter) {
            this.filter = WhereFilter.copyOf(filter);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            if (filter.isEmpty()) {
                applyDateTimeCriteria(null);
            }
            FuturePlanner planner = new FuturePlanner(db, filter, new Date());
            return planner.getPlannedTransactionsWithTotals();
        }

        @Override
        protected void onPostExecute(TransactionList data) {
            try {
                ScheduledListAdapter adapter = new ScheduledListAdapter(thiscontext, data.transactions);
                setListAdapter(adapter);
                setTotals(data.totals);
                updateFilterText(filter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void updateFilterText(WhereFilter filter) {
        totalText = (TextView)getView().findViewById(R.id.total);
        filterText = (TextView)getView().findViewById(R.id.period);
        Criteria c = filter.get(DatabaseHelper.ReportColumns.DATETIME);
        if (c != null) {
            filterText.setText(DateUtils.formatDateRange(this.getActivity(), c.getLongValue1(), c.getLongValue2(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
        } else {
            filterText.setText(R.string.no_filter);
        }
		getActivity().setTitle(R.string.planner);
    }

    private void setTotals(Total[] totals) {
        Utils u = new Utils(this.getActivity());
        u.setTotal(totalText, totals[0]);
    }

}
