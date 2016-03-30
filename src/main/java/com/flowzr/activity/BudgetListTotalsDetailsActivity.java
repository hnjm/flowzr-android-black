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
import com.flowzr.db.BudgetsTotalCalculator;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Budget;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.model.Total;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public class BudgetListTotalsDetailsActivity extends AbstractTotalsDetailsFragment  {

    private WhereFilter filter = WhereFilter.empty();
    private BudgetsTotalCalculator calculator;
    
    public BudgetListTotalsDetailsActivity() {
        super(R.string.budget_total_in_currency);
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getActivity().setTitle(R.string.budgets);
    }
    
    @Override
    protected void internalOnCreate() {
        Bundle bundle=getArguments();
        if (bundle != null) {
            filter = WhereFilter.fromBundle(bundle);
        }
    }

    @Override
    protected void prepareInBackground() {
        List<Budget> budgets = em.getAllBudgets(filter);
        calculator = new BudgetsTotalCalculator(db, budgets);
        calculator.updateBudgets(null);
    }

    protected Total getTotalInHomeCurrency() {
        return calculator.calculateTotalInHomeCurrency();
    }

    protected Total[] getTotals() {
        return calculator.calculateTotals();
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
