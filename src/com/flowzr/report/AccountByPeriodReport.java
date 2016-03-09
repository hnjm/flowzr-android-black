package com.flowzr.report;

import android.content.Context;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper.TransactionColumns;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Account;
import com.flowzr.model.Currency;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly account results.
 * @author Abdsandryk
 */
public class AccountByPeriodReport extends Report2DChart {
	
	/**
	 * Default constructor.
	 * @param em Database Adapted
	 * @param context Context
	 * @param periodLength Period Length
	 * @param currency Currency
	 */
	public AccountByPeriodReport(Context context, MyEntityManager em, int periodLength, Currency currency) {
		super(context, em, periodLength, currency);
	}

	public AccountByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	/* (non-Javadoc)
	 * @see com.flowzr.graph.ReportGraphic2D#getChildrenGraphics()
	 */
	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.flowzr.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long accountId = filterIds.get(currentFilterOrder);
			Account a = em.getAccount(accountId);
			if (a != null) {
				return a.title;
			} else {
				return context.getString(R.string.no_account);
			}
		} else {
			// no category
			return context.getString(R.string.no_account);
		}
	}

	/* (non-Javadoc)
	 * @see com.flowzr.graph.ReportGraphic2D#setFilterIds()
	 */
	@Override
	public void setFilterIds() {
		filterIds = new ArrayList<>();
		currentFilterOrder = 0;
		List<Account> accounts = em.getAllAccountsList();
		if (accounts.size() > 0) {
			Account a;
			for (int i=0; i<accounts.size(); i++) {
				a = accounts.get(i);
				filterIds.add(a.id);
			}
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.from_account_id.name();
	}
	
	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_account);
	}

}
