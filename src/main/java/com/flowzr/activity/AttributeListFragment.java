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

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.AttributeListAdapter;
import com.flowzr.db.DatabaseHelper.AttributeColumns;

public class AttributeListFragment extends AbstractListFragment {
	
	public AttributeListFragment() {
		super(R.layout.entity_list);
	}


	@Override
	protected String getEditActivityClass() {
		return AttributeActivity.class.getCanonicalName();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.add, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}	

	@Override
	protected void addItem() {
		Bundle bundle = new Bundle ();
		bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,AttributeActivity.class.getCanonicalName());
		activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new AttributeListAdapter(db, this.getActivity(), cursor);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {

	}

	@Override
	protected Cursor createCursor() {
		return db.getAllAttributes();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == MainActivity.RESULT_OK) {
			cursor.requery();
		}
	}

	@Override
	protected void deleteItem(View v, int position, final long id) {
		new AlertDialog.Builder(this.getActivity())
			.setTitle(R.string.delete)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(R.string.attribute_delete_alert)			
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteAttribute(id);
					cursor.requery();
				}				
			})
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}

	@Override
	public void editItem(View v, int position, long id) {
		Bundle bundle = new Bundle();
		bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
		bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, id);
		bundle.putLong(AttributeColumns.ID, id);
		activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

	@Override
	protected String getMyTitle() {
		return getString(R.string.attribute);
	}
	
}
