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
package com.flowzr.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.view.NodeInflater;

import java.util.List;

public abstract class AbstractActivity extends AppCompatActivity implements ActivityLayoutListener {

	protected DatabaseAdapter db;
	protected MyEntityManager em;
	
	protected ActivityLayout x;

	protected void initToolbar() {
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		final ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.setHomeAsUpIndicator(R.drawable.ic_drawer);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		NodeInflater nodeInflater = new NodeInflater(layoutInflater);
		x = new ActivityLayout(nodeInflater, this);
		db = new DatabaseAdapter(this);
		db.open();
		em = db.em();
		initToolbar();
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
	
}
