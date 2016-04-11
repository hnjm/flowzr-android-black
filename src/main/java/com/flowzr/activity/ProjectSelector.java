/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MyEntity;
import com.flowzr.model.Project;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.TransactionUtils;

import java.util.ArrayList;

import static com.flowzr.activity.AbstractEditorActivity.setVisibility;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public class ProjectSelector {

    private final MainActivity activity;
    private final Fragment myFragment;
    private final MyEntityManager em;
    private final ActivityLayout x;
    private final boolean isShowProject;

    private View projectNode;
    private TextView projectText;
    private ArrayList<Project> projects;
    private ListAdapter projectAdapter;

    private long selectedProjectId = 0;

    public ProjectSelector(MainActivity activity, Fragment myFragment, MyEntityManager em, ActivityLayout x) {
        this.activity = activity;
        this.myFragment = myFragment;
        this.em = em;
        this.x = x;
        this.isShowProject = MyPreferences.isShowProject(activity);
    }

    public void fetchProjects() {
        projects = em.getActiveProjectsList(true);
        projectAdapter = TransactionUtils.createProjectAdapter(activity, projects);
    }

    public void fetchProjects(long[] ids) {
        projects = em.getActiveProjectsList(true);
        ArrayList<Project> filtered= new ArrayList<>();

        for (int i=0; i<projects.size(); i++) {
            for (long id : ids) {
                if (id == projects.get(i).id) {
                    filtered.add(projects.get(i));

                }
            }
        }
        projectAdapter = TransactionUtils.createProjectAdapter(activity, filtered);
    }

    public void createNode(LinearLayout layout) {
        if (isShowProject) {
            projectText= x.addListNode2(layout, R.id.project, R.drawable.ic_star_border, R.string.project,layout.getResources().getString(R.string.select_project));
            projectNode = (View) projectText.getTag();
        }
    }

    public void onClick(int id) {
        switch (id) {
            case R.id.project:
                pickProject();
                break;
            case R.id.project_add: {
                Intent intent = new Intent(activity, ProjectActivity.class);
                Fragment fragment = new ProjectActivity();

                activity.startFragmentForResult(fragment,myFragment);
                break;
            }
        }
    }

    private void pickProject() {
        int selectedProjectPos = MyEntity.indexOf(projects, selectedProjectId);
        x.selectPositionWithAddOption(activity, R.id.project, R.string.project, projectAdapter, selectedProjectPos, R.string.create);
    }

    public void onSelectedPos(int id, int selectedPos) {
        switch(id) {
            case R.id.project:
                onProjectSelected(selectedPos);
                break;
        }
    }

    private void onProjectSelected(int selectedPos) {
        if (selectedPos== DialogInterface.BUTTON_NEUTRAL) {
            Bundle bundle = new Bundle();
            bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,AbstractTransactionActivity.NEW_PROJECT_REQUEST);
            bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,ProjectActivity.class.getCanonicalName());
            Fragment fragment = new ProjectActivity();
            fragment.setArguments(bundle);
            activity.startFragmentForResult(fragment,myFragment);
        }else {
            Project p = projects.get(selectedPos);
            selectProject(p);
        }
    }

    public void selectProject(long projectId) {
        if (isShowProject) {
            Project p = MyEntity.find(projects, projectId);
            selectProject(p);
        }
    }

    private void selectProject(Project p) {
        if (isShowProject && p != null) {
            projectText.setText(p.title);
            selectedProjectId = p.id;
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case AbstractTransactionActivity.NEW_PROJECT_REQUEST:
                    onNewProject(data);
                    break;
            }
        }
    }

    private void onNewProject(Intent data) {
        fetchProjects();
        long projectId = data.getLongExtra(DatabaseHelper.EntityColumns.ID, -1);
        if (projectId != -1) {
            selectProject(projectId);
        }
    }

    public void setProjectNodeVisible(boolean visible) {
        if (isShowProject && projectNode!=null) {
            setVisibility(projectNode, visible ? View.VISIBLE : View.GONE);
        }
    }

    public long getSelectedProjectId() {
            return selectedProjectId;
    }
}
