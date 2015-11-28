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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.utils.PinProtection;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import java.util.List;

public abstract class AbstractEditorActivity extends AppCompatActivity implements ActivityLayoutListener {

	protected DatabaseAdapter db;
	protected MyEntityManager em;
	
	protected ActivityLayout x;

    protected Utils u;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // Setup ActionBar		
		//initToolbar();

		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		NodeInflater nodeInflater = new NodeInflater(layoutInflater);

		x = new ActivityLayout(nodeInflater, this);
		db = new DatabaseAdapter(this);
		db.open();
		em = db.em();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}


    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.ok_cancel, menu);
        return true;
    }


    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	   	        
            case android.R.id.home:
            {
                TaskStackBuilder tsb = TaskStackBuilder.create(this);
                final int intentCount = tsb.getIntentCount();
                if (intentCount > 0)
                {
                    Intent upIntent = tsb.getIntents()[intentCount - 1];
                    if (NavUtils.shouldUpRecreateTask(this, upIntent))
                    {
                        // This activity is not part of the application's task, so create a new task with a synthesized back stack.
                        tsb.startActivities();
                        finish();
                    }
                    else
                    {
                        // This activity is part of the application's task, so simply navigate up to the hierarchical parent activity.
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                }
                else
                {
                    onBackPressed();
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }	
	
    protected boolean shouldLock() {
        return true;
    }

	@Override
	public void onClick(View v) {
		int id = v.getId();
		onClick(v, id);
	}

	protected abstract void onClick(View v, int id);


	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
	}

    protected boolean checkSelected(Object value, int messageResId) {
        if (value == null) {
            Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected boolean checkSelectedId(long value, int messageResId) {
		if (value <= 0) {
			Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public static void setVisibility(View v, int visibility) {
		v.setVisibility(visibility);
		Object o = v.getTag();
		if (o instanceof View) {
			((View)o).setVisibility(visibility);
		}
	}
		
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();		
	}


    @Override
    protected void onPause() {
        super.onPause();
        if (shouldLock()) {
            PinProtection.lock(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldLock()) {
            PinProtection.unlock(this);
        }
    }

}
