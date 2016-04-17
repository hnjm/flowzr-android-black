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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
		super(R.layout.entity_list);
        this.clazz = clazz;
	}

	@Override
	public void onAttach(Context a) {
		super.onAttach(a);
		setHasOptionsMenu(true);
		activity=(MainActivity)a;
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
		Intent intent = new Intent(MyEntityListFragment.this.getActivity(), MainActivity.class);
        intent.putExtra(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
        intent.putExtra(MyFragmentAPI.ENTITY_ID_EXTRA, -1);
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,intent.getExtras());
	}

    protected abstract String getEditActivityClass();

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
		Bundle bundle = new Bundle();
		bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, id);
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		T e = em.load(clazz, id);
		Criteria blotterFilter = createBlotterCriteria(e);
		Bundle bundle = new Bundle();
		blotterFilter.toBundle(e.title,bundle);
		activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
	}

    protected abstract Criteria createBlotterCriteria(T e);

}
