/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.flowzr.R;
import com.flowzr.adapter.ReportListAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Currency;
import com.flowzr.report.Report;
import com.flowzr.report.ReportType;


public class ReportsListFragment extends ListFragment {

    private MainActivity activity;

	public final static ReportType[] reports = new ReportType[]{
			ReportType.BY_PERIOD,
			ReportType.BY_CATEGORY,
            ReportType.BY_PAYEE,
			ReportType.BY_LOCATION,
			ReportType.BY_PROJECT,
			ReportType.BY_ACCOUNT_BY_PERIOD, 
			ReportType.BY_CATEGORY_BY_PERIOD,
            ReportType.BY_PAYEE_BY_PERIOD,
			ReportType.BY_LOCATION_BY_PERIOD,
			ReportType.BY_PROJECT_BY_PERIOD
	};

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
    	return inflater.inflate(R.layout.entity_list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		getActivity().setTitle(R.string.reports);
		setListAdapter(new ReportListAdapter(this.getView().getContext(), reports));
	}

	@Override
    public void onAttach(Context c) {
		super.onAttach(c);
        activity = (MainActivity) c;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        Bundle bundle=new Bundle();
        if (reports[position].isConventionalBarReport()) {
            bundle.putBoolean(MyFragmentAPI.CONVENTIONAL_REPORTS, true);
            bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, ReportFragment.class.getCanonicalName());
        } else {
            bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, Report2DChartFragment.class.getCanonicalName());
            bundle.putInt(Report2DChart.REPORT_TYPE, position);
        }
        bundle.putString(MyFragmentAPI.EXTRA_REPORT_TYPE, reports[position].name());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}

	public static Report createReport(Context context, MyEntityManager em, Bundle extras) {
		String reportTypeName = extras.getString(MyFragmentAPI.EXTRA_REPORT_TYPE);
		ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = em.getHomeCurrency();
		return reportType.createReport(context, c);
	}
}
