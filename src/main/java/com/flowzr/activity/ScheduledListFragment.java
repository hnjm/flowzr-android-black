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
import android.os.Handler;
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
import com.flowzr.view.FloatingActionButton;
import com.flowzr.view.MyFloatingActionMenu;

import java.util.ArrayList;
import java.util.List;

import static com.flowzr.utils.AndroidUtils.isCompatible;

public class ScheduledListFragment extends BlotterFragment {

    private RecurrenceScheduler scheduler;

	public ScheduledListFragment() {}
	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
	    inflater.inflate(R.menu.scheduled_actions, menu);
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
        blotterFilter = new WhereFilter("schedules");
        blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
        scheduler = new RecurrenceScheduler(db);
        internalOnCreateTemplates();
        recreateCursor();
        recreateAdapter();


		if (getView().findViewById(R.id.total_layout)!=null) {
            getView().findViewById(R.id.total_layout).setVisibility(View.GONE);
        }

        setUpFab();
        getActivity().setTitle(R.string.scheduled_transactions);
    }

    public void setUpFab() {
        if (isCompatible(14)) {

            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) activity.findViewById(R.id.menu1);
            final FloatingActionButton fab1 = (FloatingActionButton) activity.findViewById(R.id.fab1);
            final FloatingActionButton fab2 = (FloatingActionButton) activity.findViewById(R.id.fab2);

            menu1.setClosedOnTouchOutside(true);
            menu1.setOnMenuButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menu1.toggle(true);
                }
            });

            Handler mUiHandler = new Handler();
            List<MyFloatingActionMenu> menus = new ArrayList<>();

            menus.add(menu1);
            menu1.hideMenuButton(true);
            int delay = 400;
            for (final MyFloatingActionMenu menu : menus) {
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menu.showMenuButton(true);
                    }
                }, delay);
                delay += 150;
            }

            if (fab1!=null) {
                fab1.setLabelText(
                        getResources().getString(R.string.add_transaction)
                                + " "
                                + getResources().getString(R.string.scheduled));
            }

            if (fab2!=null) {
                fab2.setLabelText(
                        getResources().getString(R.string.add_transfer)
                                + " "
                                + getResources().getString(R.string.scheduled));
            }

            fab1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                }
            });

            fab2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                }
            });


        }
    }

	/**
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item= menu.findItem(R.id.action_filter);
		item.setVisible(false);		
		menu.findItem(R.id.action_list_template).setVisible(false);
		item= menu.findItem(R.id.action_mass_op).setVisible(false);
		menu.findItem(R.id.opt_menu_bill).setVisible(false);
		menu.findItem(R.id.opt_menu_month).setVisible(false);		
    }
	**/
    
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
