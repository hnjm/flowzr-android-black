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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.ScheduledListAdapter;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.TransactionInfo;
import com.flowzr.service.RecurrenceScheduler;

import java.util.ArrayList;

public class ScheduledListFragment extends BlotterFragment {

    private RecurrenceScheduler scheduler;

	public ScheduledListFragment() {}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
	    inflater.inflate(R.menu.blotter_actions, menu);
	}   
	
    @Override
	protected void calculateTotals() {
		// do nothing
	}
	
	@Override
	protected Cursor createCursor() {
		return null;
	}
	
	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		if (scheduler!=null) {
			ArrayList<TransactionInfo> transactions = scheduler.getSortedSchedules(System.currentTimeMillis());
        	return new ScheduledListAdapter(this.getActivity(), transactions);
		} else {
			return new ScheduledListAdapter(this.getActivity(), new ArrayList<TransactionInfo>());
		}
	}

    @Override
    public void recreateCursor() {
        long now = System.currentTimeMillis();
        if (scheduler!=null) {
	        ArrayList<TransactionInfo> transactions = scheduler.scheduleAll(this.getActivity(), now);
	        updateAdapter(transactions);
        }
    }

    private void updateAdapter(ArrayList<TransactionInfo> transactions) {
		((ScheduledListAdapter)adapter).setTransactions(transactions);
	}

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);    
		blotterFilter = new WhereFilter("schedules");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));		
    }
	
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getActivity().setTitle(R.string.scheduled_transactions);
		blotterFilter = new WhereFilter("schedules");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));			
		scheduler = new RecurrenceScheduler(db);	
		internalOnCreateTemplates();
		recreateCursor();
		recreateAdapter();
		//if (getView().findViewById(R.id.total_text_layout)!=null) {
		//	getView().findViewById(R.id.total_text_layout).setVisibility(View.GONE);
		//}
		//if (getView().findViewById(R.id.bAddTransfer)!=null) {
		//	getView().findViewById(R.id.bAddTransfer).setVisibility(View.GONE);
		//}
		if (getView().findViewById(R.id.total_layout)!=null) {
			getView().findViewById(R.id.total_layout).setVisibility(View.GONE);
		}
		if (getView().findViewById(R.id.fragment_land_container)!=null) {
			getView().findViewById(R.id.fragment_land_container).setVisibility(View.GONE);
		}    		
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item= menu.findItem(R.id.action_filter);
		item.setVisible(false);		
		menu.findItem(R.id.action_list_template).setVisible(false);
		item= menu.findItem(R.id.action_mass_op).setVisible(false);
		menu.findItem(R.id.opt_menu_bill).setVisible(false);
		menu.findItem(R.id.opt_menu_month).setVisible(false);		
    }
    
	protected void internalOnCreateTemplates() {
		blotterFilter = new WhereFilter("schedules");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == MainActivity.RESULT_OK) {
			recreateCursor();
		}
	}

    @Override
    protected void afterDeletingTransaction(long id) {
        super.afterDeletingTransaction(id);
        scheduler.cancelPendingIntentForSchedule(this.getActivity(), id);
    }

    @Override
    public void integrityCheck() {
        new InstalledOnSdCardCheckTask(this.getActivity()).execute();
    }

}
