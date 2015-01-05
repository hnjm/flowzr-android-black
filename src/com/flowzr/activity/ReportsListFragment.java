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


import com.flowzr.R;
import com.flowzr.adapter.ReportListAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Currency;
import com.flowzr.report.Report;
import com.flowzr.report.ReportType;
import com.flowzr.utils.PinProtection;

import android.content.Context;
import android.content.Intent;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


public class ReportsListFragment extends ListFragment {
	

	public static final String EXTRA_REPORT_TYPE = "reportType";
	
	public final ReportType[] reports = new ReportType[]{
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
    	return inflater.inflate(R.layout.reports_list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		getActivity().setTitle(R.string.reports);
		setListAdapter(new ReportListAdapter(this.getView().getContext(), reports));
	}
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Bundle bundle = new Bundle();
		Fragment fragment=null;
		// Conventional Bars reports
		if (reports[position].isConventionalBarReport()) {
			bundle.putString(EXTRA_REPORT_TYPE, reports[position].name());
			fragment= new ReportFragment(); 			
		} else {
			bundle.putInt(Report2DChart.REPORT_TYPE, position);
			fragment= new Report2DChartActivity();	
		}
		fragment.setArguments(bundle);
		if (getActivity().findViewById(R.id.fragment_land_container)!=null) {
			FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_land_container,fragment);
			transaction.addToBackStack(null);
			transaction.commit();			
		} else {
			((EntityListActivity) getActivity()).loadFragment(fragment);	
		}
	}

	public static Report createReport(Context context, MyEntityManager em, Bundle extras) {
		String reportTypeName = extras.getString(EXTRA_REPORT_TYPE);
		ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = em.getHomeCurrency();
		return reportType.createReport(context, c);
	}

	public static Fragment newInstance(Bundle bundle) {
	      ReportsListFragment f = new ReportsListFragment();
          f.setArguments(bundle);
          return f;
	}

}
