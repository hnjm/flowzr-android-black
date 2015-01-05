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

import java.util.List;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import com.flowzr.R;
import com.flowzr.adapter.AttributeListAdapter;
import com.flowzr.db.DatabaseHelper.AttributeColumns;
import com.flowzr.utils.MenuItemInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.widget.ListAdapter;

public class AttributeListActivity extends AbstractListFragment {
	
	public AttributeListActivity() {
		super(R.layout.attributes_list);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.add, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}	

	@Override
	protected void addItem() {
		Intent intent = new Intent(this.getActivity(), AttributeActivity.class);
		startActivityForResult(intent, 1);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new AttributeListAdapter(db, this.getActivity(), cursor);
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
		Intent intent = new Intent(this.getActivity(), AttributeActivity.class);
		intent.putExtra(AttributeColumns.ID, id);
		startActivityForResult(intent, 2);		
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
