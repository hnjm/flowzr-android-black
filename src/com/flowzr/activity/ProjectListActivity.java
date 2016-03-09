/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.view.View;

import com.flowzr.R;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.filter.Criteria;
import com.flowzr.model.Project;

import java.util.ArrayList;

public class ProjectListActivity extends MyEntityListActivity<Project> {

    public ProjectListActivity() {
        super(Project.class);
    }

    @Override
    protected ArrayList<Project> loadEntities() {
        return em.getAllProjectsList(false);
    }

    @Override
    protected String getMyTitle() {
        return getString(R.string.project);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class getEditActivityClass() {
        return ProjectActivity.class;
    }

    @Override
    protected Criteria createBlotterCriteria(Project p) {
        return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(p.id));
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        em.deleteProject(id);
        recreateCursor();
    }
}
