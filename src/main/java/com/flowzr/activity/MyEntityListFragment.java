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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.EntityListAdapter;
import com.flowzr.filter.Criteria;
import com.flowzr.model.MyEntity;

import java.util.List;

public abstract class MyEntityListFragment<T extends MyEntity> extends AbstractListFragment {

	private static final int NEW_ENTITY_REQUEST = 1;
	public static final int EDIT_ENTITY_REQUEST = 2;

    private final Class<T> clazz;

	private List<T> entities;
	private MainActivity activity;

	public MyEntityListFragment(Class<T> clazz) {
		super(R.layout.project_list);
        this.clazz = clazz;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.add, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		entities = loadEntities();
	}

    protected abstract List<T> loadEntities();

    @Override
	protected void addItem() {
		Intent intent = new Intent(MyEntityListFragment.this.getActivity(), getEditActivityClass());
		//@TODO add editor as fragment
        startActivityForResult(intent, NEW_ENTITY_REQUEST);
	}

    protected abstract Class<? extends MyEntityActivity> getEditActivityClass();

    @Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new EntityListAdapter<>(this.getActivity(), entities);
	}

	@Override
	protected Cursor createCursor() {
		return null;
	}
	
    @Override
    public void recreateCursor() {
        entities = loadEntities();
        @SuppressWarnings("unchecked")
        EntityListAdapter<T> a = (EntityListAdapter<T>)adapter;
        a.setEntities(entities);
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == MainActivity.RESULT_OK) {
			recreateCursor();
		}
	}

	@Override
	protected void deleteItem(View v, int position, final long id) {
        em.delete(clazz, id);
		recreateCursor();
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(MyEntityListFragment.this.getActivity(), getEditActivityClass());
		intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
        //@TODO editor to fragments
		startActivityForResult(intent, EDIT_ENTITY_REQUEST);
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		T e = em.load(clazz, id);
		Criteria blotterFilter = createBlotterCriteria(e);
		Bundle bundle = new Bundle();
		blotterFilter.toBundle(e.title,bundle);
		activity.onFragmentMessage(FragmentAPI.REQUEST_BLOTTER,bundle);
	}

    protected abstract Criteria createBlotterCriteria(T e);

}
