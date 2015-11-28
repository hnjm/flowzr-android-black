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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import com.flowzr.R;
import com.flowzr.adapter.EntityTypeListAdapter;

import com.flowzr.model.EntityType;



public class EntityListFragment extends AbstractListFragment {
	
	
	public final EntityType[] entities = new EntityType[]{
			EntityType.CURRENCIES,
			EntityType.EXCHANGE_RATES,
			EntityType.CATEGORIES,
			EntityType.PAYEES,
			EntityType.PROJECTS,
			EntityType.LOCATIONS
	};
	
    public EntityListFragment() {
		super(R.layout.entity_list);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(createAdapter(null));
	}

	@Override
	protected void prepareActionGrid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Cursor createCursor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new EntityTypeListAdapter(this.getActivity(), entities);
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		// nothing to do
	}

	@Override
	protected void editItem(View v, int position, long id) {
		// nothing to do
		
	}

	@Override
	protected void viewItem(View v, int position, long id) {
		EntityListActivity main=((EntityListActivity)getActivity());
        main.loadFragment(entities[position].getActivityClass());	
	}

	@Override
	protected String getMyTitle() {
		return getString(R.string.entities);
	}





}
