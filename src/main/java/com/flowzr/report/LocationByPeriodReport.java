package com.flowzr.report;

import android.content.Context;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper.TransactionColumns;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Currency;
import com.flowzr.model.MyLocation;
import com.flowzr.utils.MyPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly results by Locations.
 * @author Abdsandryk
 */
public class LocationByPeriodReport extends Report2DChart {
	

	public LocationByPeriodReport(Context context, MyEntityManager em, int periodLength, Currency currency) {
		super(context, em, periodLength, currency);
	}
	

	public LocationByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}


	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}


	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long locationId = filterIds.get(currentFilterOrder);
			MyLocation location = em.get(MyLocation.class, locationId);
			if (location != null) {
				return location.name;
			} else {
				return context.getString(R.string.current_location);
			}
		} else {
			// no location
			return context.getString(R.string.current_location);
		}
	}

	@Override
	public void setFilterIds() {
		boolean includeNoLocation = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		currentFilterOrder = 0;
		List<MyLocation> locations = em.getAllLocationsList(includeNoLocation);
		if (locations.size()>0) {
			MyLocation l;
			for (int i=0; i<locations.size(); i++) {
				l = locations.get(i);
				filterIds.add(l.id);
			}
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.location_id.name();
	}
	
	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_location);
	}

}
