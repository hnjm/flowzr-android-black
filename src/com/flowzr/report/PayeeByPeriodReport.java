package com.flowzr.report;

import android.content.Context;
import com.flowzr.R;
import com.flowzr.db.DatabaseHelper.TransactionColumns;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Currency;
import com.flowzr.model.Payee;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly results by Payees.
 * @author Denis Solonenko
 */
public class PayeeByPeriodReport extends Report2DChart {

	public PayeeByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long payeeId = filterIds.get(currentFilterOrder);
			Payee payee = em.get(Payee.class, payeeId);
			if (payee != null) {
				return payee.getTitle();
			} else {
				return context.getString(R.string.no_payee);
			}
		} else {
			// no payee
			return context.getString(R.string.no_payee);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	public void setFilterIds() {
		filterIds = new ArrayList<Long>();
		currentFilterOrder = 0;
		List<Payee> payees = em.getAllPayeeList();
		if (payees.size() > 0) {
            for (Payee p : payees) {
                filterIds.add(p.getId());
            }
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.payee_id.name();
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_payee);
	}
}
