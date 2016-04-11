/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *      Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.EntityTypeListAdapter;
import com.flowzr.model.EntityType;



public class EntityListFragment extends AbstractListFragment {

	@Override
	protected String getEditActivityClass() {
		return null;
	}

	@Override
	public void onAttach(Context a) {
		super.onAttach(a);
		setHasOptionsMenu(true);
		activity=(MainActivity)a;
	}



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
    protected void internalOnCreate(Bundle savedInstanceState) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
	protected Cursor createCursor() {
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
		Bundle bundle = new Bundle();
		bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, entities[position].getActivityClass().getClass().getCanonicalName());
		activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}

	@Override
	protected String getMyTitle() {
		return getString(R.string.entities);
	}





}
