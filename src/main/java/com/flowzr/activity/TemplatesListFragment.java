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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.TemplateListAdapter;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.filter.WhereFilter;


public class TemplatesListFragment extends BlotterFragment {

	public TemplatesListFragment() {}
	
	
	@Override
	protected void calculateTotals() {
		// do nothing
	}
	
	@Override
	protected Cursor createCursor() {
		return db.getAllTemplates(blotterFilter);
	}

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
    	return new TemplateListAdapter(this.getActivity(), db, cursor);
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    menu.clear();
	    inflater.inflate(R.menu.template_actions, menu);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.templates, container, false);
    }
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		Bundle args=this.getArguments();
		blotterFilter = new WhereFilter("templates");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(1));
	    blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
	}
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		blotterFilter = new WhereFilter("templates");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(1));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
        blotterFilter.asc(DatabaseHelper.BlotterColumns.template_name.name());


		//SelectTemplateFragment only use blotter.xml, TemplateListFragment use template.xml ...
		if (getView().findViewById(R.id.fragment_land_container)!=null) { 
			getView().findViewById(R.id.fragment_land_container).setVisibility(View.GONE);
		}
        if (getView().findViewById(R.id.container_V)!=null) {
            getView().findViewById(R.id.container_V).setVisibility(View.GONE);
        }


	}
	
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.bCancel:
				getActivity().setResult(MainActivity.RESULT_CANCELED);
				getActivity().finish();				
				return true;
		}
		return super.onOptionsItemSelected(item);
    }
	
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);    	
		menu.findItem(R.id.bEditTemplate).setVisible(false);
    }
    
    
}
