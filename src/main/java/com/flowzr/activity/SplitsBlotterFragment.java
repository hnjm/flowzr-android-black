/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.flowzr.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import com.flowzr.adapter.TransactionsListAdapter;
import com.flowzr.blotter.BlotterTotalCalculationTask;
import com.flowzr.blotter.TotalCalculationTask;

public class SplitsBlotterFragment extends BlotterFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bFilter.setVisibility(View.GONE);
	}
	
	@Override
	protected Cursor createCursor() {
        return db.getBlotterForAccountWithSplits(blotterFilter);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TransactionsListAdapter(this.getActivity(), db, cursor);
	}

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new BlotterTotalCalculationTask(this.getActivity(), db, blotterFilter, totalText);
    }

}
