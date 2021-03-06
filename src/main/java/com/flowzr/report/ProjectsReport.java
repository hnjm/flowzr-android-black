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
package com.flowzr.report;


import android.content.Context;

import com.flowzr.activity.BlotterFragment;
import com.flowzr.activity.SplitsBlotterFragment;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.datetime.PeriodType;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Currency;
import com.flowzr.model.Payee;
import com.flowzr.model.Project;

import static com.flowzr.db.DatabaseHelper.V_REPORT_PROJECTS;

public class ProjectsReport extends Report {

	public ProjectsReport(Context context, Currency currency) {
		super(ReportType.BY_PROJECT, context, currency);
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
        cleanupFilter(filter);
		return queryReport(db, V_REPORT_PROJECTS, filter);
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(id));
	}

	@Override
	protected String getTitleForId(DatabaseAdapter db, long id) {
		return db.em().load(Project.class,id).getTitle();
	}

	@Override
    public Class<? extends BlotterFragment> getBlotterActivityClass() {
        return SplitsBlotterFragment.class;
    }

}
