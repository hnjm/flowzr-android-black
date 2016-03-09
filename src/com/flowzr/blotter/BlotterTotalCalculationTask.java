/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.flowzr.blotter;

import android.content.Context;
import android.widget.TextView;

import com.flowzr.datetime.DateUtils;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.TransactionsTotalCalculator;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Total;
import com.flowzr.utils.MyPreferences;

import java.util.Calendar;

public class BlotterTotalCalculationTask extends TotalCalculationTask {

	private final DatabaseAdapter db;
	private final WhereFilter filter;

	public BlotterTotalCalculationTask(Context context, DatabaseAdapter db, WhereFilter filter, TextView totalText) {
        super(context, totalText);
		this.db = db;
		this.filter = filter;
		if (MyPreferences.useTodayForTotal(context)) {
	        Calendar c = Calendar.getInstance();
	        long end = DateUtils.endOfDay(c).getTimeInMillis();
	        this.filter.put(new DateTimeCriteria(0, end));
		}
	}

    @Override
    public Total getTotalInHomeCurrency() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getBlotterBalanceInHomeCurrency();
    }

    @Override
    public Total[] getTotals() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getTransactionsBalance();
    }

}
