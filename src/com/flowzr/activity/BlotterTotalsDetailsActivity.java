/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.os.Bundle;
import android.view.View;

import com.flowzr.R;
import com.flowzr.blotter.AccountTotalCalculationTask;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.blotter.BlotterTotalCalculationTask;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.model.Total;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public class BlotterTotalsDetailsActivity extends AbstractTotalsDetailsFragment  {

    private volatile TotalCalculationTask totalCalculationTask;
	private WhereFilter blotterFilter;

    public BlotterTotalsDetailsActivity() {
        super(R.string.blotter_total_in_currency);
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		String title = blotterFilter.getTitle();
		if (title != null) {
			getActivity().setTitle(title);
		} else {
			getActivity().setTitle(R.string.blotter);
		}
    }
    
    @Override
    protected void internalOnCreate() {
        Bundle b=getArguments();
        blotterFilter = WhereFilter.fromBundle(b);
        cleanupFilter(blotterFilter);
        totalCalculationTask = createTotalCalculationTask(blotterFilter);
    }

    private void cleanupFilter(WhereFilter blotterFilter) {
        blotterFilter.remove(BlotterFilter.BUDGET_ID);
    }

    private TotalCalculationTask createTotalCalculationTask(WhereFilter blotterFilter) {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        if (filter.getAccountId() > 0) {
            shouldShowHomeCurrencyTotal = false;
            return new AccountTotalCalculationTask(this.getActivity(), db, filter, null);
        } else {
            return new BlotterTotalCalculationTask(this.getActivity(), db, filter, null);
        }
    }

    protected Total getTotalInHomeCurrency() {
        return totalCalculationTask.getTotalInHomeCurrency();
    }

    protected Total[] getTotals() {
        return totalCalculationTask.getTotals();
    }

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
