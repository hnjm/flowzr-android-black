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

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.flowzr.R;
import com.flowzr.adapter.BlotterListAdapter;
import com.flowzr.filter.WhereFilter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.utils.EnumUtils;
import com.flowzr.utils.LocalizableEnum;

import java.util.Arrays;

public class MassOpActivity extends BlotterFragment {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

		long accountId = blotterFilter.getAccountId();
		final MassOp[] operations = MassOp.values();
		final Spinner spOperation = (Spinner)getView().findViewById(R.id.spOperation);
		switch (item.getItemId()) {
			case R.id.action_cancel:
				getActivity().finish();			
				return true;
			case R.id.bCheckAll:
				((BlotterListAdapter)getListAdapter()).checkAll();				
				return true;
			case R.id.bUnCheckAll:
				((BlotterListAdapter)getListAdapter()).uncheckAll();				
				return true;
			case R.id.proceed:
				if (spOperation.getSelectedItemPosition()>0) {
					MassOp op = operations[spOperation.getSelectedItemPosition() - 1];
					applyMassOp(op);
				} else {
					Toast.makeText(this.getActivity(),getResources().getString(R.string.select)
							+ " " + getResources().getString(R.string.mass_operations),  Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.action_filter:
				Intent intent = new Intent(getActivity(), BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
				startActivityForResult(intent, FILTER_REQUEST);				
				return true;
		}
		return super.onOptionsItemSelected(item);
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    menu.clear();
	    inflater.inflate(R.menu.blotter_massop_actions, menu);

	}  

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.blotter_mass_op, container, false);
	}
    
	
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		((EntityListActivity) getActivity()).setMyTitle(getResources().getString(R.string.mass_operations));
		Intent intent = getActivity().getIntent();
		if (intent != null) {			
			blotterFilter = WhereFilter.fromIntent(intent);
		}
				
		intent = getActivity().getIntent();
		if (intent != null) {			
			blotterFilter = WhereFilter.fromIntent(intent);
			//applyFilter();
		}
		final MassOp[] operations = MassOp.values();
		final Spinner spOperation = (Spinner)getView().findViewById(R.id.spOperation);
		spOperation.setPrompt(getString(R.string.mass_operations));
		spOperation.setAdapter(EnumUtils.createSpinnerAdapter(getActivity(), operations));		
		LinearLayout l = (LinearLayout)getView().findViewById(R.id.total_text);
		l.setVisibility(LinearLayout.GONE);		
		recreateAdapter();
        prepareActionGrid();

		((EntityListActivity) getActivity()).getSupportActionBar().invalidateOptionsMenu();
	}
	
	protected void applyMassOp(final MassOp op) {
		int count = ((BlotterListAdapter)getListAdapter()).getCheckedCount();
		if (count > 0) {
			new AlertDialog.Builder(this.getActivity())
			.setMessage(getString(R.string.apply_mass_op, getString(op.getTitleId()), count))
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					BlotterListAdapter adapter = ((BlotterListAdapter)getListAdapter()); 
					long[] ids = adapter.getAllCheckedIds();
					Log.d("Financisto", "Will apply "+op+" on "+Arrays.toString(ids));
					op.apply(db, ids);
					adapter.uncheckAll();
					adapter.changeCursor(createCursor());
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
		} else {
			Toast.makeText(this.getActivity(), R.string.apply_mass_op_zero_count, Toast.LENGTH_SHORT).show();			
		}
	}


	
	@Override
	protected void calculateTotals() {
		// do nothing
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new BlotterListAdapter(this.getActivity(), db, R.layout.blotter_mass_op_list_item, cursor, true);
	}

	private enum MassOp implements LocalizableEnum{
		CLEAR(R.string.mass_operations_clear_all){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.clearSelectedTransactions(ids);
			}			
		}, 
		RECONCILE(R.string.mass_operations_reconcile){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.reconcileSelectedTransactions(ids);
			}			
		}, 
		DELETE(R.string.mass_operations_delete){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.deleteSelectedTransactions(ids);
                db.rebuildRunningBalances();
			}
		};
		
		private final int titleId;
		
		MassOp(int titleId) {
			this.titleId = titleId;
		}

		public abstract void apply(DatabaseAdapter db, long[] ids);

		@Override
		public int getTitleId() {
			return titleId;
		}
	}
	
}
