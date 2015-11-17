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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import com.flowzr.R;
import com.flowzr.adapter.TransactionsListAdapter;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.*;
import com.flowzr.utils.CurrencyCache;

import java.util.Map;

public class BudgetBlotterFragment extends BlotterFragment {
	
	private Map<Long, Category> categories;
	private Map<Long, Project> projects;
	
    public BudgetBlotterFragment() {
		super();
	}
        
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        Bundle args=this.getArguments();
        if (args != null ) {
            blotterFilter = WhereFilter.fromBundle(args);
        }
		db=new DatabaseAdapter(getActivity());
		em=db.em();
		categories = MyEntity.asMap(db.getCategoriesList(true));
		projects = MyEntity.asMap(em.getActiveProjectsList(true));
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	protected Cursor createCursor() {
		long budgetId = blotterFilter.getBudgetId();
		return getBlotterForBudget(budgetId);
	}

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_filter);
        item.setVisible(false);
        item = menu.findItem(R.id.action_mass_op);
        item.setVisible(false);
        item = menu.findItem(R.id.opt_menu_bill);
        item.setVisible(false);
        item = menu.findItem(R.id.opt_menu_month);
        item.setVisible(false);
    }

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TransactionsListAdapter(this.getActivity(), db, cursor);
	}
	
	private Cursor getBlotterForBudget(long budgetId) {
		Budget b = em.load(Budget.class, budgetId);
		String where = Budget.createWhere(b, categories, projects);
		return db.getBlotterWithSplits(where);
	}

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new TotalCalculationTask(this.getActivity(), totalText) {
            @Override
            public Total getTotalInHomeCurrency() {
                long t0 = System.currentTimeMillis();
                try {
                    try {
                        long budgetId = blotterFilter.getBudgetId();
                        Budget b = em.load(Budget.class, budgetId);
                        Total total = new Total(b.getBudgetCurrency());
                        total.balance = db.fetchBudgetBalance(categories, projects, b);
                        return total;
                    } finally {
                        long t1 = System.currentTimeMillis();
                        Log.d("BUDGET TOTALS", (t1-t0)+"ms");
                    }
                } catch (Exception ex) {
                    Log.e("BudgetTotals", "Unexpected error", ex);
                    return Total.ZERO;
                }
            }

            @Override
            public Total[] getTotals() {
                return new Total[0];
            }
        };
    }

}
