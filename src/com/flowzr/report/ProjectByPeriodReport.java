package com.flowzr.report;

import android.content.Context;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper.TransactionColumns;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Currency;
import com.flowzr.model.Project;
import com.flowzr.utils.MyPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly results by Projects.
 * @author Abdsandryk
 */
public class ProjectByPeriodReport extends Report2DChart {
	

	public ProjectByPeriodReport(Context context, MyEntityManager em, int periodLength, Currency currency) {
		super(context, em, periodLength, currency);
	}
	

	public ProjectByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}


	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long projectId = filterIds.get(currentFilterOrder);
			Project project = em.getProject(projectId);
			if (project!=null) {
				return project.getTitle();
			} else {
				return context.getString(R.string.no_project);
			}
		} else {
			// no project
			return context.getString(R.string.no_project);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}


	@Override
	public void setFilterIds() {
		boolean includeNoProject = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		currentFilterOrder = 0;
		ArrayList<Project> projects = em.getAllProjectsList(includeNoProject);
		if (projects.size()>0) {
			Project p;
			for (int i=0; i<projects.size(); i++) {
				p = projects.get(i);
				filterIds.add(p.getId());
			}
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.project_id.name();
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_project);
	}
}
