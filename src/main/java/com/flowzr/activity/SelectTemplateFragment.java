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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.TemplateListAdapter;

public class SelectTemplateFragment extends TemplatesListFragment {
	
	public static final String TEMPATE_ID = "template_id";
	public static final String MULTIPLIER = "multiplier";
	public static final String EDIT_AFTER_CREATION = "edit_after_creation";

	private TextView multiplierText;
	private int multiplier = 1; 
	

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	setHasOptionsMenu(true);
    	return inflater.inflate(R.layout.templates, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.template_actions, menu);
		menu.removeItem(R.id.bAdd);
		menu.removeItem(R.id.bCancel);
		menu.removeItem(R.id.bTransfer);
	}
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				returnResult(id, true);
				return true;
			}
		});
	
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			}
		});
		

		multiplierText = (TextView)getView().findViewById(R.id.multiplier);
		ImageButton ib = (ImageButton)getView().findViewById(R.id.bPlus);
		ib.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				incrementMultiplier();
			}
		});
		ib = (ImageButton)getView().findViewById(R.id.bMinus);
		ib.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				decrementMultiplier();
			}
		});
		if (getView().findViewById(R.id.fragment_land_container)!=null) {
			getView().findViewById(R.id.fragment_land_container).setVisibility(View.GONE);
		}
        if (getView().findViewById(R.id.container_V)!=null) {
            getView().findViewById(R.id.container_V).setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.bEditTemplate:
				Bundle bundle = new Bundle();
				bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,TemplatesListFragment.class.getCanonicalName());
				activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
				return true;
		}
		return super.onOptionsItemSelected(item);
    }
	
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
        if (menu.findItem(R.id.bEditTemplate)!=null) {
            menu.findItem(R.id.bEditTemplate).setVisible(true);
        }

    }
    
	protected void incrementMultiplier() {
		++multiplier;
		multiplierText.setText("x"+multiplier);
	}

	protected void decrementMultiplier() {
		--multiplier;
		if (multiplier < 1) {
			multiplier = 1;
		}
		multiplierText.setText("x"+multiplier);		
	}

	@Override
	public void registerForContextMenu(View view) {
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TemplateListAdapter(this.getActivity(), db, cursor);
	}

    @Override
    public void onItemClick(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
	protected void viewItem(View v, int position, long id) {
		returnResult(id, false);
	}

	@Override
	public void editItem(long id) {
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// do nothing
	}

	void returnResult(long id, boolean edit) {
        //@TODO generate templates
        Bundle bundle = new Bundle();
        bundle.putLong(TEMPATE_ID,id);
        bundle.putInt(MULTIPLIER,multiplier);
        bundle.putLong(MyFragmentAPI.ENTITY_CLASS_EXTRA,id);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
    }

}
