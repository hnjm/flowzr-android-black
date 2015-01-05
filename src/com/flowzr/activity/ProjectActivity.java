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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Project;

public class ProjectActivity extends AbstractEditorActivity {


    public static final String ENTITY_ID_EXTRA = "entityId";
    private DatabaseAdapter db;
    private MyEntityManager em;

    private Project project = new Project();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project);

        CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
        activityCheckBox.setChecked(true);

        db = new DatabaseAdapter(this);
        db.open();

        em = db.em();

        Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(ENTITY_ID_EXTRA, -1);
            if (id != -1) {
                project = em.load(Project.class, id);
                editProject();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
                EditText title = (EditText)findViewById(R.id.title);
                CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
                project.title = title.getText().toString();
                project.isActive = activityCheckBox.isChecked();
                long id = em.saveOrUpdate(project);
                Intent intent = new Intent();
                intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
                setResult(RESULT_OK, intent);
                finish();	        		
        		return true;
	    	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void editProject() {
        EditText title = (EditText)findViewById(R.id.title);
        CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
        title.setText(project.title);
        activityCheckBox.setChecked(project.isActive);
    }

	@Override
	protected void onClick(View v, int id) {
		// TODO Auto-generated method stub
		
	}

}
